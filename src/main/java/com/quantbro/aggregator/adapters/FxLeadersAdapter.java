package com.quantbro.aggregator.adapters;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;

@Component
public class FxLeadersAdapter extends AbstractSignalProviderAdapter implements SignalProviderAdapter {

	private final static Logger logger = LoggerFactory.getLogger(FxLeadersAdapter.class);

	@Override
	public SignalProviderName getName() {
		return SignalProviderName.FXLEADERS;
	}

	@Override
	protected List<Signal> getSignalsImpl() throws ScrapingException {
		final List<Signal> result = new ArrayList<Signal>();
		try {
			final Document document = Jsoup.connect(getRootUrl()).get();
			final Element signalTable = document.getElementById("gvSignalsShortNG");
			final Elements signalRows = signalTable.getElementsByTag("tr");
			if (signalRows.size() == 1) {
				return result;
			}

			signalRows.stream().filter(signalRow -> {
				return (signalRow.toString().contains("<td>Active</td>")) && (signalRow.getElementsByClass("btnTrNow").size() == 1);
			}).forEach(signalRow -> {
				final Elements columns = signalRow.getElementsByTag("td");
				if (columns.size() != 0) {
					try {
						final Instrument instrument = Instrument.parse(columns.get(0).text());
						final Side side = Side.valueOf(columns.get(1).text());
						final BigDecimal stopLoss = new BigDecimal(columns.get(5).text());
						final BigDecimal takeProfit = new BigDecimal(columns.get(6).text());
						final Signal newSignal = new Signal(getName(), instrument, side, stopLoss, takeProfit, Optional.empty());
						result.add(newSignal);
					} catch (final IllegalArgumentException e) { // unknown currency
						logger.warn("Unknown currency \"" + columns.get(0).text() + "\", will not parse.");
					}
				}
			});
		} catch (final IOException e) {
			throw new ScrapingException(e);
		}

		return result;
	}

}
