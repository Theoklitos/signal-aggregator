package com.quantbro.aggregator.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.domain.Instrument;

@RunWith(SpringRunner.class)
public final class InstrumentUnitTest {

	@Test(expected = IllegalArgumentException.class)
	public void erroneousCurrencyPairParsing() {
		Instrument.parse("AUD-USD");
	}

	@Test
	public void successfulcurrencyPairParsing() {
		assertEquals(Instrument.AUD_USD, Instrument.parse("AUD/USD"));
		assertEquals(Instrument.EUR_CHF, Instrument.parse("EURCHF"));
	}

}
