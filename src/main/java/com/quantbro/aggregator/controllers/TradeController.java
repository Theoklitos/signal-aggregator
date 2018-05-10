package com.quantbro.aggregator.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.services.TradingService;

@Controller
public class TradeController {

	@Autowired
	private TradingService tradingService;

	@RequestMapping(value = "/trades", method = RequestMethod.GET)
	public String getAllTrades(final Model model) {
		model.addAttribute("openTrades", tradingService.getTradesByStatus(TradeStatus.OPENED));
		model.addAttribute("pendingTrades", tradingService.getTradesByStatus(TradeStatus.PENDING));
		model.addAttribute("closedTrades", tradingService.findLatestClosedTrades(15));
		model.addAttribute("erroneousTrades", tradingService.getTradesByStatus(TradeStatus.ERROR));
		return "trades";
	}

	@RequestMapping(value = "/trades/{tradeId}", method = RequestMethod.GET)
	public String getSingleTrade(final Model model, @PathVariable("tradeId") final Long tradeId) {
		final Trade trade = tradingService.getTradeById(tradeId);
		model.addAttribute("trade", trade);
		if (trade.getStatus().equals(TradeStatus.ERROR)) {
			// truncate the json?
			model.addAttribute("tradeErrorMessage", trade.getMessage());
		}

		return "singleTrade";
	}

}