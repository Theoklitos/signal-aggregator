package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;
import com.quantbro.aggregator.Fixtures;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.SignalProviderRanking;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;

@RunWith(SpringRunner.class)
public class RankingServiceUnitTest {

	@InjectMocks
	private RankingService service;

	@Mock
	private TradeRepository tradeRepository;

	@Test
	public void testInitialRanking() {
		final Trade profitableTrade = Fixtures.createTrade(SignalProviderName.DUXFOREX, BigDecimal.valueOf(16.54), TradeStatus.CLOSED_BY_TAKEPROFIT);

		when(tradeRepository.findAllClosedTrades()).thenReturn(Lists.newArrayList(profitableTrade));
		when(tradeRepository.findAveragePlOfProfitableClosedTrades()).thenReturn(BigDecimal.valueOf(16.54));
		when(tradeRepository.findAveragePlOfUnprofitableClosedTrades()).thenReturn(BigDecimal.valueOf(0));

		final SignalProviderRanking ranking = service.calculateAndGetRanking();
		assertEquals(1, ranking.getProviderRankingMap().size());
		assertEquals(BigDecimal.valueOf(1.00), ranking.getProviderRankingMap().get(SignalProviderName.DUXFOREX));
	}

	@Test
	public void testRankingOfTrades() {
		final Trade duxp1 = Fixtures.createTrade(SignalProviderName.DUXFOREX, BigDecimal.valueOf(16.54), TradeStatus.CLOSED_BY_TAKEPROFIT);
		final Trade duxp2 = Fixtures.createTrade(SignalProviderName.DUXFOREX, BigDecimal.valueOf(12.2), TradeStatus.CLOSED_BY_TAKEPROFIT);
		final Trade duxl1 = Fixtures.createTrade(SignalProviderName.DUXFOREX, BigDecimal.valueOf(-20.87), TradeStatus.CLOSED_BY_STOPLOSS);
		final Trade forp1 = Fixtures.createTrade(SignalProviderName.FORESIGNAL, BigDecimal.valueOf(25.11), TradeStatus.CLOSED_BY_TAKEPROFIT);

		final List<Trade> closedTrades = Lists.newArrayList(forp1, duxp1, duxp2, duxl1);
		when(tradeRepository.findAllClosedTrades()).thenReturn(closedTrades);
		when(tradeRepository.findAveragePlOfProfitableClosedTrades()).thenReturn(BigDecimal.valueOf(13.4625));
		when(tradeRepository.findAveragePlOfUnprofitableClosedTrades()).thenReturn(BigDecimal.valueOf(-20.87));

		final SignalProviderRanking ranking = service.calculateAndGetRanking();
		assertEquals(2, ranking.getProviderRankingMap().size());
		assertEquals(BigDecimal.valueOf(0.39), ranking.getProviderRankingMap().get(SignalProviderName.DUXFOREX));
		assertEquals(BigDecimal.valueOf(1.87), ranking.getProviderRankingMap().get(SignalProviderName.FORESIGNAL));
	}

	@Test
	public void testWeightOfTrades() {
		final BigDecimal result1 = service.getWeightedTradeValue(BigDecimal.valueOf(1.50), BigDecimal.valueOf(1.00));
		assertEquals(BigDecimal.valueOf(1.5), result1);

		final BigDecimal result2 = service.getWeightedTradeValue(BigDecimal.valueOf(.50), BigDecimal.valueOf(1.00));
		assertEquals(BigDecimal.valueOf(0.5), result2);

		final BigDecimal result3 = service.getWeightedTradeValue(BigDecimal.valueOf(0), BigDecimal.valueOf(1.00));
		assertEquals(BigDecimal.valueOf(0), result3);
	}

}
