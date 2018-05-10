package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.Fixtures;
import com.quantbro.aggregator.IntegrationTestConfiguration;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.email.Emailer;

/**
 * A full integration test is not really needed, this could be done as unit tests. But I wanted to figure out how this works.
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = { IntegrationTestConfiguration.class })
@TestPropertySource(locations = "classpath:application-test.properties")
public final class SignalServiceIntegrationTest {

	@Autowired
	private SignalService service;

	@Autowired
	private Emailer emailer;

	@Autowired
	private SignalRepository repository;

	@Test(expected = SignalUpdateException.class)
	public void adapterToUpdateAndSignalsDontMatch() {
		final List<Signal> signals = Fixtures.createSignalsForProvider(3, SignalProviderName.FORESIGNAL);
		service.update(SignalProviderName.FXLEADERS, signals, true, true);
	}

	@Test
	public void liveSignalsAreClosedWhenTheyAreNotInTheNewSignalList() {
		final Signal openSignal1 = Fixtures.createSignalForProviderWithStatus(SignalProviderName.FORESIGNAL, SignalStatus.LIVE);
		repository.save(openSignal1);
		final Signal openSignal2 = Fixtures.createSignalForProviderWithStatus(SignalProviderName.FXLEADERS, SignalStatus.LIVE);
		repository.save(openSignal2);

		service.update(SignalProviderName.FORESIGNAL, Lists.newArrayList(), false, false);

		assertEquals(1, repository.findByProviderNameAndStatus(SignalProviderName.FORESIGNAL, SignalStatus.CLOSED).size());
		assertEquals(1, repository.findByProviderNameAndStatus(SignalProviderName.FXLEADERS, SignalStatus.LIVE).size());
	}

	@Test
	public void newSignalTriggersEmailAndGhostTrade() {
		assertEquals(0, repository.count());
		service.update(SignalProviderName.FXLEADERS, Fixtures.createSignalsForProvider(1, SignalProviderName.FXLEADERS), false, false);
		verify(emailer, times(1)).sendEmail(anyString(), anyString());
		assertEquals(1, repository.count());
	}

	@Before
	public void setUp() {
		repository.deleteAll();
	}

	@Test
	public void whenSignalsAlreadyExistThenNothingHappens() {
		final Signal openSignal = Fixtures.createSignalForProviderWithStatus(SignalProviderName.FORESIGNAL, SignalStatus.LIVE);
		repository.save(openSignal);

		service.update(SignalProviderName.FORESIGNAL, Lists.newArrayList(openSignal), false, false);

		verify(emailer, never()).sendEmail(anyString(), anyString());
		assertEquals(1, repository.findByProviderNameAndStatus(SignalProviderName.FORESIGNAL, SignalStatus.LIVE).size());
	}

}
