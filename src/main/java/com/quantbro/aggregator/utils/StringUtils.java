package com.quantbro.aggregator.utils;

import java.text.DecimalFormat;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public final class StringUtils {

	public final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd MMM HH:mm:ss");

	public static String getReadableDateTime(final DateTime dateTime) {
		if (dateTime == null) {
			return "";
		}
		return DATE_TIME_FORMATTER.print(dateTime);
	}

	public static String getReadableDuration(final Duration duration) {
		if (duration == null) {
			return null;
		}

		PeriodFormatter formatter = null;
		if (duration.isLongerThan(new Duration(10000))) {
			formatter = new PeriodFormatterBuilder().appendDays().appendSuffix("d").appendHours().appendSuffix("h").appendMinutes().appendSuffix("m")
					.appendSeconds().appendSuffix("s").toFormatter();
		} else if (duration.isLongerThan(new Duration(1000))) {
			formatter = new PeriodFormatterBuilder().appendSecondsWithMillis().appendSuffix("s").toFormatter();
		} else {
			formatter = new PeriodFormatterBuilder().appendMillis().appendSuffix("ms").toFormatter();
		}

		final String formatted = formatter.print(duration.toPeriod());
		return formatted;
	}

	public static String getReadableDuration(final long durationInMillis) {
		return getReadableDuration(new Duration(durationInMillis));
	}

	public static boolean isBlank(final String string) {
		return org.apache.commons.lang3.StringUtils.isBlank(string);
	}

	public static boolean isNotBlank(final String string) {
		return org.apache.commons.lang3.StringUtils.isNotBlank(string);
	}

	/**
	 * parses strings like "3 hours, 26 minuts ago" to a {@link Duration}
	 */
	public static Duration parseTimeAgoToDuration(final String value) {
		int hoursAgo = 0;
		int minutesAgo = 0;
		if (value.contains("hour")) {
			hoursAgo = Integer.valueOf(value.substring(0, 2).trim());
		}
		int minutesSubstringStart = 0;
		if (hoursAgo > 0) {
			minutesSubstringStart = 8;
		}
		if (hoursAgo == 1) {
			minutesSubstringStart--;
		}
		minutesAgo = Integer.valueOf(value.substring(minutesSubstringStart, minutesSubstringStart + 2).trim());
		return new Duration(((hoursAgo * 60 * 60) + (minutesAgo * 60)) * 1000);
	}

	/**
	 * returns the given string with the last character (if any) removed
	 */
	public static String removeLastCharacter(String string) {
		if (string != null && string.length() > 0) {
			string = string.substring(0, string.length() - 1);
		}
		return string;
	}

	public static String roundDoubleToPlaces(final double value, final int places) {
		final StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < places; i++) {
			buffer.append("#");
		}
		final DecimalFormat df = new DecimalFormat("###." + buffer.toString());
		return df.format(value);
	}

	/**
	 * returns a substring that starts AFTER the given string and ends in the first whitespace (or end of string) after that
	 *
	 * @returns null if the starting string is not found
	 */
	public static String substringUntilWhitespace(final String string, final String startAfter) {
		final int indexToStart = string.indexOf(startAfter);
		if (indexToStart == -1) {
			return null;
		}
		final String substring = string.substring(indexToStart + startAfter.length()).trim();
		return org.apache.commons.lang3.StringUtils.substringBefore(substring, " ");
	}
}
