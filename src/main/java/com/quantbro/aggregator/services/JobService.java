package com.quantbro.aggregator.services;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.configuration.AdapterFactory;
import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.jobs.ScrapingJob;
import com.quantbro.aggregator.jobs.TradeSynchronizationJob;

/**
 * Handles all the jobs in our webapp e.g. the ones that scrape signal providers, the ones that sync the trades etc
 */
@Component
public final class JobService {

	public static final String PHANTOMJS_DRIVER_LOCATION_PROPERTY = "sa.phantomjs.driver.location";
	public static final String CHROME_DRIVER_LOCATION_PROPERTY = "sa.chrome.driver.location";

	private static final Logger logger = LoggerFactory.getLogger(JobService.class);

	@Autowired
	private AdapterFactory adapterInitializer;

	@Autowired
	private TaskScheduler scheduler;

	@Autowired
	private TradeSynchronizationService tradeSynchronizer;

	@Autowired
	private SignalService signalService;

	@Autowired
	private AggregationService aggregationService;

	@Autowired
	private Emailer emailer;

	@Value("${phantomJsDriver.location}")
	private String phantomJsDriverLocation;

	@Value("${chromeDriver.location}")
	private String chromeDriverLocation;

	private final Collection<ScrapingJob> scrapingJobs;
	private TradeSynchronizationJob tradeSynchronizationJob;

	public JobService() {
		scrapingJobs = Sets.newHashSet();
	}

	@EventListener
	public void doOnApplicationStart(final ApplicationReadyEvent event) {
		setWebDriverProperties();
		initializeScrapingJobs();
		initializeSynchronizationJob();
	}

	public Collection<ScrapingJob> getScrapingJobs() {
		return scrapingJobs;
	}

	public TradeSynchronizationJob getTradeSynchronizationJob() {
		return tradeSynchronizationJob;
	}

	protected void initializeScrapingJobs() {
		adapterInitializer.initializeAndGetAdapters().stream().forEach(adapter -> {
			final ScrapingJob scrapingJob = new ScrapingJob(scheduler, signalService, aggregationService, emailer, adapter);
			if (adapter.isEnabled()) {
				scrapingJob.initialize();
			}
			scrapingJobs.add(scrapingJob);
		});
	}

	protected void initializeSynchronizationJob() {
		tradeSynchronizationJob = new TradeSynchronizationJob(scheduler, tradeSynchronizer, emailer);
		tradeSynchronizationJob.initialize();
	}

	/**
	 * will schedule the scraping job for the given provider now, if the provider is enabled
	 *
	 * @throws IllegalArgumentException
	 *             if the provider is disabled
	 */
	public void scheduleScrapingJobForProviderNow(final SignalProviderName providerName) throws IllegalArgumentException {
		final List<ScrapingJob> matchingJobs = scrapingJobs.stream().filter(job -> job.getName() == providerName.toString()).collect(Collectors.toList());
		matchingJobs.stream().findFirst().ifPresent(job -> {
			if (job.isEnabled()) {
				job.scheduleNow();
			} else {
				final String message = "Tried to start disabled job for adapter \"" + job.getName() + "\"";
				logger.error(message);
				throw new IllegalArgumentException(message);
			}
		});

	}

	public void scheduleSynchronizationJobNow() {
		tradeSynchronizationJob.scheduleNow();
	}

	private void setWebDriverProperties() {
		boolean foundDriver = false;
		if (StringUtils.isNotBlank(phantomJsDriverLocation)) {
			System.setProperty(PHANTOMJS_DRIVER_LOCATION_PROPERTY, phantomJsDriverLocation);
			foundDriver = true;
		} else if (StringUtils.isNotBlank(chromeDriverLocation)) {
			System.setProperty(CHROME_DRIVER_LOCATION_PROPERTY, chromeDriverLocation);
			foundDriver = true;
		}

		if (!foundDriver) {
			throw new RuntimeException("No webdriver binary location set, cannot scrape.");
		}
	}

}
