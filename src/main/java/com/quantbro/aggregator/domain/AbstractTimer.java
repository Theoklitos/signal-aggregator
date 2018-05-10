package com.quantbro.aggregator.domain;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.quantbro.aggregator.utils.StringUtils;

/**
 * can keep track of a "start" and an "end" date, also the duration between them
 */
@MappedSuperclass
public abstract class AbstractTimer {

	@NotNull
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime startDate;

	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime endDate;

	public AbstractTimer() {
		startDate = new DateTime();
	}

	/**
	 * returns a toString() representation of the abstract part of the class
	 */
	protected String abstractToString() {
		final String duration = StringUtils.getReadableDuration(duration());
		final String durationString = (duration == null) ? "" : " duration: " + duration;
		final String endDateString = (endDate == null) ? "" : ", ended: " + StringUtils.getReadableDateTime(endDate);
		return "started " + StringUtils.getReadableDateTime(startDate) + endDateString + durationString;
	}

	/**
	 * returns null if no endDate has been set
	 */
	public Duration duration() {
		if (endDate == null) {
			return null;
		}
		return new Duration(startDate, endDate);
	}

	public void end() {
		setEndDate(new DateTime());
	}

	public DateTime getEndDate() {
		return endDate;
	}

	public DateTime getStartDate() {
		return startDate;
	}

	public void setEndDate(final DateTime endDate) {
		this.endDate = endDate;
	}

	public void setStartDate(final DateTime startDate) {
		this.startDate = startDate;
	}

}
