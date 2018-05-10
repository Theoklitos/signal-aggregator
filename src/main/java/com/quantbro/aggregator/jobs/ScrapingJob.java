package com.quantbro.aggregator.jobs;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.quantbro.aggregator.adapters.ScrapingException;
import com.quantbro.aggregator.adapters.SignalProviderAdapter;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.services.AggregationService;
import com.quantbro.aggregator.services.SignalService;
import com.quantbro.aggregator.services.UpdateOperationResult;
import com.quantbro.aggregator.utils.StringUtils;

public class ScrapingJob extends AbstractJob implements SignalAggregatorJob {

	private static final Logger logger = LoggerFactory.getLogger(ScrapingJob.class);

	private boolean isRandomlyTimed;

	private final SignalService signalService;
	private final AggregationService aggregationService;
	private final SignalProviderAdapter adapter;
	private final Emailer emailer;

	public ScrapingJob(final TaskScheduler scheduler, final SignalService signalService, final AggregationService service, final Emailer emailer,
			final SignalProviderAdapter adapter) {
		super(scheduler);
		this.signalService = signalService;
		aggregationService = service;
		this.emailer = emailer;
		this.adapter = adapter;
	}

	@Override
	public String getId() {
		return adapter.getName().name();
	}

	@Override
	public String getName() {
		return adapter.getName().toString();
	}

	@Override
	public void initialize() {
		setStatus(JobStatus.INITIALIZED);

		scheduleNow();

		final String cronString = adapter.getCronString();
		isRandomlyTimed = (StringUtils.isNotBlank(cronString)) ? false : true;
		if (!isRandomlyTimed) { // we configure via cron job
			final ScheduledFuture<?> future = getScheduler().schedule(new Runnable() {
				@Override
				public void run() {
					scrapeAndUpdate();
				}
			}, new CronTrigger(cronString));
			setFuture(future);
			logger.info("Scheduled adapter " + adapter.getName() + " based on cron job \"" + cronString + "\". Next run in: " + getReadableTimeToNextRun());
		} else { // configure random time scheduling
			getScheduler().schedule(new Runnable() {

				@Override
				public void run() {
					scrapeAndScheduleRandomly();
				}
			}, new Date());
		}
	}

	/**
	 * is the sp adapter (and by consequence) this job enabled?
	 */
	@Override
	public boolean isEnabled() {
		return adapter.isEnabled();
	}

	/**
	 * Will run the job immediately
	 */
	@Override
	public void scheduleNow() {
		getScheduler().schedule(new Runnable() {

			@Override
			public void run() {
				scrapeAndUpdate();
			}
		}, new Date());
	}

	private void scrapeAndScheduleRandomly() {
		final DateTime now = new DateTime();
		final Period randomIntervalPeriod = adapter.getRandomIntervalPeriod();
		final long millisToChooseFrom = (randomIntervalPeriod.toStandardSeconds().getSeconds() * 1000) * 2;
		final long randomMomentInPeriod = RandomUtils.nextLong(1, millisToChooseFrom);
		final DateTime dateOfNextScrape = now.plus(randomMomentInPeriod);

		final ScheduledFuture<?> future = getScheduler().schedule(new Runnable() {

			@Override
			public void run() {
				scrapeAndUpdate();
				scrapeAndScheduleRandomly();
			}
		}, dateOfNextScrape.toDate());
		setFuture(future);

		logger.info("Randomly scheduled adapter " + adapter.getName() + " to run in " + getReadableTimeToNextRun() + ", period is "
				+ adapter.getRandomIntervalPeriod() + ".");
	}

	/**
	 * main signal update method
	 */
	private synchronized void scrapeAndUpdate() {
		startTimer();

		final SignalProviderName adapterName = adapter.getName();
		final AtomicBoolean firstRun = getFirstRun();
		if (firstRun.get()) {
			logger.info("Starting first update job for provider " + adapterName + ", signals will not trigger trades.");
		} else {
			logger.info("Starting update job for provider " + adapterName + ".");
		}

		try {
			final List<Signal> signals = adapter.getSignals();
			logger.info(adapterName + " succesfully scraped " + signals.size() + " signal(s).");
			final UpdateOperationResult result = signalService.update(adapterName, signals, !firstRun.get(), true);
			aggregationService.updateAggregations();
			setStatus(JobStatus.SUCCESFUL);
			// also set a nice message
			final String newSignalsString = (result.getOpened() == 0) ? "" : result.getOpened() + " new signal(s). ";
			final String closedSignalsString = (result.getClosed() == 0) ? "" : result.getClosed() + " closed signal(s).";
			final String message = (newSignalsString.isEmpty() || closedSignalsString.isEmpty()) ? "Nothing new." : newSignalsString + closedSignalsString;
			setMessage(message);
		} catch (final ScrapingException e) {
			// specific scraping-related exception
			emailer.escalate("Failed to scrape " + adapterName, "", e);
			e.printStackTrace();
			setStatus(JobStatus.ERROR);
			setMessage(e.getMessage());
		} catch (final Throwable t) {
			// all other kinds of errors
			emailer.escalate("Unexpected error when scraping " + adapterName, "", Optional.of(t));
			t.printStackTrace();
			setStatus(JobStatus.ERROR);
			setMessage(t.getMessage());
		} finally {
			endTimer();
			setLastRunDate(new DateTime());
		}

		logger.info("Update job for provider " + adapterName + " complete. Took " + StringUtils.getReadableDuration(new Duration(getDurationMillis())));
		if (firstRun.get()) {
			firstRun.set(false);
		}
	}

}
