package com.quantbro.aggregator.domain;

import java.math.BigDecimal;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.validation.PositiveNumber;
import com.quantbro.aggregator.serialization.SignalSerializer;

@Entity
@Table(name = "signals")
@JsonSerialize(using = SignalSerializer.class)
public class Signal extends AbstractTimer {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@NotNull
	@Enumerated(EnumType.STRING)
	private SignalProviderName providerName;

	@NotNull
	@Enumerated(EnumType.STRING)
	private Instrument instrument;

	@NotNull
	@Enumerated(EnumType.STRING)
	private Side side;

	@NotNull
	@Enumerated(EnumType.STRING)
	private SignalStatus status;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "tradeId")
	private Trade trade;

	@Digits(integer = 9, fraction = 5)
	@PositiveNumber
	private BigDecimal stopLoss;

	@Digits(integer = 9, fraction = 5)
	@PositiveNumber
	private BigDecimal takeProfit;

	@Digits(integer = 9, fraction = 5)
	@PositiveNumber(allowNull = true)
	private BigDecimal entryPrice;

	public Signal() {

	}

	/**
	 * also sets the status to LIVE
	 */
	public Signal(final SignalProviderName providerName) {
		this.providerName = providerName;
		this.status = SignalStatus.LIVE;
	}

	/**
	 * also sets the status to LIVE
	 */
	public Signal(final SignalProviderName providerName, final Instrument instrument, final Side side, final BigDecimal stopLoss, final BigDecimal takeProfit,
			final Optional<BigDecimal> entryPrice) {
		super();
		this.instrument = instrument;
		this.side = side;
		this.stopLoss = stopLoss;
		this.takeProfit = takeProfit;
		this.providerName = providerName;
		this.status = SignalStatus.LIVE;
		entryPrice.ifPresent(price -> this.entryPrice = price);
	}

	/**
	 * Sets state to CLOSE and endDate to now
	 */
	public void close() {
		setStatus(SignalStatus.CLOSED);
		end();
	}

	public Optional<BigDecimal> getEntryPrice() {
		return (entryPrice == null) ? Optional.empty() : Optional.of(entryPrice);
	}

	public long getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public SignalProviderName getProviderName() {
		return providerName;
	}

	public Side getSide() {
		return side;
	}

	public SignalStatus getStatus() {
		return status;
	}

	public BigDecimal getStopLoss() {
		return stopLoss;
	}

	public BigDecimal getTakeProfit() {
		return takeProfit;
	}

	public Trade getTrade() {
		return trade;
	}

	public boolean hasTrade() {
		return trade != null;
	}

	public boolean isClosed() {
		return status == SignalStatus.CLOSED;
	}

	/**
	 * are these objects referring to the "same" external signal?
	 */
	public boolean matches(final Signal signal) {
		boolean entryPriceEquals = false;
		final Optional<BigDecimal> thisEntryPrice = getEntryPrice();
		final Optional<BigDecimal> otherEntryPrice = signal.getEntryPrice();
		if (!thisEntryPrice.isPresent() && !otherEntryPrice.isPresent()) {
			entryPriceEquals = true;
		} else if (thisEntryPrice.isPresent() && otherEntryPrice.isPresent()) {
			entryPriceEquals = thisEntryPrice.get().compareTo(otherEntryPrice.get()) == 0;
		}
		return providerName.equals(signal.getProviderName()) && instrument.equals(signal.getInstrument()) && side.equals(signal.getSide())
				&& (stopLoss.compareTo(signal.getStopLoss()) == 0) && (takeProfit.compareTo(signal.getTakeProfit()) == 0) && entryPriceEquals;
	}

	public void setEntryPrice(final BigDecimal entryPrice) {
		this.entryPrice = entryPrice;
	}

	public void setInstrument(final Instrument pair) {
		this.instrument = pair;
	}

	public void setProviderName(final SignalProviderName providerName) {
		this.providerName = providerName;
	}

	public void setSide(final Side side) {
		this.side = side;
	}

	public void setStatus(final SignalStatus status) {
		this.status = status;
	}

	public void setStopLoss(final BigDecimal stopLoss) {
		this.stopLoss = stopLoss;
	}

	public void setTakeProfit(final BigDecimal takeProfit) {
		this.takeProfit = takeProfit;
	}

	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

	@Override
	public String toString() {
		String entryPriceString = "";
		if (getEntryPrice().isPresent()) {
			entryPriceString = ". Entry price: " + getEntryPrice().get();
		}
		return "[" + getProviderName() + "] " + getInstrument() + " " + getSide() + entryPriceString + " SL(" + getStopLoss() + ") TP(" + getTakeProfit()
				+ "). Status: " + status + " " + abstractToString();
	}

}