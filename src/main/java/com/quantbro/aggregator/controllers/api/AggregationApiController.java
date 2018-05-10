package com.quantbro.aggregator.controllers.api;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.services.AggregationService;

@RestController
public class AggregationApiController {

	@Autowired
	private AggregationService aggregationService;

	@RequestMapping(method = RequestMethod.GET, path = "/api/aggregations")
	public Collection<Aggregation> getAllAggregations() {
		return aggregationService.getAllAggregations();
	}
}
