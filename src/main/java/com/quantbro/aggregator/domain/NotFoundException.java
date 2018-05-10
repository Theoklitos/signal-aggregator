package com.quantbro.aggregator.domain;

/**
 * When some data (from our models) was expected, but it was not found
 */
public final class NotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotFoundException(final String expectedId) {
		super("No item with ID '" + expectedId + "' found");
	}

}
