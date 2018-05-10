package com.quantbro.aggregator.domain;

import org.assertj.core.util.Lists;

public enum TradeStatus {
	/**
	 * When the remote trade is declared, before any external actions are taken
	 */
	INITIALIZED,

	/**
	 * An order was placed for this trade, but it is waiting on some condition for it to open
	 */
	PENDING,

	/**
	 * There is a corresponding trade opened via an external broker
	 */
	OPENED,

	/**
	 * this was a pending trade and it was cancelled
	 */
	CANCELLED,

	/**
	 * If the signal of this trade was closed
	 */
	CLOSED_BY_ORDER,

	/**
	 * if TP price was reached
	 */
	CLOSED_BY_TAKEPROFIT,

	/**
	 * if SL price was reached
	 */
	CLOSED_BY_STOPLOSS,

	/**
	 * Something went wrong. The message should contain more info.
	 */
	ERROR;

	public boolean isClosed() {
		return Lists.newArrayList(TradeStatus.CLOSED_BY_ORDER, TradeStatus.CLOSED_BY_STOPLOSS, TradeStatus.CLOSED_BY_TAKEPROFIT).contains(this);
	}

}
