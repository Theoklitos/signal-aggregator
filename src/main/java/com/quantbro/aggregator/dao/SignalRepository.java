package com.quantbro.aggregator.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;

public interface SignalRepository extends CrudRepository<Signal, Long> {

	// TODO do the signal aggregation grouping here via a JPA query.
	// See: https://stackoverflow.com/questions/36328063/how-to-return-a-custom-object-from-a-spring-data-jpa-group-by-query

	@Query("SELECT s FROM Signal s LEFT JOIN s.trade ORDER BY s.trade.pl DESC")
	List<Signal> findAllOrderedByPl();

	List<Signal> findByProviderNameAndStatus(final SignalProviderName name, final SignalStatus status);

	List<Signal> findByStatus(final SignalStatus status);

	@Query("FROM Signal WHERE status IN :statuses")
	List<Signal> findByStatuses(final @Param("statuses") SignalStatus... statuses);

	@Query(nativeQuery = true, value = "SELECT * FROM signals WHERE status='CLOSED' ORDER BY end_date DESC LIMIT :number")
	List<Signal> findLatestClosedSignals(@Param("number") int maxNumberOfSignals);

}
