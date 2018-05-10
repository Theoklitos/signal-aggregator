package com.quantbro.aggregator.controllers.advice;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.quantbro.aggregator.domain.NotFoundException;

@ControllerAdvice
public class GeneralControllerAdvice {

	@ExceptionHandler(IllegalArgumentException.class)
	public ModelAndView handleBadRequest(final HttpServletRequest request, final IllegalArgumentException exception) {
		final ModelAndView mav = new ModelAndView();
		mav.setStatus(HttpStatus.BAD_REQUEST);
		mav.setViewName("errors/badRequest");
		mav.addObject("message", exception.getLocalizedMessage());
		return mav;
	}

	@ExceptionHandler(NotFoundException.class)
	public ModelAndView handleNotFound(final HttpServletRequest request, final NotFoundException exception) {
		final ModelAndView mav = new ModelAndView();
		mav.setStatus(HttpStatus.NOT_FOUND);
		mav.setViewName("errors/notFoundError");
		mav.addObject("message", exception.getLocalizedMessage());
		return mav;
	}
}
