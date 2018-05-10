package com.quantbro.aggregator.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.trading.ForexClient;
import com.quantbro.aggregator.trading.RemoteTransaction;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTransactionStatus;
import com.quantbro.aggregator.trading.TradingException;

@Service
public class TradingService {

	private final static Logger logger = LoggerFactory.getLogger(TradingService.class);

	@Autowired
	private ForexClient forexClient;

	@Autowired
	private Emailer emailer;

	@Autowired
	private SignalProviderConfigurationHolder signalProviderConfigurations;

	@Autowired
	private TradeRepository tradeRepository;

	@Value("${trading.unitsToTrade}")
	private int units;

	/**
	 * Closes the trade via the broker and updates the trade's values based on the results. Will escalate if something goes wrong.
	 */
	@Transactional
	public void closeTrade(final Trade trade, final TradeStatus reasonForClosing) {
		try {
			final String accountId = signalProviderConfigurations.findTradingAccountId(trade);
			final RemoteTransaction closedRti = forexClient.closeTrade(accountId, trade.getRemoteId());
			updateTradeForClosedRti(trade, closedRti);
			if (!(trade.getStatus() == TradeStatus.CANCELLED)) {
				trade.setStatus(reasonForClosing);
			}
		} catch (final TradingException e) {
			e.printStackTrace();
			trade.endTradeForException(e);
			emailer.escalate("Could not close trade",
					"Trade " + trade + " was meant to be closed because " + reasonForClosing + ", but we got an error: " + e.getMessage(), Optional.of(e));
		}
	}

	@Transactional(readOnly = true)
	public List<Trade> findLatestClosedTrades(final int numberOfTrades) {
		return tradeRepository.findLatestClosedTrades(numberOfTrades);
	}

	/**
	 * returns closed trades of any reason
	 */
	@Transactional
	public List<Trade> getAllClosedTrades() {
		return tradeRepository.findAllClosedTrades();
	}

	@Transactional
	public List<Trade> getAllTrades() {
		return Lists.newArrayList(tradeRepository.findAll());
	}

	@Transactional
	public List<Trade> getNonClosedTradesForSignalProvider(final SignalProviderName name) {
		return tradeRepository.findNonClosedTradesByProviderName(name);
	}

	@Transactional
	public Trade getTradeById(final long id) {
		return tradeRepository.findOne(id);
	}

	@Transactional
	public Trade getTradeByRemoteId(final String remoteId) {
		return tradeRepository.findByRemoteId(remoteId);
	}

	@Transactional
	public List<Trade> getTradesByStatus(final TradeStatus status) {
		return tradeRepository.findByStatus(status);
	}

	protected int getUnitsToTrade() {
		return units;
	}

	@Transactional
	public Trade openTradeForSignal(final Signal signal) {
		final Trade newTrade = new Trade(signal.getProviderName());
		newTrade.setSignal(signal);
		final String accountId = signalProviderConfigurations.findTradingAccountId(newTrade);
		boolean wasExceptionThrown = false;

		try {
			final RemoteTransaction newRt = forexClient.openTrade(accountId, units, signal.getInstrument(), signal.getSide(), signal.getEntryPrice(),
					signal.getStopLoss(), signal.getTakeProfit());
			newTrade.setRemoteId(newRt.getId());
			newTrade.setEntryPrice(newRt.getPrice());
			newTrade.setJson(newRt.getPrettyJson());
			final TradeStatus tradeStatus = newRt.getStatus().equals(RemoteTransactionStatus.PENDING) ? TradeStatus.PENDING : TradeStatus.OPENED;
			newTrade.setStatus(tradeStatus);
		} catch (final TradingException e) {
			wasExceptionThrown = true;
			e.printStackTrace();
			newTrade.endTradeForException(e);
			emailer.escalate("Could not open trade", "A trade was triggered by signal #" + signal.getId() + " but could not open.", Optional.of(e));
		}

		if (wasExceptionThrown) {
			logger.info("Error when opening trade for [" + signal.getProviderName() + "] signal #" + signal.getId());
		} else {
			final String extraInfo = newTrade.getStatus() == TradeStatus.PENDING ? "a pending" : "";
			logger.info("Succesfully opened " + extraInfo + " trade for [" + signal.getProviderName() + "] signal #" + signal.getId());
		}

		return tradeRepository.save(newTrade);
	}

	protected void setUnitsToTrade(final int units) {
		this.units = units;
	}

