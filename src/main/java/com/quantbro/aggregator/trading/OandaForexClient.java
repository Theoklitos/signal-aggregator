package com.quantbro.aggregator.trading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.mashape.unirest.http.Unirest;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTradeCloseReason;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTransactionStatus;
import com.quantbro.aggregator.utils.ForexUtils;

/**
 * consumes OANDA's rest api. Can work with multiple accounts.
 */
public class OandaForexClient implements ForexClient {

	private static final Logger logger = LoggerFactory.getLogger(OandaForexClient.class);

	public static final void main(final String args[]) {
		final OandaForexClient client = new OandaForexClient();
		client.secretKey = "b8a205b7efb4595300e08c5f6befec88-0bc95b47d5e680de94309c47d2b84b2c";
		client.rootUrl = "https://api-fxpractice.oanda.com";
		// final String accountId = "101-004-1663141-001"; // fxleaders
		// final String accountId = "101-004-1663141-003"; // foresignal

		// System.out.println(client.openTrade(accountId, 1000, Instrument.GBP_USD, Side.BUY, Optional.of(BigDecimal.valueOf(1.3173)), BigDecimal.valueOf(1.3108),
		// BigDecimal.valueOf(1.3228)));

		// client.getAllRemoteTransactionsForStatus(accountId, RemoteTransactionStatus.PENDING).forEach(rt -> {
		// System.out.println(rt.getPrettyJson());
		// });

		// System.out.println(client.getRemoteTransaction(accountId, "5", RemoteTransactionStatus.CLOSED).getPrettyJson());

		// System.out.println(client.doRequest(client.rootUrl + "/v3/accounts/" + accountId + "/transactions/13", HttpMethod.GET, Optional.empty()));

	}

	@Value("${forexClient.secretKey}")
	private String secretKey;

	@Value("${forexClient.rootUrl}")
	private String rootUrl;

	public OandaForexClient() {
		Unirest.setDefaultHeader("Authorization", "Bearer " + secretKey);
	}

	@Override
	public RemoteTransaction closeTrade(final String accountId, final String remoteId) throws TradingException {
		final JSONObject body = new JSONObject();
		JSONObject response = null;
		try {
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/trades/" + remoteId + "/close", HttpMethod.PUT, Optional.of(body));
		} catch (final TradingException e) {
			logger.debug("Failed when trying to close trade; will attempt to close order instead...");
			// it may be an order; try to cancel the order
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/orders/" + remoteId + "/cancel", HttpMethod.PUT, Optional.of(body));
		}
		logger.debug("Close trade response: " + response.toString(5));
		if (response.has("orderFillTransaction")) {
			final RemoteTransaction rt = new RemoteTransaction(response);
			final JSONObject orderFillJson = response.getJSONObject("orderFillTransaction");
			final BigDecimal pl = new BigDecimal(orderFillJson.getString("pl"));
			rt.setPl(pl);
			rt.setEntryPrice(new BigDecimal(orderFillJson.getString("price")));
			rt.setCloseReason(RemoteTradeCloseReason.ORDER);
			rt.setStatus(RemoteTransactionStatus.CLOSED);

			// use an average close price
			final JSONObject fullPriceJson = orderFillJson.getJSONObject("fullPrice");
			final BigDecimal closeoutBid = BigDecimal.valueOf(fullPriceJson.getDouble("closeoutBid"));
			final BigDecimal closeoutAsk = BigDecimal.valueOf(fullPriceJson.getDouble("closeoutAsk"));
			final BigDecimal closingPriceAverage = closeoutAsk.add(closeoutBid).divide(BigDecimal.valueOf(2));
			rt.setClosingPrice(closingPriceAverage);

			final String receivedRemoteId = orderFillJson.getJSONArray("tradesClosed").getJSONObject(0).getString("tradeID");
			rt.setId(receivedRemoteId);
			return rt;
		} else if (response.has("orderCancelTransaction")) {
			final RemoteTransaction rt = new RemoteTransaction(response);
			// final JSONObject orderFillJson = response.getJSONObject("orderCancelTransaction");
			rt.setCloseReason(RemoteTradeCloseReason.ORDER);
			rt.setStatus(RemoteTransactionStatus.CANCELLED);
			return rt;
		}

		// if there was no "orderFillTransaction" or "orderCancelTransaction" there something went wrong. we can debug this once it happens and we have some json samples
		throw new TradingException("No orderFillTransaction in response, after opening trade. JSON:\n" + response.toString(5));
	}

