package com.quantbro.aggregator.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfiguration {

	@Value("${jobThreads:8}")
	public int jobThreads;

	@Bean
	public TaskScheduler taskScheduler() {
		final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setThreadNamePrefix("sa-job-thread-");
		taskScheduler.setPoolSize(jobThreads);
		return taskScheduler;
	}

}
