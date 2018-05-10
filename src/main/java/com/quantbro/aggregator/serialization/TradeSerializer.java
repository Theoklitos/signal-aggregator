package com.quantbro.aggregator.serialization;

import java.io.IOException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.Trade;

public class TradeSerializer extends JsonSerializer<Trade> {

	public final static DateTimeFormatter API_DATE_FORMAT = ISODateTimeFormat.dateTime();

	@Override
	public void serialize(final Trade value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeNumberField("id", value.getId());
		gen.writeStringField("signalProvider", value.getProviderName().toString());
		gen.writeStringField("status", value.getStatus().toString());

		final Signal signal = value.getSignal();
		if (signal != null) {
			gen.writeStringField("instrument", signal.getInstrument().toString());
			gen.writeStringField("side", signal.getSide().toString());
		}

		gen.writeStringField("startDate", value.getStartDate().toString(API_DATE_FORMAT));
		if (value.getEndDate() != null) {
			gen.writeStringField("endDate", value.getEndDate().toString(API_DATE_FORMAT));
		}
		gen.writeNumberField("pl", value.getPl());
		gen.writeEndObject();
	}
}
