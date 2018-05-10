package com.quantbro.aggregator.utils;

import static org.junit.Assert.assertEquals;

import org.joda.time.Duration;
import org.junit.Test;

public class StringUtilsUnitTest {

	@Test
	public void parseTimeAgoString() {
		final Duration result = StringUtils.parseTimeAgoToDuration("1 hour 32 minutes ago");
		assertEquals(92, result.getStandardMinutes());

		final Duration result2 = StringUtils.parseTimeAgoToDuration("23 hours 1 minute ago");
		assertEquals(1381, result2.getStandardMinutes());

		final Duration result3 = StringUtils.parseTimeAgoToDuration("9 hours 2 minutes ago");
		assertEquals(542, result3.getStandardMinutes());
	}

	@Test
	public void substringUntilWhitespace() {
		final String result = StringUtils.substringUntilWhitespace("ONE -1 zz ", "-1");
		assertEquals("zz", result);

		final String result2 = StringUtils.substringUntilWhitespace("ONE -1 zz", "-1");
		assertEquals("zz", result2);

		final String result3 = StringUtils.substringUntilWhitespace("TWO 11 -1 sdfsdf -1 zz", "-1");
		assertEquals("sdfsdf", result3);
	}
}
