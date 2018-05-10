package com.quantbro.aggregator.trading;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

/**
 * When something goes wrong during trading (via a remote trader/API)
 */
public class TradingException extends Error {

	private static final long serialVersionUID = -3418288411588099694L;
	private JSONObject jsonOfError;

	public TradingException(final ResponseEntity<String> responseEntity) {
		this("Trader responded with (" + responseEntity.getStatusCode().value() + ") " + responseEntity.getStatusCode().getReasonPhrase() + ". "
				+ responseEntity.getBody());
	}

	public TradingException(final String message) {
		this(message, new JSONObject());
	}

	public TradingException(final String message, final JSONObject errorJson) {
		super("Error when trading: " + message + ". Relevant json:\n" + errorJson.toString(5));
		this.jsonOfError = errorJson;
	}

	public TradingException(final Throwable t) {
		super(t);
	}

	public String getPrettyJsonOfError() {
		return (jsonOfError == null) ? "" : jsonOfError.toString(5);
	}

}
