package com.quantbro.aggregator.scraping;

public enum ScrapingType {

	/**
	 * will use jsoup or some similar simple parsing library. no js
	 */
	HTML(false),

	/**
	 * will use the phantomjs driver
	 */
	PHANTOMJS(true),

	/**
	 * will use chrome (chromedriver)
	 */
	CHROME(true),

	/**
	 * will receve signals by reading and parsing emails
	 */
	EMAIL(false);

	private final boolean shouldUseWebDriver;

	private ScrapingType(final boolean shouldUseWebDriver) {
		this.shouldUseWebDriver = shouldUseWebDriver;
	}

	public boolean usesWebDriver() {
		return shouldUseWebDriver;
	}
}
