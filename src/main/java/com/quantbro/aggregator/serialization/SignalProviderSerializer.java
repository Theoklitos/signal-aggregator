package com.quantbro.aggregator.serialization;

import java.io.IOException;
import java.math.RoundingMode;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.quantbro.aggregator.controllers.api.pojos.SignalProvider;
import com.quantbro.aggregator.domain.Trade;

public class SignalProviderSerializer extends JsonSerializer<SignalProvider> {

	public final static DateTimeFormatter API_DATE_FORMAT = ISODateTimeFormat.dateTime();

	@Override
	public void serialize(final SignalProvider value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeStringField("name", value.getName().toString());
		if (value.getRank() != null) {
			gen.writeNumberField("rank", value.getRank().setScale(2, RoundingMode.UP));
		}
		gen.writeNumberField("totalPl", value.getTotalPl().setScale(2, RoundingMode.UP));

		gen.writeFieldName("trades");
		gen.writeStartArray();
		for (final Trade trade : value.getClosedTrades()) {
			gen.writeObject(trade);
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}
}
