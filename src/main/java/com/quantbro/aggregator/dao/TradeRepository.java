package com.quantbro.aggregator.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;

public interface TradeRepository extends CrudRepository<Trade, Long> {

	@Query("from Trade WHERE status LIKE '%CLOSED%'")
	List<Trade> findAllClosedTrades();

	@Query("SELECT avg(pl) from Trade WHERE status LIKE '%CLOSED%' AND pl > 0.0")
	BigDecimal findAveragePlOfProfitableClosedTrades();

	@Query("SELECT avg(pl) from Trade WHERE status LIKE '%CLOSED%' AND pl < 0.0")
	BigDecimal findAveragePlOfUnprofitableClosedTrades();

	Trade findByRemoteId(final String remoteId);

	List<Trade> findByStatus(final TradeStatus status);

	@Query("FROM Trade WHERE providerName = :providerName AND status LIKE '%CLOSED%'")
	List<Trade> findClosedTradesByProviderName(@Param("providerName") final SignalProviderName providerName);

	@Query(nativeQuery = true, value = "SELECT * FROM trades WHERE status LIKE '%CLOSED%' ORDER BY end_date DESC LIMIT :number")
	List<Trade> findLatestClosedTrades(@Param("number") int numberOfTrades);

	@Query("FROM Trade WHERE providerName = :providerName AND status NOT LIKE '%CLOSED%'")
	List<Trade> findNonClosedTradesByProviderName(@Param("providerName") final SignalProviderName providerName);
}
