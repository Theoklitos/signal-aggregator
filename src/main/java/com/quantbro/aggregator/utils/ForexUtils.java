package com.quantbro.aggregator.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Trade;

/**
 * functions related to the currency trading and some numerical operations
 */
public class ForexUtils {

	@Deprecated
	public static double calculatePips(final BigDecimal entryPrice, final double currentOrClosingPrice, final Instrument instrument, final Side side) {
		final double priceDifference = (currentOrClosingPrice - entryPrice.doubleValue());
		double pips = priceDifference * instrument.getPipValue();
		if (side.equals(Side.SELL)) {
			pips = -pips;
		}
		final BigDecimal pipsBd = new BigDecimal(pips);
		pipsBd.setScale(1, RoundingMode.HALF_UP);
		return pipsBd.doubleValue();
	}

	/**
	 * Calculates the pips (positive or negative) based on open/close prices and the trade's instrument
	 */
	@Deprecated
	public static double calculatePips(final Trade trade, final double currentOrClosingPrice) {
		return calculatePips(trade.getEntryPrice(), currentOrClosingPrice, trade.getSignal().getInstrument(), trade.getSignal().getSide());
	}

	/**
	 * returns false is the number is zero, null or less than zero
	 */
	public static boolean isPositiveNumber(final BigDecimal amount) {
		return (amount != null) && (amount.compareTo(BigDecimal.ZERO) > 0);
	}

}
