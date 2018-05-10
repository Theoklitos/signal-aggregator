package com.quantbro.aggregator.trading;

import com.quantbro.aggregator.domain.Side;

/**
 * a bid/ask price pair
 */
public final class Price {

	private final double bid;
	private final double ask;

	public Price(final double bid, final double ask) {
		this.bid = bid;
		this.ask = ask;
	}

	public double getAsk() {
		return ask;
	}

	public double getBid() {
		return bid;
	}

	public double getForSide(final Side side) {
		return side.equals(Side.SELL) ? ask : bid;
	}

	@Override
	public String toString() {
		return "Bid: " + bid + ", ask: " + ask + ", avg: " + (bid + ask) / 2;
	}
}
