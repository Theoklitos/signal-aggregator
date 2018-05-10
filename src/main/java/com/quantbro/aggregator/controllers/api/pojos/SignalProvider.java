package com.quantbro.aggregator.controllers.api.pojos;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.serialization.SignalProviderSerializer;

/**
 * a simple pojo used to carry all the relevant info for a provider around
 *
 */
@JsonSerialize(using = SignalProviderSerializer.class)
public class SignalProvider {

	private SignalProviderName name;
	private BigDecimal totalPl;
	private BigDecimal rank;
	private List<Trade> closedTrades;

	public List<Trade> getClosedTrades() {
		return closedTrades;
	}

	public SignalProviderName getName() {
		return name;
	}

	public BigDecimal getRank() {
		return rank;
	}

	public BigDecimal getTotalPl() {
		return totalPl;
	}

	public void setClosedTrades(final List<Trade> closedTrades) {
		this.closedTrades = closedTrades;
	}

	public void setName(final SignalProviderName name) {
		this.name = name;
	}

	public void setRank(final BigDecimal rank) {
		this.rank = rank;
	}

	public void setTotalPl(final BigDecimal totalPl) {
		this.totalPl = totalPl;
	}

}
