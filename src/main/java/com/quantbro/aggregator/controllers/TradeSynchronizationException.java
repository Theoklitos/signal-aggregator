package com.quantbro.aggregator.controllers;

import java.util.List;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.services.TradeUpdateException;

/**
 * when something goes wrong when contacting the remote broker (e.g. Oanda) and updating our local trades based on it
 */
public class TradeSynchronizationException extends Error {

	private static final long serialVersionUID = -8327566479945135310L;

	private final List<TradeUpdateException> tradeUpdateExceptions;

	public TradeSynchronizationException() {
		tradeUpdateExceptions = Lists.newArrayList();
	}

	public TradeSynchronizationException(final Throwable t) {
		super(t);
		tradeUpdateExceptions = Lists.newArrayList();
	}

	public void addException(final TradeUpdateException e) {
		getTradeUpdateExceptions().add(e);
	}

	public List<TradeUpdateException> getTradeUpdateExceptions() {
		return tradeUpdateExceptions;
	}

}