	/**
	 * Use this when GETing trades, not when opening or closing them.
	 */
	private RemoteTransaction convertGottenTradeToRemoteTransaction(final JSONObject rtObject) {
		final RemoteTransaction rti = new RemoteTransaction(rtObject);
		final String stateString = rtObject.getString("state");
		if (stateString.equals("PENDING")) { // for pending orders
			rti.setId(rtObject.getString("id"));
			rti.setStatus(RemoteTransactionStatus.PENDING);
			rti.setEntryPrice(new BigDecimal(rtObject.getString("price")));
			return rti;
		} else if (stateString.equals("FILLED")) { // a pending order that is now filled
			rti.setId(rtObject.getString("id"));
			try {
				if (rtObject.has("tradeOpenedID")) {
					rti.setRelatedId(rtObject.getString("tradeOpenedID"));
				} else {
					rti.setRelatedId(rtObject.getJSONArray("tradeClosedIDs").getString(0));
				}
			} catch (final JSONException e) {
				// this might happen and we'd have to investigate...
				throw new TradingException("FILLED order without a tradeOpenedID or tradeClosedID!", rtObject);
			}
			rti.setStatus(RemoteTransactionStatus.FILLED);
			if (rtObject.has("price")) {
				rti.setEntryPrice(new BigDecimal(rtObject.getString("price")));
			}
			return rti;
		} else if (stateString.equals("CANCELLED")) { // the pending order (not market!) was cancelled manually
			rti.setId(rtObject.getString("id"));
			rti.setStatus(RemoteTransactionStatus.CANCELLED);
			rti.setCloseDateTime(new DateTime(rtObject.getString("cancelledTime")));
			return rti;
		} else { // for market orders
			rti.setId(rtObject.getString("id"));
			if (stateString.equals("OPEN")) {
				rti.setStatus(RemoteTransactionStatus.OPEN);
			} else if (stateString.equals("CLOSED")) {
				rti.setStatus(RemoteTransactionStatus.CLOSED);
				rti.setCloseDateTime(new DateTime(rtObject.getString("closeTime")));
				rti.setClosingPrice(new BigDecimal(rtObject.getString("averageClosePrice")));
				if (rtObject.has("stopLossOrder") && rtObject.getJSONObject("stopLossOrder").getString("state").equals("FILLED")) {
					rti.setCloseReason(RemoteTradeCloseReason.STOPLOSS);
				} else if (rtObject.has("takeProfitOrder") && rtObject.getJSONObject("takeProfitOrder").getString("state").equals("FILLED")) {
					rti.setCloseReason(RemoteTradeCloseReason.TAKEPROFIT);
				} else { // cancelled trades will arrive here
					rti.setStatus(RemoteTransactionStatus.CANCELLED);
					rti.setCloseDateTime(new DateTime(rtObject.getString("closeTime")));
					rti.setClosingPrice(new BigDecimal(rtObject.getString("averageClosePrice")));
				}
			} else {
				// not sure what others states could be or what they mean. Long and investigate.
				throw new TradingException("Unrecognized state:\n" + rtObject.toString(5));
			}

			final String plString = rtObject.has("unrealizedPL") ? rtObject.getString("unrealizedPL") : rtObject.getString("realizedPL");
			rti.setPl(new BigDecimal(plString));
			final String priceString = rtObject.getString("price");
			rti.setEntryPrice(new BigDecimal(priceString));
			return rti;
		}
	}

