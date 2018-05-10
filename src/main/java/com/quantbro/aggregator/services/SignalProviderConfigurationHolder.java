package com.quantbro.aggregator.services;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.repository.cdi.Eager;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.quantbro.aggregator.adapters.AbstractSignalProviderAdapter;
import com.quantbro.aggregator.adapters.AdapterInitializationException;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.configuration.SignalProviderConfiguration;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.scraping.ScrapingType;

/**
 * A semi-service that initializes and holds configurations about all
 */
@Service
@Eager
public class SignalProviderConfigurationHolder {

	private final static Logger logger = LoggerFactory.getLogger(SignalProviderConfigurationHolder.class);

	@Autowired
	private Environment enviroment;

	private Map<SignalProviderName, SignalProviderConfiguration> configurations;

	@EventListener
	public void doOnApplicationStart(final ContextRefreshedEvent event) {
		this.configurations = Maps.newConcurrentMap();

		// get all the adapter class names
		final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.addIncludeFilter(new AssignableTypeFilter(AbstractSignalProviderAdapter.class));
		final Set<String> adapterClassNames = provider.findCandidateComponents(AbstractSignalProviderAdapter.class.getPackage().getName()).stream()
				.map(bd -> bd.getBeanClassName()).collect(Collectors.toSet());

		// create the config pojos one by one
		adapterClassNames.forEach(adapterClassName -> {
			final String adapterPropertyName = getAdapterPropertyName(adapterClassName);
			final String adapterPropertyPrefix = "providers." + adapterPropertyName;
			try {
				final SignalProviderName name = findSignalProviderNameEnum(adapterPropertyName); // hack!
				final String cronString = enviroment.getProperty(adapterPropertyPrefix + ".cronJob");
				final String rootUrl = enviroment.getProperty(adapterPropertyPrefix + ".rootUrl");
				final String randomIntervalString = enviroment.getProperty(adapterPropertyPrefix + ".randomIntervalPeriod");
				final String accountId = enviroment.getProperty(adapterPropertyPrefix + ".accountId");
				if (StringUtils.isBlank(rootUrl) || ((StringUtils.isBlank(cronString) && StringUtils.isBlank(randomIntervalString)))
						|| StringUtils.isBlank(accountId)) {
					throw new AdapterInitializationException(name);
				}
				Period randomIntervalPeriod = null;
				if (StringUtils.isNotBlank(randomIntervalString)) {
					randomIntervalPeriod = Period.parse(randomIntervalString);
				}
				ScrapingType type = ScrapingType.PHANTOMJS;
				final String typeString = enviroment.getProperty(adapterPropertyPrefix + ".scrapingType");
				if (StringUtils.isBlank(typeString)) {
					logger.warn("No type defined for " + name + ", will default to " + type);
				} else {
					type = ScrapingType.valueOf(typeString.toUpperCase());
				}
				final boolean shouldTradesCloseSignals = getBooleanProperty(adapterPropertyPrefix + ".shouldTradesCloseSignals", false, name);
				final boolean shouldSignalsCloseTrades = getBooleanProperty(adapterPropertyPrefix + ".shouldSignalsCloseTrades", false, name);
				final String username = enviroment.getProperty(adapterPropertyPrefix + ".username");
				final String password = enviroment.getProperty(adapterPropertyPrefix + ".password");

				// get and create the dump folder
				final String dumpFolder = enviroment.getProperty("logging.scrapingDumpFolder");
				final File dumpFolderFile = new File(dumpFolder);
				if (!dumpFolderFile.exists()) {
					if (!dumpFolderFile.mkdir()) {
						throw new RuntimeException("Could not create scraping dump folder " + dumpFolder + "!");
					}
				}
				final File dumpFolderForAdapter = new File(dumpFolder, adapterPropertyName);
				if (!dumpFolderForAdapter.exists()) {
					if (!dumpFolderForAdapter.mkdir()) {
						throw new RuntimeException("Could not create scraping dump folder for adapter " + dumpFolderForAdapter + "!");
					}
				}

				final SignalProviderConfiguration configuration = new SignalProviderConfiguration(cronString, rootUrl, type, shouldTradesCloseSignals,
						shouldSignalsCloseTrades, randomIntervalPeriod, accountId, dumpFolderForAdapter, username, password);
				configurations.put(name, configuration);
				logger.info("Read configuration for adapter " + name + " succesfully.");
			} catch (final AdapterInitializationException e) {
				logger.warn("Adapter for " + adapterPropertyName + " is disabled due to missing properties.");
			} catch (final IllegalArgumentException e) {
				logger.warn("Adapter for " + adapterPropertyName + " had error " + e.getMessage() + " therefore it is disabled.");
			}
		});
	}

	/**
	 * this is a hack that tries to find an enum that looks like the adapter property name. ideally this value should be stored in the enum itself
	 */
	private SignalProviderName findSignalProviderNameEnum(final String adapterPropertyName) {
		final Optional<SignalProviderName> spnOpt = Stream.of(SignalProviderName.values())
				.filter(spn -> spn.toString().toLowerCase().contains(adapterPropertyName.toLowerCase())).findAny();
		if (spnOpt.isPresent()) {
			return spnOpt.get();
		} else {
			throw new IllegalArgumentException("Could not find signal provider enum for " + adapterPropertyName);
		}
	}

	/**
	 * finds the account id for the signal provider
	 *
	 * @throws IllegalArgumentException
	 *             if there is no account id configured for this provider
	 */
	public String findTradingAccountId(final SignalProviderName signalProviderName) {
		final SignalProviderConfiguration configuration = configurations.get(signalProviderName);
		if (configuration == null) {
			throw new IllegalArgumentException("Could not find trading account id for " + signalProviderName);
		}
		return configuration.getAccountId();
	}

	/**
	 * finds the account id for the signal provider of the given trade
	 *
	 * @throws IllegalArgumentException
	 *             if there is no account id configured for this provider
	 */
	public String findTradingAccountId(final Trade trade) {
		return findTradingAccountId(trade.getProviderName());
	}

	private String getAdapterPropertyName(final String adapterClassName) {
		if (StringUtils.isBlank(adapterClassName)) {
			return adapterClassName;
		}
		final String simpleAdapterClassName = StringUtils.substringAfterLast(adapterClassName, ".");
		final String propertyName = simpleAdapterClassName.substring(0, 1).toLowerCase() + simpleAdapterClassName.substring(1);
		return propertyName.substring(0, propertyName.length() - 7);
	}

	private boolean getBooleanProperty(final String propertyName, final boolean defaultValue, final SignalProviderName name) {
		final String stringProperty = enviroment.getProperty(propertyName, String.valueOf(defaultValue));
		final String stringPropertyFormatted = stringProperty.trim().toLowerCase();
		if (StringUtils.isNotBlank(stringPropertyFormatted) && !stringPropertyFormatted.equals("true") && !stringPropertyFormatted.equals("false")) {
			throw new AdapterInitializationException(name);
		}
		return Boolean.valueOf(stringProperty);
	}

	/**
	 * returns a configuration for the given provider, otherwise null if none exists
	 */
	public SignalProviderConfiguration getForProvider(final SignalProviderName name) {
		return configurations.get(name);
	}

	/**
	 * returns a configuration for the provider of the given trade, otherwise null if none exists
	 */
	public SignalProviderConfiguration getForProvider(final Trade trade) {
		return getForProvider(trade.getProviderName());
	}

	/**
	 * does a config exist for the provider of the given trade?
	 */
	public boolean hasConfigurationForProvider(final Trade trade) {
		return getForProvider(trade.getProviderName()) != null;
	}

}
