package com.quantbro.aggregator.services;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.configuration.SignalProviderConfiguration;
import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.email.Emailer;

@Service
public class SignalService {

	private final static Logger logger = LoggerFactory.getLogger(SignalService.class);

	@Autowired
	private SignalRepository signalRepository;

	@Autowired
	private TradingService tradingService;

	@Autowired
	private SignalProviderConfigurationHolder configurations;

	@Autowired
	private Emailer emailer;

	@Transactional(readOnly = true)
	public Collection<Signal> findAllOrderedByPl() {
		signalRepository.findAll();
		return signalRepository.findAllOrderedByPl();
	}

	@Transactional(readOnly = true)
	public List<Signal> findLatestClosedSignals(final int numberOfClosedSignals) {
		return signalRepository.findLatestClosedSignals(15);
	}

	@Transactional(readOnly = true)
	public List<Signal> getAllSignals() {
		return Lists.newArrayList(signalRepository.findAll());
	}

	/**
	 * has a trade been opened, for this provider, that has the same instrument but the opposite side as the given signal? If yes, that trade will be returned in the optional
	 */
	private Optional<Trade> getOppositeTrade(final SignalProviderName signalProvider, final Signal signal) {
		final List<Trade> openAndPendingTrades = tradingService.getNonClosedTradesForSignalProvider(signalProvider).stream()
				.filter(trade -> (trade.getStatus() == TradeStatus.OPENED) || (trade.getStatus() == TradeStatus.PENDING)).collect(Collectors.toList());
		final Optional<Trade> oppositeTrade = openAndPendingTrades.stream().filter(openOrPendingTrade -> {
			final Signal signalOfTrade = openOrPendingTrade.getSignal();
			if (signalOfTrade != null) {
				return signalOfTrade.getInstrument().equals(signal.getInstrument()) && !signalOfTrade.getSide().equals(signal.getSide());
			}
			return false;
		}).findAny();

		return oppositeTrade;
	}

	@Transactional(readOnly = true)
	public List<Signal> getReadOnlySignalsByStatuses(final SignalStatus... statuses) {
		return signalRepository.findByStatuses(statuses);
	}

	@Transactional(readOnly = true)
	public Optional<Signal> getSignalById(final long signalId) {
		final Signal result = signalRepository.findOne(signalId);
		return result == null ? Optional.empty() : Optional.of(result);
	}

	@Transactional
	public List<Signal> getSignalsByStatus(final SignalStatus status) {
		return signalRepository.findByStatus(status);
	}

	@Transactional
	public List<Signal> getSignalsByStatuses(final SignalStatus... statuses) {
		return signalRepository.findByStatuses(statuses);
	}

	/**
	 * what happens when we get a signal but a trade already exists for the same instrument but for the opposite direction?
	 *
	 * @param trade
	 */
	private void handleOpposingTrades(final SignalProviderName providerToUpdate, final Signal signal, final Trade trade) {
		logger.info("Douchebag provider \"" + providerToUpdate + " \" gave a signal that is opposite to an existing trade. Setting signal #" + signal.getId()
				+ "'s to STALE.");
		signal.setStatus(SignalStatus.STALE);
		emailer.escalate("Opposing signals/trades!", "Provider " + providerToUpdate + " just sent out a " + signal.getInstrument() + " " + signal.getSide()
				+ " signal that would open a trade opposite to an existing " + trade.getStatus() + " one. You should investigate.", Optional.empty());
	}

	/**
	 * should this provider had its trades close when their parent signals close?
	 */
	private boolean shouldCloseTrades(final SignalProviderName providerToUpdate) {
		final SignalProviderConfiguration configuration = configurations.getForProvider(providerToUpdate);
		if (configuration == null) {
			// provider is disabled
			return false;
		} else {
			return configuration.shouldSignalsCloseTrades();

		}
	}

	/**
	 *
	 * Updates the signals of a provider. Whether trades should be opened or closed can by set by the parameters
	 */
	@Transactional
	public UpdateOperationResult update(final SignalProviderName providerToUpdate, final List<Signal> signals, final boolean shouldOpenTradesForNewSignals,
			final boolean shouldCloseMissingSignals) {
		final UpdateOperationResult result = new UpdateOperationResult();

		// validate
		final AtomicInteger newSignals = new AtomicInteger(0);
		signals.parallelStream().forEach(signal -> {
			if (signal.getProviderName() != providerToUpdate) {
				throw new SignalUpdateException("Signal name \"" + signal.getProviderName() + "\" different than expected \"" + providerToUpdate + "\"");
			}
		});

		final List<Signal> openSignals = signalRepository.findByProviderNameAndStatus(providerToUpdate, SignalStatus.LIVE);
		final List<Signal> staleSignals = signalRepository.findByProviderNameAndStatus(providerToUpdate, SignalStatus.STALE);
		final List<Signal> openAndStaleSignals = Stream.concat(openSignals.stream(), staleSignals.stream()).collect(Collectors.toList());

		if (shouldCloseMissingSignals) {
			openAndStaleSignals.forEach(openOrStaleSignal -> {
				if (signals.stream().noneMatch(signal -> signal.matches(openOrStaleSignal))) {
					if (shouldCloseTrades(providerToUpdate)) {
						final Trade trade = openOrStaleSignal.getTrade();
						if (trade == null && openOrStaleSignal.getStatus().equals(SignalStatus.LIVE)) {
							logger.warn("About to close open signal #" + openOrStaleSignal.getId() + " but it did not have any corresponding trade opened.");
						} else if (trade != null && !trade.getStatus().isClosed()) {
							tradingService.closeTrade(trade, TradeStatus.CLOSED_BY_ORDER);
							result.called();
						}
					}
					openOrStaleSignal.close(); // the signal is officially closed
					logger.info("Closed signal #" + openOrStaleSignal.getId());
					result.closed();
				}
			});
		}

		// main functionality: check for new signals and open new trades
		final AtomicBoolean hasNewSignals = new AtomicBoolean(false);
		signals.stream().forEach(newSignal -> {
			// is the signal new or existing?
			if (!openAndStaleSignals.stream().anyMatch(openOrStaleSignal -> newSignal.matches(openOrStaleSignal))) {
				logger.info("New signal: " + newSignal);
				final Signal persistedSignal = signalRepository.save(newSignal);
				if (shouldOpenTradesForNewSignals) {
					final Optional<Trade> oppositeTradeOpt = getOppositeTrade(providerToUpdate, newSignal);
					if (oppositeTradeOpt.isPresent()) { // an edge case that has caused many bugs
						handleOpposingTrades(providerToUpdate, persistedSignal, oppositeTradeOpt.get());
					} else { // here, finally, the trade opens
						final Trade newTrade = tradingService.openTradeForSignal(persistedSignal);
						result.called();
						persistedSignal.setTrade(newTrade);
					}
				} else {
					logger.info("No trade will be opened during first run.");
					persistedSignal.setStatus(SignalStatus.STALE);
				}
				newSignals.incrementAndGet();
				hasNewSignals.set(true);
				result.opened();
			}
		});

		if (!hasNewSignals.get()) {
			logger.info("No new signals.");
		}

		return result;
	}

}
