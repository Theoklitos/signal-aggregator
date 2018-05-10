package com.quantbro.aggregator.controllers.api;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quantbro.aggregator.domain.NotFoundException;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.services.SignalService;
import com.quantbro.aggregator.services.UpdateOperationResult;

@RestController
public class SignalApiController {

	private final static Logger logger = LoggerFactory.getLogger(SignalApiController.class);

	@Autowired
	private SignalService signalService;

	@RequestMapping(method = RequestMethod.GET, path = "/api/signals")
	public Collection<Signal> getAllSignals() {
		return signalService.findAllOrderedByPl();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/api/signals/{signalId}")
	public Signal getSingleSignal(@PathVariable("signalId") final String signalId) throws NotFoundException {
		return signalService.getSignalById(Integer.valueOf(signalId)).orElseThrow(() -> new NotFoundException(signalId));
	}

	@ExceptionHandler(NumberFormatException.class)
	void handleBadRequests(final HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.BAD_REQUEST.value(), "Signal IDs have to be numeric");
	}

	@ExceptionHandler(NotFoundException.class)
	void handleSignalNotFoundRequests(final HttpServletResponse response, final NotFoundException e) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value(), e.getLocalizedMessage());
	}

	@RequestMapping(method = RequestMethod.POST, path = "/api/signals")
	public ResponseEntity<?> newSignal(@Valid @RequestBody final Signal newSignal, @RequestParam("closeMissingSignals") final boolean closeMissingSignals,
			@RequestParam("openNewTrades") final boolean openNewTrades) {
		logger.info("New incoming signal: " + newSignal + ". Close signals: " + closeMissingSignals + ", open new trades: " + openNewTrades);
		final UpdateOperationResult result = signalService.update(newSignal.getProviderName(), Lists.newArrayList(newSignal), openNewTrades,
				closeMissingSignals);
		if (result.getOpened() > 0) {
			return new ResponseEntity<>(HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(HttpStatus.OK);
		}

	}

}
