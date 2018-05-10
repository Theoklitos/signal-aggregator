package com.quantbro.aggregator.adapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.utils.StringUtils;

@Component
public class ForesignalAdapter extends AbstractSignalProviderAdapter {

	private final static Logger logger = LoggerFactory.getLogger(ForesignalAdapter.class);

	/**
	 * swallows exceptions
	 */
	private String findElementByClassNameSafe(final WebElement item, final String className) {
		try {
			return item.findElement(By.className(className)).getText();
		} catch (final Exception e) {
			return "";
		}
	}

	private DateTime getAsDateTime(final String value) {
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY M d HH:mm:ss");
		final String hourAndMinutes = value.substring(10);
		final DateTime now = new DateTime();
		final String fullString = now.getYear() + " " + now.getMonthOfYear() + " " + now.getDayOfMonth() + " " + hourAndMinutes + ":00";
		return DateTime.parse(fullString, formatter);
	}

	@Override
	public SignalProviderName getName() {
		return SignalProviderName.FORESIGNAL;
	}

	@Override
	protected List<Signal> getSignalsImpl() {
		final List<Signal> result = new ArrayList<Signal>();

		try {
			driver.get(getRootUrl());
			if (driver.getPageSource().contains("Daily limit")) {
				logger.debug("Foresignal started to hit us with daily limits; might want to check this out");
				tryLogin(driver, getUsername(), getPassword());
			}

			if (driver.getPageSource().contains("Daily limit")) {
				driver.findElementByXPath("/html/body/div/div[2]/nav/div/ul/li[3]/a").click(); // logout
				tryLogin(driver, "tonis", "purrr123"); // hack. try with alternate account
			}

			if (driver.getPageSource().contains("Daily limit")) {
				throw new ScrapingException("Daily use exceeded, signals blocked.");
			}

			if (driver.getPageSource().contains("We're sorry but your computer or network may be sending automated queries.")) {
				throw new ScrapingException("We've been CAPTCHA'd by foresignal.com");
			}

			final List<WebElement> signalElements = driver.findElementsByClassName("signal-cell");
			final List<Signal> scrapedSignals = signalElements.stream().filter(signalElement -> {
				try {
					final String signalStatus = signalElement.findElement(By.className("signal-status")).getText().trim();
					return !signalStatus.equals("Filled") && !signalStatus.equals("Cancelled"); // ignore the filled or cancelled signals
				} catch (final NoSuchElementException e) {
					return false;
				}
			}).map(signalElement -> {
				if (signalElement.toString().contains("Subscribers have instant access to the signals")) { // they are onto us!
					throw new ScrapingException("At least one signals is blocked.");
				}

				final Signal newSignal = new Signal(getName());
				final Instrument instrument = Instrument.parse(signalElement.findElement(By.cssSelector("[title]")).getText());
				newSignal.setInstrument(instrument);
				final Side side = Side.valueOf(signalElement.findElement(By.className("signal-status")).getText().toUpperCase());
				newSignal.setSide(side);

				final List<WebElement> signalItems = signalElement.findElements(By.className("signal-item"));
				for (int i = 0; i < signalItems.size(); i++) {
					final WebElement item = signalItems.get(i);
					if (i == 0) {
						final String timeAgoString = signalElement.findElement(By.className("signal-color")).getText();
						final DateTime startDate = getStartDateTime(timeAgoString.trim());
						newSignal.setStartDate(startDate);
						continue;
					}

					final String value = findElementByClassNameSafe(item, "signal-value");
					if (i == 1) { // start date
						// final DateTime startDate = getAsDateTime(value);
						// newSignal.setStartDate(startDate); // we read this value based on the time-ago string
					} else if (i == 2) { // end date
						// what to do with this info?
						@SuppressWarnings("unused")
						final DateTime endDate = getAsDateTime(value);
					} else if (i == 4) { // price
						newSignal.setEntryPrice(new BigDecimal(value));
					} else if (i == 5) { // takeprofit
						newSignal.setTakeProfit(new BigDecimal(value));
					} else if (i == 6) { // stop loss
						newSignal.setStopLoss(new BigDecimal(value));
					}
				}

				return newSignal;
			}).collect(Collectors.toList());
			result.addAll(scrapedSignals);
		} catch (final Exception e) {
			throw new ScrapingException(e);
		}

		if (result.size() == 0) {
			throw new ScrapingException("Foresignal adapter did not scrape any signals. This is unlikely. Check the output/screenshot to make sure.");
		}

		return result;
	}

	/**
	 * foresignal tells us when the signal opened like this: "3 hours, 26 minutes ago"
	 */
	private DateTime getStartDateTime(final String value) {
		final Duration durationAgo = StringUtils.parseTimeAgoToDuration(value);
		return new DateTime().minus(durationAgo);
	}

	private void tryLogin(final PhantomJSDriver driver, final String username, final String password) {
		driver.get(getRootUrl() + "/login");
		driver.findElementById("user_name").sendKeys(username);
		driver.findElementById("user_password").sendKeys(password);
		driver.findElementByClassName("btn-primary").click();
		if (driver.getCurrentUrl().contains("captcha")) {
			throw new ScrapingException("Encountered captcha. Seems like they detected our scraping.");
		}
	}

}
