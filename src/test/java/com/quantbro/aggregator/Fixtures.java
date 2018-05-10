package com.quantbro.aggregator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.trading.RemoteTransaction;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTradeCloseReason;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTransactionStatus;

public final class Fixtures {

	public static RemoteTransaction createClosedRti(final RemoteTradeCloseReason reason, final String remoteId) {
		return createRti(RemoteTransactionStatus.CLOSED, reason, remoteId);
	}

	public static RemoteTransaction createOpenRti(final String remoteId) {
		return createRti(RemoteTransactionStatus.OPEN, null, remoteId);
	}

	public static Trade createOpenTrade(final SignalProviderName name, final String remoteId) {
		return createTrade(name, TradeStatus.OPENED, remoteId);
	}

	public static RemoteTransaction createRti(final RemoteTransactionStatus status, final RemoteTradeCloseReason reason, final String remoteId) {
		final RemoteTransaction rti = new RemoteTransaction(null);
		rti.setId(remoteId);
		rti.setCloseReason(reason);
		rti.setStatus(status);
		return rti;
	}

	public static Signal createSignal(final SignalProviderName name, final SignalStatus status, final Instrument instrument, final Side side) {
		final Signal signal = new Signal(name);
		signal.setInstrument(instrument);
		signal.setSide(side);
		signal.setStatus(status);
		signal.setTakeProfit(BigDecimal.valueOf(11.1));
		signal.setStopLoss(BigDecimal.valueOf(16.1));
		return signal;
	}

	public static Signal createSignalForProvider(final SignalProviderName name) {
		return createSignalForProviderWithStatus(name, SignalStatus.LIVE);
	}

	public static Signal createSignalForProviderAndInstrument(final SignalProviderName name, final Instrument instrument) {
		return createSignal(name, SignalStatus.LIVE, instrument, Side.SELL);
	}

	public static Signal createSignalForProviderWithStatus(final SignalProviderName name, final SignalStatus status) {
		return createSignal(name, status, Instrument.EUR_GBP, Side.SELL);
	}

	public static List<Signal> createSignalsForProvider(final int noOfSignals, final SignalProviderName name) {
		return IntStream.range(0, noOfSignals).mapToObj(i -> {
			return createSignalForProvider(name);
		}).collect(Collectors.toList());
	}

	public static Trade createTrade(final SignalProviderName name, final BigDecimal pl, final TradeStatus status) {
		final Trade trade = new Trade();
		trade.setProviderName(name);
		trade.setPL(pl);
		return trade;
	}

	public static Trade createTrade(final SignalProviderName name, final TradeStatus status, final String remoteId) {
		final Trade trade = new Trade();
		trade.setProviderName(name);
		trade.setStatus(status);
		trade.setRemoteId(remoteId);
		return trade;
	}

	public static Trade getTradeForSignal(final Signal signal) {
		final Trade trade = new Trade();
		signal.setTrade(trade);
		trade.setSignal(signal);
		return trade;
	}

}
