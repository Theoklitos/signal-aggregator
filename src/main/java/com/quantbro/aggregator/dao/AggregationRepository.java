package com.quantbro.aggregator.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Aggregation.AggregationStatus;

public interface AggregationRepository extends CrudRepository<Aggregation, Long> {

	List<Aggregation> findByStatus(final AggregationStatus status);

}
