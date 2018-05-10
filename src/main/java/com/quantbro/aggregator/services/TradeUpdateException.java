package com.quantbro.aggregator.services;

/**
 * An error when updating/changing the state or a Trade
 *
 */
public class TradeUpdateException extends Error {

	private static final long serialVersionUID = -5577633887181952647L;

	public TradeUpdateException(final String message) {
		super(message);
	}

}
