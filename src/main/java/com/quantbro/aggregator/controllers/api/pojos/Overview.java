package com.quantbro.aggregator.controllers.api.pojos;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import com.quantbro.aggregator.serialization.OverviewSerializer;

/**
 * a simple pojo used to hold some generic overview information about our app
 *
 */
@JsonSerialize(using = OverviewSerializer.class)
public class Overview {

	private final DateTime appStartDate;
	private BigDecimal totalPl;
	private int numberOfTradesTracked;
	private BigDecimal averagePlOfProfitableClosedTrades;
	private BigDecimal averagePlOfUnprofitableClosedTrades;
	private List<String> activeProviders;

	public Overview(final DateTime appStartDate) {
		this.appStartDate = appStartDate;
		activeProviders = Lists.newArrayList();
	}

	public List<String> getActiveProviders() {
		return activeProviders;
	}

	public DateTime getAppStartDate() {
		return appStartDate;
	}

	public BigDecimal getAveragePlOfProfitableClosedTrades() {
		return averagePlOfProfitableClosedTrades;
	}

	public BigDecimal getAveragePlOfUnprofitableClosedTrades() {
		return averagePlOfUnprofitableClosedTrades;
	}

	public int getNumberOfTrackedTrades() {
		return numberOfTradesTracked;
	}

	public BigDecimal getTotalPl() {
		return totalPl;
	}

	public void setActiveProviders(final List<String> activeProviders) {
		this.activeProviders = activeProviders;
	}

	public void setAveragePlOfProfitableClosedTrades(final BigDecimal averagePlOfProfitableClosedTrades) {
		this.averagePlOfProfitableClosedTrades = averagePlOfProfitableClosedTrades;
	}

	public void setAveragePlOfUnprofitableClosedTrades(final BigDecimal averagePlOfUnprofitableClosedTrades) {
		this.averagePlOfUnprofitableClosedTrades = averagePlOfUnprofitableClosedTrades;
	}

	public void setNumberOfTrackedTrades(final int numberOfTradesTracked) {
		this.numberOfTradesTracked = numberOfTradesTracked;
	}

	public void setTotalPl(final BigDecimal totalPl) {
		this.totalPl = totalPl;
	}
}
