package com.quantbro.aggregator.adapters;

/**
 * When an adapter could not be initialized (e.g. due to missing properties)
 */
public class AdapterInitializationException extends Error {

	private static final long serialVersionUID = 1015238967335688067L;
	private final SignalProviderName name;

	public AdapterInitializationException(final SignalProviderName name) {
		this.name = name;
	}

	public SignalProviderName getAdapterName() {
		return name;
	}

}
