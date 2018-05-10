package com.quantbro.aggregator.jobs;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import com.google.common.base.Stopwatch;
import com.quantbro.aggregator.jobs.SignalAggregatorJob.JobStatus;
import com.quantbro.aggregator.utils.StringUtils;

public class AbstractJob {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AbstractJob.class);

	private final TaskScheduler scheduler;
	private final AtomicBoolean firstRun;
	private final Stopwatch durationTimer;
	private DateTime lastRunDate;
	private JobStatus status;
	private ScheduledFuture<?> future;
	private String message;

	public AbstractJob(final TaskScheduler scheduler) {
		this.scheduler = scheduler;
		firstRun = new AtomicBoolean(true);
		durationTimer = Stopwatch.createUnstarted();
		this.status = JobStatus.DISABLED;
	}

	protected void endTimer() {
		durationTimer.stop();
	}

	public Long getDurationMillis() {
		return durationTimer.elapsed(TimeUnit.MILLISECONDS);
	}

	protected Stopwatch getDurationTimer() {
		return durationTimer;
	}

	public AtomicBoolean getFirstRun() {
		return firstRun;
	}

	public DateTime getLastRunDate() {
		return lastRunDate;
	}

	public String getMessage() {
		return message;
	}

	public Optional<Long> getMillisToNextRun() {
		if (future == null || status.equals(JobStatus.DISABLED)) {
			return Optional.empty();
		} else {
			return Optional.of(future.getDelay(TimeUnit.MILLISECONDS));
		}
	}

	public String getReadableTimeToNextRun() {
		if (future == null || status.equals(JobStatus.DISABLED)) {
			return "Never";
		} else {
			return StringUtils.getReadableDuration(future.getDelay(TimeUnit.MILLISECONDS));
		}
	}

	public TaskScheduler getScheduler() {
		return scheduler;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setFuture(final ScheduledFuture<?> future) {
		this.future = future;
	}

	public void setLastRunDate(final DateTime lastRunDate) {
		this.lastRunDate = lastRunDate;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public void setStatus(final JobStatus status) {
		this.status = status;
	}

	protected void startTimer() {
		durationTimer.reset();
		durationTimer.start();
	}

}
