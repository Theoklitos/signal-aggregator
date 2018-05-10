package com.quantbro.aggregator.jobs;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.google.common.base.Joiner;
import com.quantbro.aggregator.controllers.TradeSynchronizationException;
import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.services.UpdateOperationResult;
import com.quantbro.aggregator.services.TradeSynchronizationService;
import com.quantbro.aggregator.utils.StringUtils;

public class TradeSynchronizationJob extends AbstractJob implements SignalAggregatorJob {

	public static final String JOB_NAME = "Trade Synchronization Job";
	public static final String JOB_ID = "tsj";

	private static final Logger logger = LoggerFactory.getLogger(TradeSynchronizationJob.class);

	private final TradeSynchronizationService tradeSynchronizer;
	private final Emailer emailer;

	public TradeSynchronizationJob(final TaskScheduler scheduler, final TradeSynchronizationService tradeSynchronizer, final Emailer emailer) {
		super(scheduler);
		this.tradeSynchronizer = tradeSynchronizer;
		this.emailer = emailer;
	}

	@Override
	public String getId() {
		return JOB_ID;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	public void initialize() {
		setStatus(JobStatus.INITIALIZED);

		synchronizeTrades();

		final String cronJob = tradeSynchronizer.getCronJob();
		if (StringUtils.isBlank(cronJob)) {
			throw new RuntimeException("No cron job for trade synchronization job found, cannot schedule.");
		}
		final ScheduledFuture<?> future = getScheduler().schedule(new Runnable() {

			@Override
			public void run() {
				synchronizeTrades();
			}

		}, new CronTrigger(cronJob));
		setFuture(future);
		logger.info("Trade synchronization job scheduled based on cron job \"" + cronJob + "\". Next run in: " + getReadableTimeToNextRun());
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void scheduleNow() {
		getScheduler().schedule(new Runnable() {

			@Override
			public void run() {
				synchronizeTrades();
			}
		}, new Date());
	}

	/**
	 * main method
	 */
	private synchronized void synchronizeTrades() {
		startTimer();
		logger.info("Starting trade synchronization job.");
		try {
			final UpdateOperationResult result = tradeSynchronizer.synchronizeTrades();
			setStatus(JobStatus.SUCCESFUL);
			setMessage(result.toString());
		} catch (final TradeSynchronizationException e) {
			e.printStackTrace();
			setStatus(JobStatus.ERROR);
			final String message = (e.getTradeUpdateExceptions().size() > 0) ? Joiner.on(", ").join(e.getTradeUpdateExceptions()) : e.getLocalizedMessage();
			setMessage(message);
		} catch (final Throwable t) { // unexpected error! escalate
			t.printStackTrace();
			setStatus(JobStatus.ERROR);
			setMessage(t.getLocalizedMessage());
			emailer.escalate("Unexpected error when sync'ing trades", "", Optional.of(t));
		} finally {
			endTimer();
			setLastRunDate(new DateTime());
		}

		logger.info("Trade synchronization job complete. Took " + getDurationTimer().toString());
	}

}
