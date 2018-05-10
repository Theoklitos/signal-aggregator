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

/**
 * this client supports only one account. do not use anymore
 */
@Deprecated
public class OldOandaForexClient { // implements ForexClient

	private static final Logger logger = LoggerFactory.getLogger(OldOandaForexClient.class);

	public static final void main(final String args[]) {
		final OldOandaForexClient client = new OldOandaForexClient();
		client.secretKey = "d13d3df3cc2481a3e9ed86c90494cff1-e541cabba88918fdb38fab31064c3d33";
		client.accountId = "101-004-1663141-001"; // old sa account
		// client.accountId = "101-004-1663141-002"; // theoklitos account
		client.rootUrl = "https://api-fxpractice.oanda.com";

		// System.out.println(client.openTrade(1000, Instrument.EUR_USD, Side.BUY, Optional.of(BigDecimal.valueOf(1.6000)), 1.17000, 1.18000));
		// System.out.println(client.openTrade(1000, Instrument.EUR_USD, Side.BUY, Optional.of(1.6000), 1.17000, 1.18000));
		// System.out.println(client.closeTrade("1342"));
		// System.out.println(client.getRemoteTradeInformation("1357", RemoteTransactionStatus.PENDING).getPrettyJson());

		System.out.println(client.getRemoteTradeInformation("1342", RemoteTransactionStatus.CLOSED).getPrettyJson());

		// client.openTrade(1000, Instrument.EUR_USD, Side.BUY, Optional.empty(), 1.18050, 1.18250);

	}

	@Value("${forexClient.secretKey}")
	private String secretKey;

	@Value("${forexClient.rootUrl}")
	private String rootUrl;

	@Value("${forexClient.accountId}")
	private String accountId;

	public OldOandaForexClient() {
		Unirest.setDefaultHeader("Authorization", "Bearer " + secretKey);
	}

	/**
	 * use with caution!
	 */
	protected void closeAllTrades() {
		getAllRemoteTradeInformationsForStatus(RemoteTransactionStatus.OPEN).stream().forEach(rti -> {
			closeTrade(rti.getId());
			logger.info("Closed trade #" + rti.getId());
			getRemoteTradeInformation(rti.getId(), RemoteTransactionStatus.CLOSED);
		});
	}

	public RemoteTransaction closeTrade(final String remoteId) throws TradingException {
		final JSONObject body = new JSONObject();
		// TODO handle canceling pending orders, which are at the /orders/{order_id}/cancel url
		final JSONObject response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/trades/" + remoteId + "/close", HttpMethod.PUT, Optional.of(body));
		logger.debug(response.toString(5));
		if (response.has("orderFillTransaction")) {
			final RemoteTransaction rti = new RemoteTransaction(response);
			final JSONObject orderFillJson = response.getJSONObject("orderFillTransaction");
			final BigDecimal pl = new BigDecimal(orderFillJson.getString("pl"));
			rti.setPl(pl);
			rti.setEntryPrice(new BigDecimal(orderFillJson.getString("price")));
			rti.setCloseReason(RemoteTradeCloseReason.ORDER);
			rti.setStatus(RemoteTransactionStatus.CLOSED);
			// rti.setClosingPrice(Double.valueOf(orderFillJson.getString("averageClosePrice"))); TODO
			final String receivedRemoteId = orderFillJson.getJSONArray("tradesClosed").getJSONObject(0).getString("tradeID");
			rti.setId(receivedRemoteId);
			return rti;
		}

		// if there was no "orderFillTransaction" there something went wrong. debug once you have a json sample
		throw new TradingException("No orderFillTransaction in response, after opening trade. JSON:\n" + response.toString(5));
	}

