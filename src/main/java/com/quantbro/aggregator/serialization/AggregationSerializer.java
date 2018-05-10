package com.quantbro.aggregator.serialization;

import java.io.IOException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Signal;

public class AggregationSerializer extends JsonSerializer<Aggregation> {

	public final static DateTimeFormatter API_DATE_FORMAT = ISODateTimeFormat.dateTime();

	@Override
	public void serialize(final Aggregation value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeStringField("instrument", value.getInstrument().toString());
		gen.writeStringField("side", value.getSide().toString());
		gen.writeNumberField("rank", value.getTotalRank());
		gen.writeStringField("status", value.getStatus().toString());

		gen.writeFieldName("signals");
		gen.writeStartArray();
		for (final Signal signal : value.getSignals()) {
			gen.writeObject(signal);
		}
		gen.writeEndArray();

		gen.writeStringField("detectionDate", value.getDetectionDate().toString(API_DATE_FORMAT));
		if (value.getEndDate() != null) {
			gen.writeStringField("endDate", value.getEndDate().toString(API_DATE_FORMAT));
		}
		gen.writeEndObject();
	}
}
