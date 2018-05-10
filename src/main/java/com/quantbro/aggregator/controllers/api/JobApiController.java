package com.quantbro.aggregator.controllers.api;

import java.util.Collection;

import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.JobData;
import com.quantbro.aggregator.jobs.SignalAggregatorJob.JobStatus;

@RestController
@RequestMapping("/api/jobs")
public class JobApiController {

	// @Autowired
	// private JobService jobManager;

	@RequestMapping(method = RequestMethod.GET)
	public Collection<JobData> getAllJobs() {
		return Lists.newArrayList(new JobData(SignalProviderName.FORESIGNAL, JobStatus.SUCCESFUL, "message!", new DateTime(), new DateTime().plusDays(1)));
	}

}
