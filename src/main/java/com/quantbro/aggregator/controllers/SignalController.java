package com.quantbro.aggregator.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.services.AggregationService;
import com.quantbro.aggregator.services.SignalService;

@Controller
public final class SignalController {

	@Autowired
	private SignalService signalService;

	@Autowired
	private AggregationService aggregationService;

	@RequestMapping(value = "/signals", method = RequestMethod.GET)
	public String getAllSignals(final Model model) {
		model.addAttribute("liveAggregations", aggregationService.getLiveAggregations());
		model.addAttribute("liveSignals", sortSignals(signalService.getSignalsByStatus(SignalStatus.LIVE)));
		model.addAttribute("staleSignals", sortSignals(signalService.getSignalsByStatus(SignalStatus.STALE)));
		model.addAttribute("closedSignals", signalService.findLatestClosedSignals(15));
		return "signals";
	}

	private List<Signal> sortSignals(final List<Signal> signals) {
		return signals.stream().sorted((final Signal s1, final Signal s2) -> {
			final Trade t1 = s1.getTrade();
			final Trade t2 = s2.getTrade();
			if (t1 == null && t2 == null) {
				return 0;
			} else if (t1 != null && t2 != null) {
				final BigDecimal pl1 = (t1.getPl() == null) ? BigDecimal.ZERO : t1.getPl();
				final BigDecimal pl2 = (t2.getPl() == null) ? BigDecimal.ZERO : t2.getPl();
				return pl2.compareTo(pl1);
			}
			if (t1 == null && t2 != null) {
				return 1;
			}
			if (t1 != null && t2 == null) {
				return -1;
			}
			return -1;
		}).collect(Collectors.toList());
	}

}