package com.quantbro.aggregator.domain.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.utils.ForexUtils;
import com.quantbro.aggregator.utils.StringUtils;

@Component
public final class TradeValidator implements Validator {

	@Override
	public boolean supports(final Class<?> clazz) {
		return Trade.class.equals(clazz);
	}

	@Override
	public void validate(final Object target, final Errors errors) {
		final Trade trade = (Trade) target;
		if (trade.getStatus().isClosed()) {
			if (trade.getEndDate() == null) {
				errors.rejectValue("endDate", "Closed trades must have an end date.");
			}
			if (trade.getPl() == null) {
				errors.rejectValue("pl", "Closed trades must have a profit/loss value assigned to them.");
			}
			if (StringUtils.isBlank(trade.getJson())) {
				errors.rejectValue("json", "Closed trades must contain some json.");
			}
		} else if (trade.getStatus().equals(TradeStatus.OPENED)) {
			if (ForexUtils.isPositiveNumber(trade.getEntryPrice())) {
				errors.rejectValue("entryPrice", "An opened trade needs to know the price it was opened at.");
			}
		}

		if ((trade.getEndDate() != null) && (trade.getEndDate().isBefore(trade.getStartDate()))) {
			errors.reject("Trade has end date being earlier than its start date");
		}
	}

}
