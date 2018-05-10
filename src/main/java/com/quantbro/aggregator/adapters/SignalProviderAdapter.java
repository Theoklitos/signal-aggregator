package com.quantbro.aggregator.adapters;

import java.util.List;

import org.joda.time.Period;

import com.quantbro.aggregator.domain.Signal;

public interface SignalProviderAdapter {

	/**
	 * get the cron string that determines when/how often this adapter will scrape signals
	 */
	String getCronString();

	SignalProviderName getName();

	/**
	 * adapters can be updated in random monents inside each such interval
	 */
	Period getRandomIntervalPeriod();

	/**
	 * scrapes the provider's page to retrieve the current signals
	 */
	public List<Signal> getSignals() throws ScrapingException;

	/**
	 * adapters that are not configured are set to "disabled" which means they won't be scheduled at all
	 */
	boolean isEnabled();

}