	/**
	 * Use this when GETing trades, not when opening or closing them.
	 */
	private RemoteTransaction convertJsonToGenericRti(final JSONObject rtiObject) {
		final RemoteTransaction rti = new RemoteTransaction(rtiObject);
		final String stateString = rtiObject.getString("state");
		if (stateString.equals("PENDING")) { // for pending orders
			rti.setId(rtiObject.getString("id"));
			rti.setStatus(RemoteTransactionStatus.PENDING);
			rti.setEntryPrice(new BigDecimal(rtiObject.getString("price")));
			return rti;
		} else if (stateString.equals("FILLED")) { // a pending order that is now filled
			rti.setId(rtiObject.getString("id"));
			try {
				if (rtiObject.has("tradeOpenedID")) {
					rti.setRelatedId(rtiObject.getString("tradeOpenedID"));
				} else {
					rti.setRelatedId(rtiObject.getJSONArray("tradeClosedIDs").getString(0));
				}
			} catch (final JSONException e) {
				// this might happen and we'd have to investigate...
				throw new TradingException("FILLED order without a tradeOpenedID or tradeClosedID!", rtiObject);
			}
			rti.setStatus(RemoteTransactionStatus.FILLED);
			if (rtiObject.has("price")) {
				rti.setEntryPrice(new BigDecimal(rtiObject.getString("price")));
			}
			return rti;
		} else if (stateString.equals("CANCELLED")) { // the order somehow closed manually
			rti.setId(rtiObject.getString("id"));
			rti.setStatus(RemoteTransactionStatus.CLOSED);
			rti.setCloseReason(RemoteTradeCloseReason.ORDER);
			return rti;
		} else { // for market orders
			rti.setId(rtiObject.getString("id"));
			if (stateString.equals("OPEN")) {
				rti.setStatus(RemoteTransactionStatus.OPEN);
			} else if (stateString.equals("CLOSED")) {
				rti.setStatus(RemoteTransactionStatus.CLOSED);
				rti.setCloseDateTime(new DateTime(rtiObject.getString("closeTime")));
				rti.setEntryPrice(new BigDecimal(rtiObject.getString("averageClosePrice")));
				if (rtiObject.has("stopLossOrder") && rtiObject.getJSONObject("stopLossOrder").getString("state").equals("FILLED")) {
					rti.setCloseReason(RemoteTradeCloseReason.STOPLOSS);
				} else if (rtiObject.has("takeProfitOrder") && rtiObject.getJSONObject("takeProfitOrder").getString("state").equals("FILLED")) {
					rti.setCloseReason(RemoteTradeCloseReason.TAKEPROFIT);
				} else {
					rti.setCloseReason(RemoteTradeCloseReason.ORDER);
				}
			} else {
				// not sure what others states could be or mean. Log them for now.
				throw new TradingException("Unrecognized state, neither OPEN nor CLOSED. Json:\n" + rtiObject.toString(5));
			}
			final RemoteTransactionStatus status = (rtiObject.getString("state").equals("OPEN")) ? RemoteTransactionStatus.OPEN
					: RemoteTransactionStatus.CLOSED;
			rti.setStatus(status);
			final String plString = rtiObject.has("unrealizedPL") ? rtiObject.getString("unrealizedPL") : rtiObject.getString("realizedPL");
			rti.setPl(new BigDecimal(plString));
			final String priceString = rtiObject.getString("price");
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
			throw new TradingException(e.getLocalizedMessage(), new JSONObject(e.getResponseBodyAsString()));
		}
	}

