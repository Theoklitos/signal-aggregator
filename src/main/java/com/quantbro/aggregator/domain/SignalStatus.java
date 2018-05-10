package com.quantbro.aggregator.domain;

public enum SignalStatus {
	/**
	 * A provider is currently advertising this signal
	 */
	LIVE,

	/**
	 * This signal is no longer mentioned by its provider
	 */
	CLOSED,

	/**
	 * This signal is live, but we were too late in detecting it and thus we consider it stale
	 */
	STALE
}
