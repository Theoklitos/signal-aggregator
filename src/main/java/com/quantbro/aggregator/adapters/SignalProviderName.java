package com.quantbro.aggregator.adapters;

public enum SignalProviderName {
	FXLEADERS("www.fxleaders.com"), FORESIGNAL("foresignal.com"), DUXFOREX("www.duxforex.com"), WETALKTRADE("www.wetalktrade.com"), ATOZFOREX("atozforex.com");

	private final String humanReadableName;

	SignalProviderName(final String humanReadableName) {
		this.humanReadableName = humanReadableName;
	}

	@Override
	public String toString() {
		return this.humanReadableName;
	}
}
