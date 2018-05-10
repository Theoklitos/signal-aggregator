package com.quantbro.aggregator.services;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quantbro.aggregator.controllers.api.pojos.Overview;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;

@Service
public class OverviewService {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(OverviewService.class);

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private JobService jobService;

	/**
	 * calculates and returns a pojo with info about the app
	 */
	@Transactional(readOnly = true)
	public Overview getCreateOverview() {
		final DateTime startTime = new DateTime(ManagementFactory.getRuntimeMXBean().getStartTime());
		final Overview overview = new Overview(startTime);

		final List<Trade> openTrades = tradeRepository.findByStatus(TradeStatus.OPENED);
		final double currentBalance = openTrades.stream().filter(trade -> trade.getPl() != null).mapToDouble(openTrade -> openTrade.getPl().doubleValue())
				.sum();
		overview.setTotalPl(BigDecimal.valueOf(currentBalance));

		final int totalTradesTracked = openTrades.size() + tradeRepository.findByStatus(TradeStatus.PENDING).size();
		overview.setNumberOfTrackedTrades(totalTradesTracked);

		final BigDecimal averagePlOfProfitableClosedTrades = tradeRepository.findAveragePlOfProfitableClosedTrades();
		overview.setAveragePlOfProfitableClosedTrades(averagePlOfProfitableClosedTrades);
		final BigDecimal averagePlOfUnprofitableClosedTrades = tradeRepository.findAveragePlOfUnprofitableClosedTrades();
		overview.setAveragePlOfUnprofitableClosedTrades(averagePlOfUnprofitableClosedTrades);

		final List<String> activeJobNames = jobService.getScrapingJobs().stream().filter(job -> job.isEnabled()).map(job -> job.getName())
				.collect(Collectors.toList());
		overview.setActiveProviders(activeJobNames);

		return overview;
	}

}
