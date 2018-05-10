package com.quantbro.aggregator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.email.GmailEmailer;
import com.quantbro.aggregator.trading.ForexClient;
import com.quantbro.aggregator.trading.OandaForexClient;

/**
 * Some dependency injection config for the "live" app
 */
@Configuration
@Profile("live")
public class BeanConfiguration {

	@Bean
	public Emailer realEmailer() {
		return new GmailEmailer();
	}

	@Bean
	public ForexClient realForexClient() {
		return new OandaForexClient();
	}

}
