package com.quantbro.aggregator.services;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * wrapper around the results of an operation that updates/closes/opens stuff.
 */
public final class UpdateOperationResult {

	private final static String UPDATED_KEY = "updated";
	private final static String CLOSED_KEY = "closed";
	private final static String OPEN_KEY = "open";
	private final static String REMOTE_CALL_KEY = "calls";

	private final Map<String, Integer> results;

	public UpdateOperationResult() {
		results = Maps.newConcurrentMap();
		results.put(UPDATED_KEY, 0);
		results.put(CLOSED_KEY, 0);
		results.put(OPEN_KEY, 0);
		results.put(REMOTE_CALL_KEY, 0);
	}

	/**
	 * a call to a remote api was made
	 */
	public void called() {
		increment(REMOTE_CALL_KEY);
	}

	public void closed() {
		increment(CLOSED_KEY);
	}

	public int getClosed() {
		return results.get(CLOSED_KEY);
	}

	public int getOpened() {
		return results.get(OPEN_KEY);
	}

	private void increment(final String key) {
		final int existing = results.get(key);
		results.put(key, existing + 1);
	}

	public void opened() {
		increment(OPEN_KEY);
	}

	@Override
	public String toString() {
		return Joiner.on(", ").withKeyValueSeparator(": ").join(results);
	}

	public void updated() {
		increment(UPDATED_KEY);
	}

}
