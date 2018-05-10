package com.quantbro.aggregator.adapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.adapters.scraping.ScrapingUtils;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.utils.StringUtils;

@Component
public class AtozforexAdapter extends AbstractSignalProviderAdapter {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(AtozforexAdapter.class);

	@Override
	public SignalProviderName getName() {
		return SignalProviderName.ATOZFOREX;
	}

	@Override
	protected List<Signal> getSignalsImpl() {
		final List<Signal> result = new ArrayList<Signal>();

		login();

		driver.get(getRootUrl() + "/forex-signals");

		// get all the daily signal panels
		final List<WebElement> dailySignalPanelsList = driver.findElementsByClassName("signals__item");
		if (dailySignalPanelsList.isEmpty()) {
			throw new ScrapingException("No daily signal panels found in atozforex!");
		} else { // actually, get the first
			final WebElement latestDailySignalPanel = dailySignalPanelsList.get(0);
			final String date = latestDailySignalPanel.findElement(By.className("info-panel__date")).getText();
			final DateTime dailyDate = parseDate(date);
			final DateTime today = new DateTime();
			if (!(dailyDate.getDayOfYear() == today.getDayOfYear())) {
				throw new ScrapingException(
						"Atozforex does not have signals for this day (" + dailyDate.getDayOfMonth() + "/" + dailyDate.getMonthOfYear() + ")");
			} else {
				// go to today's signals page
				latestDailySignalPanel.findElement(By.tagName("a")).click();
				// finally, parse the signals
				driver.findElementByClassName("signal-field").findElements(By.tagName("p")).stream().forEach(element -> {
					final String rawText = element.getText();
					if (StringUtils.isBlank(rawText) || rawText.contains("Free Forex Signals") || rawText.contains("Entry size:")
							|| rawText.contains("Note:")) {
						return;
					}
					try {
						final Signal newSignal = parseAtozStringToSignal(rawText);
						result.add(newSignal);
					} catch (final IllegalArgumentException e) {
						logger.error("Cannot parse instrument " + e.getMessage() + ", will ignore it.");
					}
				});
			}
		}

		return result;
	}

	private void login() {
		driver.get(getRootUrl() + "/login");
		driver.findElementById("user_login").sendKeys(getUsername());
		driver.findElementById("user_pass").sendKeys(getPassword());
		driver.findElementById("wp-submit").click();
		ScrapingUtils.waitForPresenceOfElementWithClass(driver, "user-profile");
		if (!driver.getPageSource().contains("teo_teo")) {
			throw new ScrapingException("Could not log in to atozforex");
		}
	}

	private Signal parseAtozStringToSignal(final String rawSignalString) {
		final Signal newSignal = new Signal(getName());

		// start date - sadly we're stuck with now()
		final DateTime now = new DateTime();
		newSignal.setStartDate(now);

		// side
		if (rawSignalString.contains("SELL")) {
			newSignal.setSide(Side.SELL);
		} else if (rawSignalString.contains("BUY")) {
			newSignal.setSide(Side.BUY);
		}

		// instrument
		final String instrumentString = StringUtils.substringUntilWhitespace(rawSignalString, "–  ");
		try {
			final Instrument instrument = Instrument.parse(instrumentString);
			newSignal.setInstrument(instrument);
		} catch (final IllegalArgumentException e) { // make the exception's message prettier
			throw new IllegalArgumentException(instrumentString);
		}

		// entry price
		final BigDecimal entryPrice = new BigDecimal(StringUtils.removeLastCharacter(StringUtils.substringUntilWhitespace(rawSignalString, "@")));
		newSignal.setEntryPrice(entryPrice);

		// sl and tp. we use the modest tp (the "tp1") and ignore the others
		final BigDecimal stopLossPrice = new BigDecimal(StringUtils.removeLastCharacter(StringUtils.substringUntilWhitespace(rawSignalString, "SL@")));
		newSignal.setStopLoss(stopLossPrice);
		final BigDecimal takeProfitPrice = new BigDecimal(StringUtils.removeLastCharacter(StringUtils.substringUntilWhitespace(rawSignalString, "TP1@")));
		newSignal.setTakeProfit(takeProfitPrice);

		return newSignal;
	}

	private DateTime parseDate(final String dateString) {
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("MMM dd, YYYY H:mm aa");
		return formatter.parseDateTime(dateString);
	}

}