	public List<RemoteTransaction> getAllRemoteTradeInformations() throws TradingException {
		final List<RemoteTransaction> openRemoteTrades = getAllRemoteTradeInformationsForStatus(RemoteTransactionStatus.OPEN);
		final List<RemoteTransaction> closedRemoteTrades = getAllRemoteTradeInformationsForStatus(RemoteTransactionStatus.CLOSED);
		final List<RemoteTransaction> pendingRemoteTrades = getAllRemoteTradeInformationsForStatus(RemoteTransactionStatus.PENDING);
		return Stream.of(openRemoteTrades.stream(), closedRemoteTrades.stream(), pendingRemoteTrades.stream()).flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	public List<RemoteTransaction> getAllRemoteTradeInformationsForStatus(final RemoteTransactionStatus status) throws TradingException {
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
				final RemoteTransaction rti = convertJsonToGenericRti(tradeArray.getJSONObject(i));
				result.add(rti);
			}
		}
		return result;
	}

	public Price getCurrentPrice(final Instrument instrument) {
		final JSONObject response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/pricing?instruments=" + instrument, HttpMethod.GET, Optional.empty());
		final JSONObject priceObject = response.getJSONArray("prices").getJSONObject(0);
		final double ask = Double.valueOf(priceObject.getString("closeoutAsk"));
		final double bid = Double.valueOf(priceObject.getString("closeoutBid"));
		return new Price(bid, ask);
	}

	public RemoteTransaction getRemoteTradeInformation(final String remoteId, final RemoteTransactionStatus status)
			throws TradeDoesNotExistException, TradingException {
		JSONObject tradeObject = null;
		JSONObject response = null;
		switch (status) {
		case OPEN:
		case CLOSED:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/trades?state=" + status.toString() + "&ids=" + remoteId, HttpMethod.GET,
					Optional.empty());
			final JSONArray tradeArray = response.getJSONArray("trades");
			if (tradeArray.length() == 0) {
				throw new TradeDoesNotExistException(remoteId);
			}
			tradeObject = tradeArray.getJSONObject(0);
			break;
		case PENDING:
			response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/orders/" + remoteId, HttpMethod.GET, Optional.empty());
			if (!response.has("order")) {
				throw new TradeDoesNotExistException(remoteId);
			}
			tradeObject = response.getJSONObject("order");
			break;
		default:
			throw new TradingException("Cannot handle remote status " + status);
		}
		return convertJsonToGenericRti(tradeObject);
	}

	public RemoteTransaction openTrade(final int units, final Instrument instrument, final Side side, final Optional<Double> entryPriceOpt,
			final double stopLoss, final double takeProfit) throws TradingException {
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

		final DecimalFormat decimalFormat = new DecimalFormat("#.####");
		decimalFormat.setRoundingMode(RoundingMode.CEILING);
		if (stopLoss != 0) {
			final JSONObject stopLossJson = new JSONObject();
			stopLossJson.put("price", decimalFormat.format(stopLoss));
			order.put("stopLossOnFill", stopLossJson);
		}
		if (takeProfit != 0) {
			final JSONObject takeProfitJson = new JSONObject();
			takeProfitJson.put("price", decimalFormat.format(takeProfit));
			order.put("takeProfitOnFill", takeProfitJson);
		}

		final JSONObject body = new JSONObject();
		body.put("order", order);

		final JSONObject response = doRequest(rootUrl + "/v3/accounts/" + accountId + "/orders", HttpMethod.POST, Optional.of(body));
		logger.debug("Open trade response: " + response.toString(5));
		if (response.has("orderFillTransaction")) {
			final RemoteTransaction rti = new RemoteTransaction(response);
			final JSONObject orderFillObject = response.getJSONObject("orderFillTransaction");
			rti.setId(orderFillObject.getString("id"));
			rti.setEntryPrice(new BigDecimal(orderFillObject.getString("price")));
			rti.setStatus(RemoteTransactionStatus.OPEN);
			return rti;
		} else if (isMarketIfTouched) { // pending orders have a different structure
			if (response.has("orderCreateTransaction")) {
				final RemoteTransaction rti = new RemoteTransaction(response);
				final JSONObject orderFillObject = response.getJSONObject("orderCreateTransaction");
				rti.setId(orderFillObject.getString("id"));
				rti.setEntryPrice(new BigDecimal(orderFillObject.getString("price")));
				rti.setStatus(RemoteTransactionStatus.PENDING);
				return rti;
			}
		} else if (response.has("orderCancelTransaction")) {
			final String reasonString = response.getJSONObject("orderCancelTransaction").getString("reason");
			throw new TradingException("Opening of trade failed. Reason: " + reasonString, response);
		}

		// if we do not know the response type, log it, so that we can handle it:
		throw new TradingException("Unkown response from OANDA: " + response.toString(5));
	}

}
