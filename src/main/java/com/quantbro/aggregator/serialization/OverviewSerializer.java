package com.quantbro.aggregator.serialization;

import java.io.IOException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.quantbro.aggregator.controllers.api.pojos.Overview;

public class OverviewSerializer extends JsonSerializer<Overview> {

	public final static DateTimeFormatter API_DATE_FORMAT = ISODateTimeFormat.dateTime();

	@Override
	public void serialize(final Overview value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeStartObject();

		gen.writeStringField("appStartDate", value.getAppStartDate().toString());
		gen.writeNumberField("numberOfTrackedTrades", value.getNumberOfTrackedTrades());

		gen.writeNumberField("totalPl", value.getTotalPl());
		gen.writeNumberField("averageProfit", value.getAveragePlOfProfitableClosedTrades());
		gen.writeNumberField("averageLoss", value.getAveragePlOfUnprofitableClosedTrades());

		gen.writeFieldName("activeProviders");
		gen.writeStartArray();
		for (final String providerName : value.getActiveProviders()) {
			gen.writeString(providerName);
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}
}
