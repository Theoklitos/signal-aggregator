package com.quantbro.aggregator.adapters.fpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.quantbro.aggregator.adapters.AbstractSignalProviderAdapter;
import com.quantbro.aggregator.adapters.ScrapingException;
import com.quantbro.aggregator.adapters.SignalProviderAdapter;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.services.JobService;

//@Component
public class ErioAdapter extends AbstractSignalProviderAdapter implements SignalProviderAdapter {

	private final static Logger logger = LoggerFactory.getLogger(ErioAdapter.class);

	public static void main(final String args[]) {
		System.setProperty(JobService.PHANTOMJS_DRIVER_LOCATION_PROPERTY, "C:\\phantomjs.exe");
		System.setProperty(JobService.CHROME_DRIVER_LOCATION_PROPERTY, "C:\\chromedriver.exe");
		final ErioAdapter adapter = new ErioAdapter();
		adapter.setConfiguration("http://www.forexpeacearmy.com/forex-strategies/590/erio_s_earner", "", "");
		// adapter.setConfiguration("http://www.forexpeacearmy.com", "", "");
		System.out.println(Joiner.on("\n").join(adapter.getSignals()));
	}

	@Override
	public SignalProviderName getName() {
		// return SignalProviderName.ERIO;
		return null; // implement when ready
	}

	@Override
	protected List<Signal> getSignalsImpl() throws ScrapingException {
		final List<Signal> result = new ArrayList<Signal>();

		driver.get(getRootUrl());

		final WebElement signalTable = driver.findElementById("robot-transaction-open");
		final List<WebElement> signalRows = signalTable.findElements(By.tagName("tr"));

		// if (signalRows.size() == 1) {
		// return result;
		// }

		signalRows.stream().filter(row -> row.findElements(By.tagName("td")).size() >= 15).forEach(signalRow -> {
			final List<WebElement> columns = signalRow.findElements(By.tagName("td"));
			for (int i = 0; i < columns.size(); i++) {
				System.out.println("#" + i + ": " + columns.get(i).getText());
			}

			System.out.println(columns.size());
			if (columns.size() != 0) {
				try {

					final Instrument instrument = Instrument.parse(columns.get(0).getText());
					final Side side = Side.valueOf(columns.get(1).getText());
					final BigDecimal stopLoss = new BigDecimal(columns.get(5).getText());
					final BigDecimal takeProfit = new BigDecimal(columns.get(6).getText());
					final Signal newSignal = new Signal(getName(), instrument, side, stopLoss, takeProfit, Optional.empty());
					result.add(newSignal);
				} catch (final IllegalArgumentException e) { // unknown currency
					logger.warn("Unknown currency \"" + columns.get(0).getText() + "\", will not parse.");
				}
			}
		});

		return result;
	}

}
