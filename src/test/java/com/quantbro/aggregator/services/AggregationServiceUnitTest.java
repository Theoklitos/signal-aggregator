package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.Fixtures;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.email.Emailer;

@RunWith(SpringRunner.class)
public final class AggregationServiceUnitTest {

	@Mock
	private SignalRepository signalRepository;

	@Mock
	private Emailer emailer;

	@InjectMocks
	private AggregationService aggregationService;

	@Test
	public void checkForAndFindAggregationAndThenNotify() {
		when(signalRepository.findByStatus(SignalStatus.STALE)).thenReturn(Lists.newArrayList());
		final List<Signal> openSignals = Lists.newArrayList();
		openSignals.add(Fixtures.createSignalForProviderAndInstrument(SignalProviderName.FORESIGNAL, Instrument.EUR_USD));
		openSignals.add(Fixtures.createSignalForProviderAndInstrument(SignalProviderName.FXLEADERS, Instrument.EUR_USD));
		openSignals.add(Fixtures.createSignalForProviderAndInstrument(SignalProviderName.FORESIGNAL, Instrument.EUR_CAD));
		when(signalRepository.findByStatus(SignalStatus.LIVE)).thenReturn(openSignals);

		aggregationService.updateAggregations();
		aggregationService.updateAggregations();

		// only the first call should send an email
		verify(emailer, times(1)).informOfNewAggregation(any(Aggregation.class));
		assertEquals(1, aggregationService.getLiveAggregations().size());
	}

}