	/**
	 * synchronizes our local trade with the remote {@link RemoteTransaction} one.
	 *
	 * Note: Does not open a transaction
	 *
	 * @throws TradeUpdateException
	 *
	 */
	public void updateTradeForClosedRti(final Trade openOrPendingTrade, final RemoteTransaction closedRti) throws TradeUpdateException {
		if (!(openOrPendingTrade.getStatus().equals(TradeStatus.OPENED) || openOrPendingTrade.getStatus().equals(TradeStatus.PENDING))
				|| (!(closedRti.getStatus() == RemoteTransactionStatus.CLOSED || (closedRti.getStatus() != RemoteTransactionStatus.PENDING)))) {
			throw new TradeUpdateException(
					"Cannot sync expected open or pending trade [" + openOrPendingTrade + " with expected closed rti [" + closedRti + "]");
		}

		TradeStatus closedTradeStatus = null;
		if (closedRti.getCloseReason() != null) {
			switch (closedRti.getCloseReason()) {
			case ORDER:
				closedTradeStatus = TradeStatus.CLOSED_BY_ORDER;
				break;
			case STOPLOSS:
				closedTradeStatus = TradeStatus.CLOSED_BY_STOPLOSS;
				break;
			case TAKEPROFIT:
				closedTradeStatus = TradeStatus.CLOSED_BY_TAKEPROFIT;
				break;
			}
		}

		// hack. he handle the cancelled pending order here, in an ugly way.
		if (openOrPendingTrade.getStatus() == TradeStatus.PENDING && closedRti.getStatus() == RemoteTransactionStatus.CANCELLED) {
			openOrPendingTrade.cancelTrade();
			openOrPendingTrade.setMessage("Order was cancelled before it was filled");
			logger.warn("Pending order #" + openOrPendingTrade.getId() + " was cancelled.");
			return;
		}

		if (openOrPendingTrade.getSignal().equals(TradeStatus.PENDING)) {
			logger.debug("Re-assinging pending trade's #" + openOrPendingTrade.getId() + " remote ID from " + openOrPendingTrade.getId() + " to "
					+ closedRti.getRelatedId());
			openOrPendingTrade.setRemoteId(closedRti.getRelatedId());
		}

		final String prefix = openOrPendingTrade.getStatus().equals(TradeStatus.PENDING) ? "Pending trade #" : "Open trade #";
		final BigDecimal closingPrice = closedRti.getClosingPrice();
		final BigDecimal pl = closedRti.getPl();
		openOrPendingTrade.endTrade(closedTradeStatus, closingPrice, pl);
		openOrPendingTrade.setJson(closedRti.getPrettyJson());
		logger.info(prefix + openOrPendingTrade.getId() + " was closed for reason " + closedTradeStatus + ". Final PL: " + openOrPendingTrade.getPl());
	}

	/**
	 * synchronizes our local trade with the remote {@link RemoteTransaction} one.
	 *
	 * Note: Does not open a transaction
	 *
	 * @throws TradeUpdateException
	 *
	 */
	public void updateTradeForFillRti(final Trade pendingTrade, final RemoteTransaction freshlyOpenRti) throws TradeUpdateException {
		if (pendingTrade.getStatus() != TradeStatus.PENDING || freshlyOpenRti.getStatus() != RemoteTransactionStatus.FILLED) {
			throw new TradeUpdateException("Cannot sync expected pending trade [" + pendingTrade + " with expected open rti [" + freshlyOpenRti + "]");
		}
		pendingTrade.setStatus(TradeStatus.OPENED);
		pendingTrade.setPL(freshlyOpenRti.getPl());
		pendingTrade.setJson(freshlyOpenRti.getPrettyJson());
		pendingTrade.setEntryPrice(freshlyOpenRti.getPrice());
		logger.debug("Re-assinging pending trade's #" + pendingTrade.getId() + " remote ID from " + pendingTrade.getRemoteId() + " to "
				+ freshlyOpenRti.getRelatedId());
		pendingTrade.setRemoteId(freshlyOpenRti.getRelatedId());

		logger.info("Pending trade #" + pendingTrade.getId() + " was filled.");
	}

	/**
	 * synchronizes our local trade with the remote {@link RemoteTransaction} one.
	 *
	 * Note: Does not open a transaction
	 *
	 * @throws TradeUpdateException
	 */
	public void updateTradeForOpenRti(final Trade openTrade, final RemoteTransaction openRti) throws TradeUpdateException {
		if (openTrade.getStatus() != TradeStatus.OPENED || openRti.getStatus() != RemoteTransactionStatus.OPEN) {
			throw new TradeUpdateException("Cannot sync expected open trade [" + openTrade + " with expected open rti [" + openRti + "]");
		}
		openTrade.setPL(openRti.getPl());
		openTrade.setJson(openRti.getPrettyJson());
	}

}
