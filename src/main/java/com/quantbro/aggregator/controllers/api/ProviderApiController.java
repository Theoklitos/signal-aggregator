package com.quantbro.aggregator.controllers.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.controllers.api.pojos.SignalProvider;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.SignalProviderRanking;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.services.RankingService;

@RestController
public class ProviderApiController {

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private RankingService rankingService;

	@RequestMapping(method = RequestMethod.GET, path = "/api/providers")
	public List<SignalProvider> getAllProviders() {
		final SignalProviderRanking latestRanking = rankingService.calculateAndGetRanking();
		return Stream.of(SignalProviderName.values()).map(name -> {
			final BigDecimal rank = latestRanking.getProviderRanking(name);
			if (rank == null) {
				return null;
			}
			final SignalProvider provider = new SignalProvider();
			provider.setName(name);
			provider.setRank(rank);
			final List<Trade> closedTrades = tradeRepository.findClosedTradesByProviderName(name);
			provider.setClosedTrades(closedTrades);
			final BigDecimal sumPl = BigDecimal.valueOf(
					closedTrades.stream().filter(trade -> trade.getPl() != null).map(trade -> trade.getPl()).mapToDouble(bd -> bd.doubleValue()).sum());
			provider.setTotalPl(sumPl);
			return provider;
		}).filter(ranking -> ranking != null).sorted((p1, p2) -> {
			return p2.getRank().compareTo(p1.getRank());
		}).collect(Collectors.toList());

	}
}
