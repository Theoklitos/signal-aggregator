package com.quantbro.aggregator.configuration;

import java.io.File;

import org.joda.time.Period;

import com.quantbro.aggregator.scraping.ScrapingType;

/**
 * a POJO that holds various configurations and options that determine how a signal provider behaves (or should behave)
 */
public class SignalProviderConfiguration {

	private final String cronString;
	private final String rootUrl;
	private final Period randomIntervalPeriod;
	private final ScrapingType type;
	private final boolean shouldTradesCloseSignals;
	private final boolean shouldSignalsCloseTrades;
	private final String accountId;
	private final File dumpFolderFile;
	private final String username;
	private final String password;

	public SignalProviderConfiguration(final String cronString, final String rootUrl, final ScrapingType type, final boolean shouldTradesCloseSignals,
			final boolean shouldSignalsCloseTrades, final Period randomIntervalPeriod, final String accountId, final File dumpFolderFile, final String username,
			final String password) {
		this.cronString = cronString;
		this.rootUrl = rootUrl;
		this.type = type;
		this.shouldTradesCloseSignals = shouldTradesCloseSignals;
		this.shouldSignalsCloseTrades = shouldSignalsCloseTrades;
		this.randomIntervalPeriod = randomIntervalPeriod;
		this.accountId = accountId;
		this.dumpFolderFile = dumpFolderFile;
		this.username = username;
		this.password = password;
	}

	public String getAccountId() {
		return accountId;
	}

	public String getCronString() {
		return cronString;
	}

	public File getDumpFolder() {
		return dumpFolderFile;
	}

	public String getPassword() {
		return password;
	}

	public Period getRandomIntervalPeriod() {
		return randomIntervalPeriod;
	}

	public String getRootUrl() {
		return rootUrl;
	}

	public ScrapingType getType() {
		return type;
	}

	public String getUsername() {
		return username;
	}

	public boolean shouldDump() {
		return dumpFolderFile != null;
	}

	public boolean shouldSignalsCloseTrades() {
		return shouldSignalsCloseTrades;
	}

	public boolean shouldTradesCloseSignals() {
		return shouldTradesCloseSignals;
	}

	@Override
	public String toString() {
		return accountId;
	}

}