	private JSONObject doRequest(final String url, final HttpMethod method, final Optional<JSONObject> body) throws TradingException {
		final RestTemplate restTemplate = new RestTemplate();
		final HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + secretKey);
		@SuppressWarnings("rawtypes")
		HttpEntity entity = null;
		if (method == HttpMethod.GET) {
			entity = new HttpEntity<String>("", headers);
		} else if (method == HttpMethod.POST || method == HttpMethod.PUT) {
			headers.setContentType(MediaType.APPLICATION_JSON);
			final String bodyString = body.isPresent() ? body.get().toString(5) : "";
			if (bodyString.length() > 0) {
				logger.debug("About to " + method + " to \"" + url + "\" this:\n" + bodyString);
			} else {
				logger.debug("About to " + method + " to \"" + url + "\"");
			}
			entity = new HttpEntity<String>(bodyString, headers);
		}

		try {
			final ResponseEntity<String> responseEntity = restTemplate.exchange(url, method, entity, String.class);
			if (responseEntity.getStatusCode().is4xxClientError()) {
				throw new TradingException(responseEntity);
			}
			return new JSONObject(responseEntity.getBody());
		} catch (final HttpClientErrorException e) {
			JSONObject jsonOfException = new JSONObject();
			try {
				jsonOfException = new JSONObject(e.getResponseBodyAsString());
			} catch (final JSONException je) {
				// it's ok, there isnt any json
			}
			throw new TradingException(e.getLocalizedMessage(), jsonOfException);
		}
	}

	@Override
	public List<RemoteTransaction> getAllRemoteTransactions(final String accountId) throws TradingException {
		final List<RemoteTransaction> openRemoteTrades = getAllRemoteTransactionsForStatus(accountId, RemoteTransactionStatus.OPEN);
		final List<RemoteTransaction> closedRemoteTrades = getAllRemoteTransactionsForStatus(accountId, RemoteTransactionStatus.CLOSED);
		final List<RemoteTransaction> pendingRemoteTrades = getAllRemoteTransactionsForStatus(accountId, RemoteTransactionStatus.PENDING);
		return Stream.of(openRemoteTrades.stream(), closedRemoteTrades.stream(), pendingRemoteTrades.stream()).flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	@Override
	public List<RemoteTransaction> getAllRemoteTransactionsForStatus(final String accountId, final RemoteTransactionStatus status) throws TradingException {
		final List<RemoteTransaction> result = Lists.newArrayList();
		JSONObject response = null;
		JSONArray tradeArray = null;
		switch (status) {
		case OPEN:
		case CLOSED:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/trades?state=" + status.toString(), HttpMethod.GET, Optional.empty());
			tradeArray = response.getJSONArray("trades");
			break;
		case PENDING:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/pendingOrders", HttpMethod.GET, Optional.empty());
			tradeArray = response.getJSONArray("orders");
			break;
		default:
			throw new TradingException("Cannot handle remote status " + status);
		}
		if (tradeArray != null) {
			for (int i = 0; i < tradeArray.length(); i++) {
				final RemoteTransaction rti = convertGottenTradeToRemoteTransaction(tradeArray.getJSONObject(i));
				result.add(rti);
			}
		}
		return result;
	}

	@Override
	public Price getCurrentPrice(final String accountId, final Instrument instrument) {
		final JSONObject response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/pricing?instruments=" + instrument, HttpMethod.GET, Optional.empty());
		final JSONObject priceObject = response.getJSONArray("prices").getJSONObject(0);
		final double ask = Double.valueOf(priceObject.getString("closeoutAsk"));
		final double bid = Double.valueOf(priceObject.getString("closeoutBid"));
		return new Price(bid, ask);
	}

	@Override
	public RemoteTransaction getRemoteTransaction(final String accountId, final String remoteTransactionId, final RemoteTransactionStatus status)
			throws TradeDoesNotExistException, TradingException {
		JSONObject tradeObject = null;
		JSONObject response = null;
		switch (status) {
		case OPEN:
		case CLOSED:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/trades?state=" + status.toString() + "&ids=" + remoteTransactionId, HttpMethod.GET,
					Optional.empty());
			final JSONArray tradeArray = response.getJSONArray("trades");
			if (tradeArray.length() == 0) {
				throw new TradeDoesNotExistException(remoteTransactionId);
			}
			tradeObject = tradeArray.getJSONObject(0);
			break;
		case PENDING:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/orders/" + remoteTransactionId, HttpMethod.GET, Optional.empty());
			if (!response.has("order")) {
				throw new TradeDoesNotExistException(remoteTransactionId);
			}
			tradeObject = response.getJSONObject("order");
			break;
		default:
			throw new TradingException("Cannot handle remote status " + status);
		}
		return convertGottenTradeToRemoteTransaction(tradeObject);
	}

	@Override
	public RemoteTransaction openTrade(final String accountId, final int units, final Instrument instrument, final Side side,
			final Optional<BigDecimal> entryPriceOpt, final BigDecimal stopLoss, final BigDecimal takeProfit) throws TradingException {
		if (units <= 0 || !ForexUtils.isPositiveNumber(takeProfit) || !ForexUtils.isPositiveNumber(takeProfit)) {
			throw new IllegalArgumentException("Tried to open trade with zero, negative or missing value");
		}

		final JSONObject order = new JSONObject();
		order.put("instrument", instrument);
		final int actualUnits = (side == Side.SELL) ? -units : units;
		order.put("units", String.valueOf(actualUnits));
		final boolean isMarketIfTouched = entryPriceOpt.isPresent();
		if (isMarketIfTouched) {
			order.put("type", "MARKET_IF_TOUCHED");
			order.put("price", String.valueOf(entryPriceOpt.get()));
		} else {
			order.put("type", "MARKET");
		}

		final DecimalFormat decimalFormat = new DecimalFormat("#.#####"); // most precise pippete possible value
		decimalFormat.setRoundingMode(RoundingMode.CEILING);
		final JSONObject stopLossJson = new JSONObject();
		stopLossJson.put("price", decimalFormat.format(stopLoss));
		order.put("stopLossOnFill", stopLossJson);
		final JSONObject takeProfitJson = new JSONObject();
		takeProfitJson.put("price", decimalFormat.format(takeProfit));
		order.put("takeProfitOnFill", takeProfitJson);
		final JSONObject body = new JSONObject();
		body.put("order", order);

		final JSONObject response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/orders", HttpMethod.POST, Optional.of(body));
		logger.debug("Open trade response: " + response.toString(5));
		if (response.has("orderFillTransaction")) {
			final RemoteTransaction rt = new RemoteTransaction(response);
			final JSONObject orderFillObject = response.getJSONObject("orderFillTransaction");
			rt.setId(orderFillObject.getString("id"));
			rt.setEntryPrice(new BigDecimal(orderFillObject.getString("price")));
			rt.setStatus(RemoteTransactionStatus.OPEN);
			return rt;
		} else if (isMarketIfTouched) { // pending orders have a different structure
			if (response.has("orderCreateTransaction")) {
				final RemoteTransaction rt = new RemoteTransaction(response);
				final JSONObject orderFillObject = response.getJSONObject("orderCreateTransaction");
				rt.setId(orderFillObject.getString("id"));
				rt.setEntryPrice(new BigDecimal(orderFillObject.getString("price")));
				rt.setStatus(RemoteTransactionStatus.PENDING);
				return rt;
			}
		} else if (response.has("orderCancelTransaction")) { // this means that the order was cancelled because there was a problem
			final String reasonString = response.getJSONObject("orderCancelTransaction").getString("reason");
			throw new TradingException("Opening of trade failed. Reason: " + reasonString, response);
		}

		// if we do not know the response type, log it, so that we can handle it:
		throw new TradingException("Unkown response from OANDA: " + response.toString(5));
	}

}
