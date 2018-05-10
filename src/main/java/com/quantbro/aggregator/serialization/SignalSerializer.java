package com.quantbro.aggregator.serialization;

import java.io.IOException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.quantbro.aggregator.domain.Signal;

public class SignalSerializer extends JsonSerializer<Signal> {

	public final static DateTimeFormatter API_DATE_FORMAT = ISODateTimeFormat.dateTime();

	@Override
	public void serialize(final Signal value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeNumberField("id", value.getId());
		gen.writeStringField("signalProvider", value.getProviderName().toString());
		gen.writeStringField("instrument", value.getInstrument().toString());
		gen.writeStringField("status", value.getStatus().toString());
		gen.writeStringField("side", value.getSide().toString());
		gen.writeStringField("startDate", value.getStartDate().toString(API_DATE_FORMAT));
		if (value.getEndDate() != null) {
			gen.writeStringField("endDate", value.getEndDate().toString(API_DATE_FORMAT));
		}
		gen.writeNumberField("stoploss", value.getStopLoss());
		gen.writeNumberField("takeprofit", value.getTakeProfit());
		if (value.getEntryPrice().isPresent()) {
			gen.writeNumberField("entryPrice", value.getEntryPrice().get());
		}
		if (value.getTrade() != null) {
			gen.writeNumberField("tradeId", value.getTrade().getId());
			gen.writeStringField("tradeStatus", value.getTrade().getStatus().toString());
			gen.writeNumberField("pl", value.getTrade().getPl());
		}
		gen.writeEndObject();
	}
}
