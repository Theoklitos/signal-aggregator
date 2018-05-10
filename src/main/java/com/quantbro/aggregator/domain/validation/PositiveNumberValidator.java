package com.quantbro.aggregator.domain.validation;

import java.math.BigDecimal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PositiveNumberValidator implements ConstraintValidator<PositiveNumber, BigDecimal> {

	private boolean allowNull;

	@Override
	public void initialize(final PositiveNumber constraintAnnotation) {
		allowNull = constraintAnnotation.allowNull();
	}

	@Override
	public boolean isValid(final BigDecimal value, final ConstraintValidatorContext context) {
		if (allowNull && value == null) {
			return true;
		}
		return (value != null) && (value.compareTo(BigDecimal.ZERO) == 1);
	}
}
