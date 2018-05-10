package com.quantbro.aggregator.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.google.common.base.Joiner;
import com.quantbro.aggregator.adapters.SignalProviderName;

/**
 * Represents a ranking of providers at a moment in time
 */
@Entity
@Table(name = "rankings")
public final class SignalProviderRanking {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private final DateTime calculatedAt;

	@ElementCollection
	@CollectionTable(name = "providerRankingMap", joinColumns = @JoinColumn(name = "rankingMap"))
	private Map<SignalProviderName, BigDecimal> providerRanking;

	public SignalProviderRanking() {
		calculatedAt = new DateTime();
		providerRanking = new LinkedHashMap<SignalProviderName, BigDecimal>();
	}

	public void addProviderRank(final SignalProviderName provider, final BigDecimal rank) {
		getProviderRankingMap().put(provider, rank);
	}

	/**
	 * @return null if not found
	 */
	public BigDecimal getProviderRanking(final SignalProviderName name) {
		return providerRanking.get(name);
	}

	public Map<SignalProviderName, BigDecimal> getProviderRankingMap() {
		return providerRanking;
	}

	public void setProviderRankingMap(final Map<SignalProviderName, BigDecimal> providerankingMap) {
		this.providerRanking = providerankingMap;
	}

	public String toPrettyString() {
		return Joiner.on(", ").withKeyValueSeparator(":").join(providerRanking);
	}

	@Override
	public String toString() {
		return getProviderRankingMap().toString();
	}

}
