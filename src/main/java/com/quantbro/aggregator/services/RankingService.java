package com.quantbro.aggregator.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.SignalProviderRanking;

@Service
public class RankingService {

	private static final int RANKING_VALUES_DECIMAL_SCALE = 2;

	private final static Logger logger = LoggerFactory.getLogger(RankingService.class);

	@Autowired
	private TradeRepository tradeRepository;

	@Transactional(readOnly = true)
	public SignalProviderRanking calculateAndGetRanking() {
		// final Stopwatch stopwatch = Stopwatch.createStarted();

		final Map<SignalProviderName, List<BigDecimal>> tempRankingMap = Maps.newHashMap();
		final BigDecimal avgProfit = tradeRepository.findAveragePlOfProfitableClosedTrades();
		final BigDecimal avgLoss = tradeRepository.findAveragePlOfUnprofitableClosedTrades();

		// get the weighted value of each trade for each provider
		tradeRepository.findAllClosedTrades().stream().forEach(closedTrade -> {
			final BigDecimal pl = closedTrade.getPl();
			if (pl == null) {
				logger.error("When calculating rankings, found a closed trade with no PL: " + closedTrade);
				return;
			}

			// get the PL for each trade and apply a weight to it
			BigDecimal weightedValue = null;
			if (pl.compareTo(BigDecimal.ZERO) < 0) {
				weightedValue = getWeightedTradeValue(pl, avgLoss).negate();
			} else {
				weightedValue = getWeightedTradeValue(pl, avgProfit);
			}

			// add the weighted value to the total
			List<BigDecimal> rankingForProvider = tempRankingMap.get(closedTrade.getProviderName());
			if (rankingForProvider == null) {
				rankingForProvider = new LinkedList<BigDecimal>();
				tempRankingMap.put(closedTrade.getProviderName(), rankingForProvider);
			}
			rankingForProvider.add(weightedValue);
		});

		// for each provider, get the average of all the weighted trades put it in the final map
		final Map<SignalProviderName, BigDecimal> sortedMap = tempRankingMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
			final double rank = entry.getValue().stream().mapToDouble(bd -> bd.doubleValue()).average().getAsDouble();
			return BigDecimal.valueOf(rank).setScale(RANKING_VALUES_DECIMAL_SCALE, RoundingMode.UP);
		})).entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // also sort it!
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

		final SignalProviderRanking ranking = new SignalProviderRanking();
		ranking.setProviderRankingMap(sortedMap);

		// logger.info("Calculated rankings in " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + " ms"); TODO spammy
		return ranking;
	}

	protected BigDecimal getWeightedTradeValue(final BigDecimal val, final BigDecimal avg) {
		if (val.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		// this should depend on the currency. but 2 decimal points gives us enough accuracy anyway
		return val.divide(avg, RANKING_VALUES_DECIMAL_SCALE, RoundingMode.UP).stripTrailingZeros();
	}

}
