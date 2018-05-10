package com.quantbro.aggregator.controllers;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.services.JobService;
import com.quantbro.aggregator.utils.StringUtils;

@Controller
public class HomeController {

	@Autowired
	private JobService jobService;

	@Autowired
	private SignalRepository signalRepository;

	@Autowired
	private TradeRepository tradeRepository;

	private String getUptimeAsReadableString() {
		final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		final Duration uptime = new Duration(rb.getUptime());
		return StringUtils.getReadableDuration(uptime);
	}

	@RequestMapping("/")
	public String mainPage(final Model model) {
		final String greetingMessage = "Up and running for " + getUptimeAsReadableString() + ". Tracking "
				+ signalRepository.findByStatus(SignalStatus.LIVE).size() + " live signal(s) and " + tradeRepository.findByStatus(TradeStatus.OPENED).size()
				+ " ghost trade(s).";
		model.addAttribute("greetingMessage", greetingMessage);
		final String jobStatusMessage = "Currently scraping " + jobService.getScrapingJobs().stream().filter(job -> job.isEnabled()).count() + " adapter(s).";
		model.addAttribute("jobStatusMessage", jobStatusMessage);

		return "index";
	}

}