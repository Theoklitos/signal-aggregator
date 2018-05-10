package com.quantbro.aggregator.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.serialization.AggregationSerializer;

/**
 * represents some signals all pointing in the same direction
 */
@Entity
@Table(name = "aggregations")
@JsonSerialize(using = AggregationSerializer.class)
public class Aggregation {

	public enum AggregationStatus {
		LIVE, CLOSED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@ManyToMany
	@JoinTable(name = "aggregation_signals")
	private List<Signal> signals;

	@OneToOne(cascade = CascadeType.ALL)
	private SignalProviderRanking ranking;

	@NotNull
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime detectionDate;

	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime endDate;

	@Enumerated(EnumType.STRING)
	private AggregationStatus status;

	@Enumerated(EnumType.STRING)
	private Instrument instrument;

	@Enumerated(EnumType.STRING)
	private Side side;

	protected Aggregation() {

	}

	public Aggregation(final Instrument instrument, final Side side, final List<Signal> signals) {
		this.setSide(side);
		this.setInstrument(instrument);
		this.setDetectionDate(new DateTime());
		this.setSignals(signals);
		setStatus(AggregationStatus.LIVE);
	}

	/**
	 * sets the status and endDate
	 */
	public void close() {
		status = AggregationStatus.CLOSED;
		endDate = new DateTime();
	}

	public DateTime getDetectionDate() {
		return detectionDate;
	}

	/**
	 * returns null if still live
	 */
	public DateTime getEndDate() {
		return endDate;
	}

	public long getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	/**
	 * searches through the ranking info in this aggregation for a rank that belongs to the signal provider of the given signal
	 *
	 * @return null if not found
	 */
	public BigDecimal getRankForProviderOfSignal(final Signal signal) {
		return ranking.getProviderRanking(signal.getProviderName());
	}

	public SignalProviderRanking getRanking() {
		return ranking;
	}

	public Side getSide() {
		return side;
	}

	public List<Signal> getSignals() {
		return signals;
	}

	public AggregationStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalRank() {
		final Set<SignalProviderName> allProvidersOfSignals = signals.stream().map(signal -> signal.getProviderName()).collect(Collectors.toSet());
		final List<BigDecimal> ranks = ranking.getProviderRankingMap().entrySet().stream().filter(es -> allProvidersOfSignals.contains(es.getKey()))
				.map(es -> es.getValue()).collect(Collectors.toList());
		return BigDecimal.valueOf(ranks.stream().mapToDouble(bg -> bg.doubleValue()).average().getAsDouble()).setScale(2, RoundingMode.UP);
	}

	/**
	 * used to compare aggregations without messing with equals()
	 */
	public boolean matches(final Aggregation aggregation) {
		return getInstrument().equals(aggregation.getInstrument()) && getSide().equals(aggregation.getSide());
	}

	public void setDetectionDate(final DateTime detectionDate) {
		this.detectionDate = detectionDate;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public void setInstrument(final Instrument instrument) {
		this.instrument = instrument;
	}

	public void setRanking(final SignalProviderRanking ranking) {
		this.ranking = ranking;
	}

	public void setSide(final Side side) {
		this.side = side;
	}

	public void setSignals(final List<Signal> signals) {
		this.signals = signals;
	}

	public void setStatus(final AggregationStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "[" + getId() + "] " + side + " " + instrument + " (" + signals.size() + ")";
	}

}
