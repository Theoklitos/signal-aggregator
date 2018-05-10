package com.quantbro.aggregator.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Test;

import com.quantbro.aggregator.adapters.SignalProviderName;

public final class SignalUnitTest {

	public static final BigDecimal ZERO = BigDecimal.ZERO;

	@Test
	public void negativeMatches() {
		final Signal signal1 = new Signal(SignalProviderName.FXLEADERS, Instrument.NZD_USD, Side.SELL, ZERO, ZERO, Optional.empty());
		final Signal signal2 = new Signal(SignalProviderName.FXLEADERS, Instrument.NZD_USD, Side.BUY, ZERO, ZERO, Optional.empty());
		assertFalse(signal1.matches(signal2));

		final Signal signal3 = new Signal(SignalProviderName.FORESIGNAL, Instrument.USD_JPY, Side.SELL, BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.3),
				Optional.of(ZERO));
		final Signal signal4 = new Signal(SignalProviderName.FORESIGNAL, Instrument.USD_JPY, Side.SELL, BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.9),
				Optional.of(ZERO));
		assertFalse(signal3.matches(signal4));
	}

	@Test
	public void positiveMatches() {
		final Signal signal1 = new Signal(SignalProviderName.FORESIGNAL, Instrument.EUR_USD, Side.SELL, ZERO, BigDecimal.valueOf(1.456), Optional.empty());
		final Signal signal2 = new Signal(SignalProviderName.FORESIGNAL, Instrument.EUR_USD, Side.SELL, ZERO, BigDecimal.valueOf(1.456), Optional.empty());
		assertTrue(signal1.matches(signal2));

		final Signal signal3 = new Signal(SignalProviderName.DUXFOREX, Instrument.AUD_USD, Side.SELL, BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.3),
				Optional.of(BigDecimal.valueOf(1.23)));
		final Signal signal4 = new Signal(SignalProviderName.DUXFOREX, Instrument.AUD_USD, Side.SELL, BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.3),
				Optional.of(BigDecimal.valueOf(1.23)));
		assertTrue(signal3.matches(signal4));
	}
}
