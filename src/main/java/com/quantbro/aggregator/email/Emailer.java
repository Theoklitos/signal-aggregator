package com.quantbro.aggregator.email;

import java.util.Optional;

import com.quantbro.aggregator.adapters.ScrapingException;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Signal;

public interface Emailer {

	/**
	 * a more urgent email that indicates something was wrong
	 *
	 * @param t
	 *            will include this stacktrace in the email
	 */
	public void escalate(final String title, final String body, final Optional<Throwable> t);

	/**
	 * sends a specific escalation for scraping messages, will try to include a screenshot in the email
	 */
	void escalate(final String title, final String body, ScrapingException e);

	/**
	 * will inform for the given aggregation (signals with the same instrument and same diretion) and can thus be a new opportunity
	 */
	public void informOfNewAggregation(Aggregation aggregation);

	/**
	 * self-explanatory. will include information of this signal's trade
	 */
	public void informOfNewSignalAndItsTrade(final Signal newSignal);

	/**
	 * Sends a simple email
	 */
	public void sendEmail(final String title, final String body);

}
