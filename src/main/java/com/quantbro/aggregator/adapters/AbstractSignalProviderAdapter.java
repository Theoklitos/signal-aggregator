package com.quantbro.aggregator.adapters;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.adapters.scraping.ScrapingUtils;
import com.quantbro.aggregator.adapters.scraping.WebDriverFactory;
import com.quantbro.aggregator.configuration.SignalProviderConfiguration;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.scraping.ScrapingType;

/**
 * Basic and common scraping functionality
 */
public abstract class AbstractSignalProviderAdapter implements SignalProviderAdapter {

	private final static Logger logger = LoggerFactory.getLogger(AbstractSignalProviderAdapter.class);

	private SignalProviderConfiguration configuration;
	protected PhantomJSDriver driver;

	@Override
	public String getCronString() {
		return configuration.getCronString();
	}

	@Override
	public abstract SignalProviderName getName();

	public String getPassword() {
		return configuration.getPassword();
	}

	@Override
	public Period getRandomIntervalPeriod() {
		return configuration.getRandomIntervalPeriod();
	}

	public String getRootUrl() {
		return configuration.getRootUrl();
	}

	@Override
	public List<Signal> getSignals() throws ScrapingException {
		if (configuration.getType().usesWebDriver()) {
			driver = WebDriverFactory.createPhantomJsDriver();
			driver.manage().window().setSize(new Dimension(1920, 1080));
		}

		final List<Signal> scrapedSignals = Lists.newArrayList();
		try {
			scrapedSignals.addAll(getSignalsImpl());
			takeDump();
		} catch (final Throwable e) {
			handleExceptionDuringScrape(e);
		} finally {
			try {
				driver.quit();
			} catch (final NoSuchWindowException e) {
				// already closed, do nothing
			}
		}

		return scrapedSignals;
	}

	protected abstract List<Signal> getSignalsImpl();

	public String getUsername() {
		return configuration.getUsername();
	}

	/**
	 * self-explanatory
	 */
	private void handleExceptionDuringScrape(final Throwable e) throws ScrapingException {
		logger.error("Error while scraping: " + e.getLocalizedMessage());
		e.printStackTrace();
		ScrapingException wrapperException;
		if (e instanceof ScrapingException) {
			wrapperException = new ScrapingException(e.getLocalizedMessage());
		} else {
			wrapperException = new ScrapingException(e);
		}
		final byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
		if (screenshot != null && screenshot.length > 0 && hasUsedWebDriverInstance()) {
			logger.info("Took screenshot of scraping error.");
			wrapperException.setScreenshot(screenshot);
		}
		driver.quit();
		throw wrapperException;
	}

	/**
	 * was the webdriver instance used?
	 */
	private boolean hasUsedWebDriverInstance() {
		return driver != null && !driver.getCurrentUrl().trim().equals("about:blank");
	}

	@Override
	public boolean isEnabled() {
		return configuration != null;
	}

	/**
	 * make sure you know what you are doing if calling this
	 */
	protected void resetDriver() {
		try {
			driver.quit();
		} catch (final Exception e) {
			// its ok
		}
		driver = WebDriverFactory.createPhantomJsDriver();
		driver.manage().window().setSize(new Dimension(1920, 1080));
	}

	public void setConfiguration(final SignalProviderConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * best used only for testing
	 */
	protected void setConfiguration(final String rootUrl, final String username, final String password) {
		final SignalProviderConfiguration configuration = new SignalProviderConfiguration(null, rootUrl, ScrapingType.PHANTOMJS, false, false, null, null, null,
				username, password);
		setConfiguration(configuration);
	}

	/**
	 * handle all the html/screenshot dumping
	 */
	private void takeDump() {
		if (configuration.shouldDump()) {
			if (configuration.getType().usesWebDriver() && hasUsedWebDriverInstance()) { // store the screenshot and html
				final String filename = StringUtils
						.replace(StringUtils.replace(com.quantbro.aggregator.utils.StringUtils.getReadableDateTime(new DateTime()), " ", "_"), ":", "-");
				ScrapingUtils.takeScreenshotLocally(driver, configuration.getDumpFolder() + File.separator + filename + ".jpg");
				final String htmlString = driver.getPageSource();
				try {
					FileUtils.writeStringToFile(new File(configuration.getDumpFolder(), filename + ".html"), htmlString);
				} catch (final IOException e) {
					e.printStackTrace(); // don't do anything, just log
				}
			}
			// handle different types here!
		}
	}

}
