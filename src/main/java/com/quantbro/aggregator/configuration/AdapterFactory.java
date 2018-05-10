package com.quantbro.aggregator.configuration;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.adapters.AbstractSignalProviderAdapter;
import com.quantbro.aggregator.adapters.AdapterInitializationException;
import com.quantbro.aggregator.adapters.SignalProviderAdapter;
import com.quantbro.aggregator.jobs.ScrapingJob;
import com.quantbro.aggregator.services.SignalProviderConfigurationHolder;
import com.quantbro.aggregator.utils.ReflectionUtils;

/**
 * Initializes and injects dependencies into {@link SignalProviderAdapter}s and their {@link ScrapingJob}s
 */
@Component
public class AdapterFactory {

	private static final Logger logger = LoggerFactory.getLogger(AdapterFactory.class);

	@Autowired
	private SignalProviderConfigurationHolder signalProviderConfigurations;

	@Autowired
	private ApplicationContext context;

	public List<SignalProviderAdapter> initializeAndGetAdapters() {
		final List<String> classNames = ReflectionUtils.getAllClassesInPackage(AbstractSignalProviderAdapter.class.getPackage().getName());
		final List<SignalProviderAdapter> initializedAdapters = classNames.stream().map(className -> {
			final String expectedClassName = StringUtils.uncapitalize(className);
			try {
				final AbstractSignalProviderAdapter adapter = context.getBean(expectedClassName, AbstractSignalProviderAdapter.class);
				final SignalProviderConfiguration configuration = signalProviderConfigurations.getForProvider(adapter.getName());
				adapter.setConfiguration(configuration);
				return adapter;
			} catch (final BeansException e) {
				if (e.getCause() instanceof AdapterInitializationException) {
					final AdapterInitializationException ae = (AdapterInitializationException) e.getCause();
					logger.warn("Missing or invalid properties found for adapter " + ae.getAdapterName() + ". Adapter will not be initialized.");
				}
			}
			return null;
		}).collect(Collectors.toList());

		// remove null values (and log) before returning
		final List<SignalProviderAdapter> initializedAdaptersWithoutNulls = initializedAdapters.stream().filter(adapter -> adapter != null)
				.collect(Collectors.toList());
		logger.info("Finished initializing adapters. Got " + initializedAdaptersWithoutNulls.size() + " active one(s).");
		return initializedAdaptersWithoutNulls;
	}

}
