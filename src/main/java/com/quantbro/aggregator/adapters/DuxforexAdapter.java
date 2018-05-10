package com.quantbro.aggregator.adapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
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
public class DuxforexAdapter extends AbstractSignalProviderAdapter {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(DuxforexAdapter.class);

	@Override
	public SignalProviderName getName() {
		return SignalProviderName.DUXFOREX;
	}

	@Override
	protected List<Signal> getSignalsImpl() {
		final List<Signal> result = new ArrayList<Signal>();

		login();

		driver.get(getRootUrl() + "/forex-signals");
		final WebElement signalsTable = ScrapingUtils.waitForVisibilityOfElementWithId(driver, "jjshoutboxoutput");
		final List<WebElement> tableChildren = signalsTable.findElements(By.tagName("div"));
		for (final WebElement signalRow : tableChildren) {
			final String text = signalRow.getText();
			if (signalRow.getText().contains("HISTORY")) {
				break;
			} else if (text.contains("Signal") && text.contains("Entry:")) {
				final Signal signal = parseDuxStringToSignal(text);
				result.add(signal);
			}
		}

		return result;
	}

	private void login() {
		driver.get(getRootUrl() + "/component/users/?view=login");
		driver.findElementById("username").sendKeys(getUsername());
		driver.findElementById("password").sendKeys(getPassword());
		driver.findElementByClassName("btn-primary").click();
		final WebElement profileButton = ScrapingUtils.waitForVisibilityOfElementWithId(driver, "btl-panel-profile");
		if (!profileButton.getText().contains("Hi,")) {
			throw new ScrapingException("Could not log in to duxforex");
		}
	}

	private Signal parseDuxStringToSignal(final String rawSignalString) {
		final Signal newSignal = new Signal(getName());

		// start date
		final DateTime now = new DateTime();
		final String startTimeString = StringUtils.substringUntilWhitespace(rawSignalString, "- ");
		final int hour = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringBefore(startTimeString, ":").trim());
		final int minute = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(startTimeString, ":").trim());
		DateTime signalStartDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), hour, minute);
		if (signalStartDate.isAfter(now)) {
			signalStartDate = signalStartDate.minusDays(1);
		}
		newSignal.setStartDate(signalStartDate);

		// side and instrument
		if (rawSignalString.contains("SELL")) {
			newSignal.setSide(Side.SELL);
			final Instrument instrument = Instrument.parse(StringUtils.substringUntilWhitespace(rawSignalString, "SELL"));
			newSignal.setInstrument(instrument);
		} else if (rawSignalString.contains("BUY")) {
			newSignal.setSide(Side.BUY);
			final Instrument instrument = Instrument.parse(StringUtils.substringUntilWhitespace(rawSignalString, "BUY"));
			newSignal.setInstrument(instrument);
		}

		// entry price
		final BigDecimal entryPrice = new BigDecimal(StringUtils.substringUntilWhitespace(rawSignalString, "Entry:"));
		newSignal.setEntryPrice(entryPrice);

		// sl and tp. we use the modest tp (the "tp1") and ignore the others
		final BigDecimal stopLossPrice = new BigDecimal(StringUtils.substringUntilWhitespace(rawSignalString, "SL:"));
		newSignal.setStopLoss(stopLossPrice);
		final BigDecimal takeProfitPrice = new BigDecimal(StringUtils.substringUntilWhitespace(rawSignalString, "TP1:"));
		newSignal.setTakeProfit(takeProfitPrice);

		return newSignal;
	}

}
