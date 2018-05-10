package com.quantbro.aggregator.services;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.quantbro.aggregator.dao.AggregationRepository;
import com.quantbro.aggregator.dao.SignalRepository;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Aggregation.AggregationStatus;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalProviderRanking;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.email.Emailer;

@Service
public class AggregationService {

	private final static Logger logger = LoggerFactory.getLogger(AggregationService.class);

	@Autowired
	private SignalRepository signalRepository;

	@Autowired
	private RankingService rankingService;

	@Autowired
	private AggregationRepository aggregationRepository;

	@Autowired
	private Emailer emailer;

	@Transactional(readOnly = true)
	public List<Aggregation> getAllAggregations() {
		return Lists.newArrayList(aggregationRepository.findAll());
	}

	@Transactional(readOnly = true)
	public List<Aggregation> getLiveAggregations() {
		return aggregationRepository.findByStatus(AggregationStatus.LIVE);
	}

	/**
	 * checks for new aggros, closes the ones that dont show up anymore, saves new ones
	 */
	@Transactional
	public void updateAggregations() {
		final List<Signal> openAndStaleSignals = Stream
				.concat(signalRepository.findByStatus(SignalStatus.LIVE).stream(), signalRepository.findByStatus(SignalStatus.STALE).stream())
				.collect(Collectors.toList());
		final List<Aggregation> liveAggregations = aggregationRepository.findByStatus(AggregationStatus.LIVE);

		// we first do a double group by to group the signals by instrument and side. then we flatten it.
		final List<Aggregation> aggregationsForLatestSignals = Lists.newArrayList();
		final Map<Instrument, Map<Side, List<Signal>>> aggregations = openAndStaleSignals.stream()
				.collect(Collectors.groupingBy(Signal::getInstrument, Collectors.groupingBy(Signal::getSide)));
		aggregations.forEach((instrument, sides) -> {
			for (final Side side : sides.keySet()) {
				final List<Signal> signals = sides.get(side);
				if (signals.size() > 1) {
					final Aggregation aggregation = new Aggregation(instrument, side, signals);
					aggregationsForLatestSignals.add(aggregation);
				}
			}
		});

		// stores which aggros where updated, so that we can close the ones who were not
		final Map<Aggregation, Boolean> whichAggregationsWereUpdatedMap = Maps.newHashMap();
		liveAggregations.forEach(liveAggregation -> {
			whichAggregationsWereUpdatedMap.put(liveAggregation, false);
		});

		// main part: the actual update
		final SignalProviderRanking ranking = rankingService.calculateAndGetRanking();
		aggregationsForLatestSignals.forEach(aggregationForLatestSignals -> {
			final Optional<Aggregation> matchedAggregationOpt = liveAggregations.stream()
					.filter(liveAggregation -> aggregationForLatestSignals.matches(liveAggregation)).findAny();
			if (matchedAggregationOpt.isPresent()) { // a similar aggregation already exists
				final Aggregation matchedLiveAggregation = matchedAggregationOpt.get();
				// store the fact that this aggro will be updated, so that we will not close it
				whichAggregationsWereUpdatedMap.put(matchedLiveAggregation, true);
				// signals were updated (added/removed). save but do not notify
				matchedLiveAggregation.setSignals(aggregationForLatestSignals.getSignals());
			} else { // new aggregation! notify and save
				aggregationForLatestSignals.setRanking(ranking);
				final Aggregation savedAggregation = aggregationRepository.save(aggregationForLatestSignals);
				logger.info("New signal aggregation for " + savedAggregation.getInstrument() + " " + savedAggregation.getSide() + " with "
						+ savedAggregation.getSignals().size() + " signals!");
				emailer.informOfNewAggregation(savedAggregation);
			}
		});

		// now close the ones that were not updated
		final Iterator<Entry<Aggregation, Boolean>> iterator = whichAggregationsWereUpdatedMap.entrySet().iterator();
		iterator.forEachRemaining(entry -> {
			if (!entry.getValue()) {
				final Aggregation aggregationToBeClosed = entry.getKey();
				aggregationToBeClosed.close();
				logger.info("Aggregation #" + aggregationToBeClosed.getId() + " (" + aggregationToBeClosed.getInstrument() + " "
						+ aggregationToBeClosed.getSide() + ") was not detected anymore and so was closed.");
			}
		});

	}
}
