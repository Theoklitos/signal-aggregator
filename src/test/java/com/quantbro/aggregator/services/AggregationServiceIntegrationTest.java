package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.Fixtures;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AggregationServiceIntegrationTest {

	@Autowired
	private SignalRepository signalRepository;

	@Autowired
	private AggregationService aggregationService;

	@Test
	public void detectAndSaveNewAggregation() {
		final Signal signal1 = Fixtures.createSignal(SignalProviderName.FORESIGNAL, SignalStatus.LIVE, Instrument.EUR_AUD, Side.SELL);
		final Signal signal2 = Fixtures.createSignal(SignalProviderName.DUXFOREX, SignalStatus.LIVE, Instrument.EUR_AUD, Side.SELL);
		final Signal signal3 = Fixtures.createSignal(SignalProviderName.FXLEADERS, SignalStatus.LIVE, Instrument.EUR_AUD, Side.SELL);
		final Signal irrSignal1 = Fixtures.createSignal(SignalProviderName.FORESIGNAL, SignalStatus.LIVE, Instrument.EUR_AUD, Side.BUY);
		final Signal irrSignal2 = Fixtures.createSignal(SignalProviderName.WETALKTRADE, SignalStatus.LIVE, Instrument.AUD_HKD, Side.BUY);
		signalRepository.save(Lists.newArrayList(signal1, signal2, signal3, irrSignal1, irrSignal2));

		// we detect the first aggregation. it should be saved.
		aggregationService.updateAggregations();
		// aggregationService.updateAggregations();
		final List<Aggregation> liveAggregations = aggregationService.getLiveAggregations();
		assertEquals(1, liveAggregations.size());
		assertEquals(Instrument.EUR_AUD, liveAggregations.get(0).getInstrument());

		// now the signals are closed and the aggregation should be closed too
		setSignalStatusAndSave(1, SignalStatus.CLOSED);
		setSignalStatusAndSave(2, SignalStatus.CLOSED);
		aggregationService.updateAggregations();
		assertEquals(0, aggregationService.getLiveAggregations().size());

		// we create a new signal to create a second (and similar) aggregation
		final Signal lastSignal = Fixtures.createSignal(SignalProviderName.WETALKTRADE, SignalStatus.LIVE, Instrument.EUR_AUD, Side.SELL);
		signalRepository.save(lastSignal);
		// assert the second aggregation
		aggregationService.updateAggregations();
		assertEquals(1, liveAggregations.size());
		assertEquals(Instrument.EUR_AUD, aggregationService.getLiveAggregations().get(0).getInstrument());
	}

	private void setSignalStatusAndSave(final int signalId, final SignalStatus status) {
		final Signal signal = signalRepository.findOne(Long.valueOf(signalId));
		signal.setStatus(status);
		signalRepository.save(signal);
	}

}
