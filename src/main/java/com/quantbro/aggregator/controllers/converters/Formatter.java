package com.quantbro.aggregator.controllers.converters;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.stereotype.Component;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.utils.StringUtils;

/**
 * Our custom formatter for thymeleaf
 */
@Component
public class Formatter {

	public String asDate(final DateTime dateTime) {
		return StringUtils.getReadableDateTime(dateTime);
	}

	public String asDuration(final DateTime startDate, final DateTime endDate) {
		if (startDate == null && endDate == null) {
			return "";
		}
		final DateTime actualEndDate = (endDate == null) ? new DateTime() : endDate;
		return StringUtils.getReadableDuration(new Duration(startDate, actualEndDate));
	}

	public String asDuration(final Long milliseconds) {
		if (milliseconds == null || milliseconds == 0L) {
			return "";
		}
		return StringUtils.getReadableDuration(new Duration(milliseconds));
	}

	public String asDuration(final Optional<Long> millisecondsOpt) {
		if (millisecondsOpt.isPresent()) {
			return asDuration(millisecondsOpt.get());
		} else {
			return "";
		}
	}

	/**
	 * returns the given bigdecimal as a readable and understandable money string (e.g. 1.3423)
	 */
	public String asMoneyString(final BigDecimal money) {
		if (money == null || (money.compareTo(BigDecimal.ZERO) == 0)) {
			return "";
		} else {
			return money.stripTrailingZeros().toPlainString();
		}
	}

	/**
	 * returns the money of the given signal's trade (if any) as a readable and understandable money string (e.g. 1.3423)
	 */
	public String asMoneyString(final Optional<BigDecimal> moneyOpt) {
		if (moneyOpt == null || !moneyOpt.isPresent()) {
			return "";
		} else {
			return asMoneyString(moneyOpt.get());
		}
	}

	/**
	 * returns the money of the given signal's trade (if any) as a readable and understandable money string (e.g. 1.3423)
	 */
	public String asMoneyString(final Signal signal) {
		if (signal == null || signal.getTrade() == null) {
			return "";
		} else {
			return asMoneyString(signal.getTrade().getPl());
		}
	}

	/**
	 * returns a readable string with information about the providers in this aggregation
	 */
	public String asProvidersString(final Aggregation aggregation) {
		final Set<SignalProviderName> allProvidersOfTrades = aggregation.getSignals().stream().map(signal -> signal.getProviderName())
				.collect(Collectors.toSet());
		return aggregation.getRanking().getProviderRankingMap().entrySet().stream().filter(es -> allProvidersOfTrades.contains(es.getKey()))
				.map(es -> es.getKey() + ": " + es.getValue()).collect(Collectors.joining(", "));
	}

	public String getBinaryClassname() {
		return "";
	}

	/**
	 * hack/ugly code: returns the classname for the html tables, based on how profitable it is
	 */
	public String getClassForMoney(final Trade trade) {
		if (trade == null || trade.getPl() == null || (trade.getPl().compareTo(BigDecimal.ZERO) == 0)) {
			return "";
		} else {
			return (trade.getPl().compareTo(BigDecimal.ZERO) == 1) ? "profit" : "loss";
		}
	}

}
