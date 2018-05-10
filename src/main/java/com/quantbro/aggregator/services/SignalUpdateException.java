package com.quantbro.aggregator.services;

/**
 * An error when updating the signals in the DB
 *
 */
public class SignalUpdateException extends Error {

	private static final long serialVersionUID = 4648322942505106348L;

	public SignalUpdateException(final String message) {
		super(message);
	}

}
