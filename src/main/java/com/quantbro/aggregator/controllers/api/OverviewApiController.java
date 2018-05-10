package com.quantbro.aggregator.controllers.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.quantbro.aggregator.controllers.api.pojos.Overview;
import com.quantbro.aggregator.services.OverviewService;

@RestController
public class OverviewApiController {

	@Autowired
	private OverviewService overviewService;

	@RequestMapping(method = RequestMethod.GET, path = "/api/overview")
	public Overview getOverview() {
		return overviewService.getCreateOverview();
	}
}
