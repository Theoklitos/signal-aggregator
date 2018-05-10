package com.quantbro.aggregator.domain;

import com.google.common.base.Objects;

/**
 * self-explanatory
 */
public class InstrumentAndSideWrapper {

	private Instrument instrument;
	private Side side;

	public InstrumentAndSideWrapper(final Instrument instrument, final Side side) {
		this.instrument = instrument;
		this.side = side;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof InstrumentAndSideWrapper) {
			final InstrumentAndSideWrapper wrapper = (InstrumentAndSideWrapper) other;
			return Objects.equal(instrument, wrapper.getInstrument()) && Objects.equal(side, wrapper.getSide());
		}
		return false;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public Side getSide() {
		return side;
	}

	public void setInstrument(final Instrument instrument) {
		this.instrument = instrument;
	}

	public void setSide(final Side side) {
		this.side = side;
	}

}
