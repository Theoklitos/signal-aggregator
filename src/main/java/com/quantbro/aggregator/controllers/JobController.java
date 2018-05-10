package com.quantbro.aggregator.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.jobs.TradeSynchronizationJob;
import com.quantbro.aggregator.services.JobService;

@Controller
public class JobController {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(JobController.class);

	@Autowired
	private JobService jobManager;

	@RequestMapping(method = RequestMethod.GET, value = "/jobs")
	public String jobInformationPage(final Model model) {
		model.addAttribute("scrapingJobs", jobManager.getScrapingJobs());
		model.addAttribute("tradeSynchronizationJob", jobManager.getTradeSynchronizationJob());
		return "jobs";
	}

	@RequestMapping(method = RequestMethod.POST, value = "/jobs")
	public String startJob(final Model model, @RequestParam(value = "jobId", required = true) final String jobId) {
		if (jobId.equals(TradeSynchronizationJob.JOB_ID)) {
			jobManager.scheduleSynchronizationJobNow();
		} else {
			final SignalProviderName providerName = SignalProviderName.valueOf(jobId);
			jobManager.scheduleScrapingJobForProviderNow(providerName);
		}
		model.addAttribute("scrapingJobs", jobManager.getScrapingJobs());
		model.addAttribute("tradeSynchronizationJob", jobManager.getTradeSynchronizationJob());
		return "redirect:/jobs";
	}

}