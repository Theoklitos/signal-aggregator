package com.quantbro.aggregator.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.configuration.SignalProviderConfiguration;
import com.quantbro.aggregator.controllers.TradeSynchronizationException;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.trading.ForexClient;
import com.quantbro.aggregator.trading.RemoteTransaction;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTransactionStatus;

/**
 * Is responsible to tracking remote trades (ie trades in the broker) and sync them with our DB
 */
@Service
public class TradeSynchronizationService {

	private final static Logger logger = LoggerFactory.getLogger(TradeSynchronizationService.class);

	@Value("${trading.tradeSynchronization.cronJob}")
	private String cronJob;

	@Autowired
	private SignalProviderConfigurationHolder signalProvidersConfiguration;

	@Autowired
	private TradingService tradingService;

	@Autowired
	private ForexClient client;

	private void closeTradeWithErrorAndLog(final Trade trade) {
		logger.warn("Trade #" + trade.getId() + " (r:" + trade.getRemoteId() + ") was open in our DB but did not exist in the broker.");
		trade.endTradeWithStatusAndMessage(TradeStatus.ERROR, "This trade was open in our DB but did not exist in the broker. Why?");
	}

	/**
	 * goes through the list of trades and requests {@link RemoteTransaction}s for each trading account, with the given statuses.
	 *
	 * @return a list of all the remote transactions fetched, along with their account ids
	 */
	private List<RemoteTransaction> fetchRemoteTransactions(final UpdateOperationResult results, final List<Trade> trades,
			final RemoteTransactionStatus... remoteStatuses) {
		final List<SignalProviderName> relevantProviders = trades.stream().map(trade -> trade.getProviderName()).collect(Collectors.toList());

		final List<String> relevantAccountIds = relevantProviders.stream().map(providerName -> {
			final SignalProviderConfiguration configuration = signalProvidersConfiguration.getForProvider(providerName);
			return (configuration == null) ? null : configuration.getAccountId(); // handle the disabled adapters
		}).filter(value -> value != null).collect(Collectors.toList());
		final List<RemoteTransaction> fetchedRts = Lists.newArrayList();
		relevantAccountIds.forEach(accountId -> {
			Stream.of(remoteStatuses).forEach(status -> {
				final List<RemoteTransaction> relevantRts = client.getAllRemoteTransactionsForStatus(accountId, status);
				relevantRts.forEach(rt -> rt.setAccountId(accountId));
				results.called();
				fetchedRts.addAll(relevantRts);
			});
		});

		return fetchedRts;
	}

	/**
	 * searches for a RT with the same ID as the remoteID of the trade
	 */
	private Optional<RemoteTransaction> findRtForTrade(final List<RemoteTransaction> rtToSearchIn, final Trade trade) {
		final Optional<RemoteTransaction> rtOpt = rtToSearchIn.stream().filter(rt -> {
			return trade.getRemoteId().equals(rt.getId()) && signalProvidersConfiguration.findTradingAccountId(trade) == rt.getAccountId();
		}).findAny();

		return rtOpt;
	}

	public String getCronJob() {
		return cronJob;
	}

	/**
	 * @return the number of trades that were closed during this sync
	 */
	@Transactional
	public UpdateOperationResult synchronizeTrades() {
		final UpdateOperationResult results = new UpdateOperationResult();
		final TradeSynchronizationException tse = new TradeSynchronizationException();

		final List<Trade> allOpenTrades = tradingService.getTradesByStatus(TradeStatus.OPENED);
		final List<Trade> allPendingTrades = tradingService.getTradesByStatus(TradeStatus.PENDING);

		// If have open trades, fetch both the remote open and closed transactions and update them if needed
		if (!allOpenTrades.isEmpty()) {
			final List<RemoteTransaction> relevantRts = fetchRemoteTransactions(results, allOpenTrades, RemoteTransactionStatus.CLOSED,
					RemoteTransactionStatus.OPEN);
			// now do the update
			allOpenTrades.forEach(openTrade -> {
				if (!signalProvidersConfiguration.hasConfigurationForProvider(openTrade)) {
					// provider is disabled, don't do anything
					return;
				}
				final Optional<RemoteTransaction> rtOpt = findRtForTrade(relevantRts, openTrade);
				if (rtOpt.isPresent()) {
					final RemoteTransaction rt = rtOpt.get();
					if (rt.getStatus() == RemoteTransactionStatus.CLOSED || rt.getStatus() == RemoteTransactionStatus.CANCELLED) {
						tradingService.updateTradeForClosedRti(openTrade, rt); // found open trade that was just closed. therefore close it
						results.closed();
					} else {
						tradingService.updateTradeForOpenRti(openTrade, rt); // found trade that is still open. update it
						results.updated();
					}
				} else { // could not find a matching remote trade
					closeTradeWithErrorAndLog(openTrade);
				}
			});
		}

		// if have pending trades, fetch the pending ones and check if they need to be updated too
		if (!allPendingTrades.isEmpty()) {
			final List<RemoteTransaction> relevantRts = fetchRemoteTransactions(results, allPendingTrades, RemoteTransactionStatus.PENDING);
			allPendingTrades.forEach(pendingTrade -> {
				if (!signalProvidersConfiguration.hasConfigurationForProvider(pendingTrade)) {
					// provider is disabled, don't do anything
					return;
				}

				final Optional<RemoteTransaction> rtOpt = findRtForTrade(relevantRts, pendingTrade);

				// sometimes pending trades are not included in the response and we need to fetch them individually. Blame OANDA
				if (!rtOpt.isPresent()) {
					final String accountId = signalProvidersConfiguration.findTradingAccountId(pendingTrade);
					logger.warn("Re-requesting information for pending trade #" + pendingTrade.getId() + " ...");
					final RemoteTransaction rt = client.getRemoteTransaction(accountId, pendingTrade.getRemoteId(), RemoteTransactionStatus.PENDING);
					rt.setAccountId(accountId);
					results.called();
					relevantRts.add(rt);
				}

				// now try again. if the trade was fetched before it will be in the list and thus will be found
				final Optional<RemoteTransaction> rtOptSecondAttempt = findRtForTrade(relevantRts, pendingTrade);
				if (rtOptSecondAttempt.isPresent()) {
					final RemoteTransaction rt = rtOptSecondAttempt.get();
					// finally, do the update itself
					if (rt.getStatus() == RemoteTransactionStatus.FILLED) { // pending order that was triggered. open it
						// TODO fetch the actual trade and set the price etc
						tradingService.updateTradeForFillRti(pendingTrade, rt);
						results.opened();
					} else if (rt.getStatus() == RemoteTransactionStatus.CLOSED || rt.getStatus() == RemoteTransactionStatus.CANCELLED) {
						tradingService.updateTradeForClosedRti(pendingTrade, rt);
						results.closed();
					}
				} else { // could not find remote trade
					closeTradeWithErrorAndLog(pendingTrade);
				}
			});
		}

		// check for errors and throw if needed
		if (tse.getTradeUpdateExceptions().size() > 0 || tse.getCause() != null) {
			throw tse;
		}

		return results;
	}

}
