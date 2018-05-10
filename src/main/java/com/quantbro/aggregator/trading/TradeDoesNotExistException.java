package com.quantbro.aggregator.trading;

public class TradeDoesNotExistException extends Error {

	private static final long serialVersionUID = 3256072816511643765L;

	public TradeDoesNotExistException(final String remoteId) {
		super("Trade with id \"" + remoteId + "\" does not exist!");
	}
}
