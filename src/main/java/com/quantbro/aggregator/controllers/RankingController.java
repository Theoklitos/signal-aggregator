package com.quantbro.aggregator.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.quantbro.aggregator.domain.SignalProviderRanking;
import com.quantbro.aggregator.services.RankingService;

@Controller
public class RankingController {

	@Autowired
	private RankingService rankingService;

	@RequestMapping(value = "/ranking", method = RequestMethod.GET)
	public String getRankings(final Model model) {
		final SignalProviderRanking ranking = rankingService.calculateAndGetRanking();
		model.addAttribute("ranking", ranking);
		return "ranking";
	}
}