package com.quantbro.aggregator.adapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.adapters.scraping.ScrapingUtils;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;

@Component
public class WeTalkTradeAdapter extends AbstractSignalProviderAdapter {

	private final static Logger logger = LoggerFactory.getLogger(WeTalkTradeAdapter.class);

	private void checkIfServiceIsDown() throws ScrapingException {
		final List<WebElement> alerts = driver.findElementsByCssSelector(".title.h3.font-weight-600.text-center");
		if (alerts.size() > 1) {
			final WebElement expectedAlert = alerts.get(1);
			if (expectedAlert.isDisplayed()) {
				throw new ScrapingException("Service is temporarily down.");
			}
		}
	}

	@Override
	public SignalProviderName getName() {
		return SignalProviderName.WETALKTRADE;
	}

	@Override
	protected List<Signal> getSignalsImpl() {
		final List<Signal> result = new ArrayList<Signal>();

		final int retries = 3;
		for (int i = 0; i < retries; i++) {
			try {
				login(driver);
				break;
			} catch (final ScrapingException e) {
				logger.warn("Attempt #" + (i + 1) + " to login failed. Trying again...");
				resetDriver();
			}
		}
		logger.debug("Succesfully logged in WeTalkTrade.");

		checkIfServiceIsDown();

		// paginate_button next/previous disabled TODO
		final WebElement element = driver.findElementByTagName("tbody");

		final List<WebElement> signals = element.findElements(By.tagName("tr"));
		signals.forEach(signal -> {
			final List<WebElement> cells = signal.findElements(By.tagName("td"));
			if (cells.size() == 1) {
				return;
			}
			final Signal newSignal = new Signal(getName());
			final DateTime startDate = parseStringToDateTime(cells.get(1).getText());
			newSignal.setStartDate(startDate);
			final Instrument instrument = Instrument.parse(cells.get(2).getText());
			newSignal.setInstrument(instrument);
			final Side side = Side.valueOf(cells.get(3).getText());
			newSignal.setSide(side);
			newSignal.setEntryPrice(new BigDecimal(cells.get(6).getText()));
			newSignal.setTakeProfit(new BigDecimal(cells.get(7).getText()));
			newSignal.setStopLoss(new BigDecimal(cells.get(8).getText()));

			result.add(newSignal);
		});

		return result;
	}

	private void login(final RemoteWebDriver driver) {
		driver.get(getRootUrl() + "/login");
		final WebElement username = new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
		username.click();
		username.sendKeys(getUsername());
		driver.findElementById("pass").click();
		driver.findElementById("pass").sendKeys(getPassword());
		driver.findElementById("LoginSubmit").click();

		try {
			ScrapingUtils.sleep(1);
			driver.findElementByClassName("md-confirm-button").click();
			ScrapingUtils.sleep(1);
		} catch (final NotFoundException e) {
			// it's ok, the popup didn't appear
		}

		try {
			final WebDriverWait wait = new WebDriverWait(driver, 30);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("tbody")));
		} catch (final TimeoutException e) {
			throw new ScrapingException("Could not sign in to WeTalkTrade. No signal table body found.");
		}

		if (!driver.getPageSource().contains("CURRENTLY RUNNING")) {
			throw new ScrapingException("Could not sign in to WeTalkTrade. See screenshot.");
		}
	}

	private DateTime parseStringToDateTime(final String value) {
		final DateTimeFormatter formatter = DateTimeFormat.forPattern("d-MMM-YYYY HH:mm:ss");
		return formatter.parseDateTime(value);
	}

}
