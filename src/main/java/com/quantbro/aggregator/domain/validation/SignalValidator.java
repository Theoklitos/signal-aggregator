package com.quantbro.aggregator.domain.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;

@Component
public final class SignalValidator implements Validator {

	@Override
	public boolean supports(final Class<?> clazz) {
		return Signal.class.equals(clazz);
	}

	@Override
	public void validate(final Object target, final Errors errors) {
		final Signal signal = (Signal) target;

		if (signal.getStatus().equals(SignalStatus.CLOSED)) {
			if (signal.getEndDate() == null) {
				errors.rejectValue("endDate", "Closed signals must have an end date.");
			}
		}

		if ((signal.getEndDate() != null) && (signal.getEndDate().isBefore(signal.getStartDate()))) {
			errors.reject("Signalhas end date being earlier than its start date");
		}
	}

}
