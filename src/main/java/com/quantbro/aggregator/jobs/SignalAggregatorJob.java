package com.quantbro.aggregator.jobs;

import java.util.Optional;

import org.joda.time.DateTime;

/**
 * Jobs that can be scheduled in our system
 */
public interface SignalAggregatorJob {

	public enum JobStatus {
		INITIALIZED, SUCCESFUL, RUNNING, ERROR, DISABLED;
	}

	/**
	 * how much did the last run of this job last?
	 */
	Long getDurationMillis();

	/**
	 * a unique ID for each job
	 */
	public String getId();

	/**
	 * when did this job run for the last time?
	 */
	DateTime getLastRunDate();

	String getMessage();

	/**
	 * how many milliseconds until the job runs again? empty box if job is disabled
	 */
	Optional<Long> getMillisToNextRun();

	/**
	 * a pretty (readable) name for the job
	 */
	public String getName();

	JobStatus getStatus();

	void initialize();

	boolean isEnabled();

	/**
	 * will schedule the job immediately
	 */
	void scheduleNow();

}
