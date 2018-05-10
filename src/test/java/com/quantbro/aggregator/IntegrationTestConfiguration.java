package com.quantbro.aggregator;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.trading.ForexClient;

@Configuration
@Profile("test")
public class IntegrationTestConfiguration {

	@Bean
	public Emailer getEmailer() {
		return Mockito.mock(Emailer.class);
	}

	@Bean
	public ForexClient getForexClient() {
		return Mockito.mock(ForexClient.class);
	}

}
