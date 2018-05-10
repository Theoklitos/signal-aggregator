package com.quantbro.aggregator.domain;

import org.joda.time.DateTime;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.jobs.SignalAggregatorJob.JobStatus;

/**
 * information about a scraping job for a specific provider
 */
public class JobData {

	private SignalProviderName name;
	private JobStatus status;
	private String message;
	// when was this job run?
	private DateTime executionDate;
	// when is this job going to run again?
	private DateTime nextExecutionDate;

	public JobData(final SignalProviderName name, final JobStatus status, final String message, final DateTime executionDate,
			final DateTime nextExecutionDate) {
		this.name = name;
		this.status = status;
		this.message = message;
		this.executionDate = executionDate;
		this.nextExecutionDate = nextExecutionDate;
	}

	public DateTime getExecutionDate() {
		return executionDate;
	}

	public String getMessage() {
		return message;
	}

	public SignalProviderName getName() {
		return name;
	}

	public DateTime getNextExecutionDate() {
		return nextExecutionDate;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setExecutionDate(final DateTime executionDate) {
		this.executionDate = executionDate;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public void setName(final SignalProviderName name) {
		this.name = name;
	}

	public void setNextExecutionDate(final DateTime nextExecutionDate) {
		this.nextExecutionDate = nextExecutionDate;
	}

	public void setStatus(final JobStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return getName() + " (" + getStatus() + "), message: " + getMessage() + ". Run at: " + getExecutionDate() + ", next at: " + getNextExecutionDate();
	}

}
