package com.quantbro.aggregator.adapters;

import java.util.Optional;

/**
 * Anything that can go wrong during signal scraping/parsing
 */
public final class ScrapingException extends Error {

	private static final long serialVersionUID = -8779387773494926216L;

	private byte[] screenshot;

	public ScrapingException(final Exception e) {
		super(e);
	}

	public ScrapingException(final String message) {
		super(message);
	}

	public ScrapingException(final Throwable t) {
		super(t);
	}

	public Optional<byte[]> getScreenshot() {
		if (screenshot == null || screenshot.length == 0) {
			return Optional.empty();
		} else {
			return Optional.of(screenshot);
		}
	}

	public void setScreenshot(final byte[] screenshot) {
		this.screenshot = screenshot;
	}
}
