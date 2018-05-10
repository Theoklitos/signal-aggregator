package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.dao.TradeRepository;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.email.Emailer;
import com.quantbro.aggregator.trading.ForexClient;
import com.quantbro.aggregator.trading.RemoteTransaction;
import com.quantbro.aggregator.trading.TradingException;

@RunWith(SpringRunner.class)
public final class TradingServiceUnitTest {

	private static final int UNITS = 1000;

	@Mock
	private ForexClient forexClient;

	@Mock
	private TradeRepository repository;

	@Mock
	private Emailer emailer;

	@InjectMocks
	private TradingService service;

	@Test
	public void emailSentOutIfAnErrorWhenOpeningTradeOccurs() {
		final Signal signal = new Signal(SignalProviderName.FORESIGNAL);
		final TradingException exception = new TradingException("oops");

		when(forexClient.openTrade(anyString(), anyInt(), any(), any(), any(), any(), any())).thenThrow(exception);
		service.openTradeForSignal(signal);

		verify(emailer, times(1)).escalate(anyString(), anyString(), Optional.of(any()));
		verify(repository, times(1)).save(any(Trade.class));
	}

	@Test
	public void errorWhenClosingTrade() {
		final Trade trade = new Trade();
		trade.setRemoteId("666");

		when(forexClient.closeTrade("123", trade.getRemoteId())).thenThrow(new TradingException(""));
		service.closeTrade(trade, TradeStatus.CLOSED_BY_STOPLOSS);

		assertEquals(TradeStatus.ERROR, trade.getStatus());
	}

	@Before
	public void setup() {
		service.setUnitsToTrade(UNITS);
	}

	@Test
	public void succesfullyCloseTrade() {
		final TradeStatus reasonForClosing = TradeStatus.CLOSED_BY_ORDER;
		final Trade trade = new Trade();
		trade.setRemoteId("666");
		final RemoteTransaction response = new RemoteTransaction(null);
		response.setPl(BigDecimal.valueOf(666.666));

		when(forexClient.closeTrade("123", trade.getRemoteId())).thenReturn(response);
		service.closeTrade(trade, reasonForClosing);

		assertEquals(reasonForClosing, trade.getStatus());
		assertNotNull(trade.getEndDate());
	}

	@Test
	public void succesfullyOpenTrade() {
		final String remoteId = "remoteId";
		final SignalProviderName providerName = SignalProviderName.FORESIGNAL;
		final Instrument instrument = Instrument.AUD_USD;
		final Side side = Side.SELL;
		final BigDecimal stopLoss = BigDecimal.valueOf(1.1);
		final BigDecimal takeProfit = BigDecimal.valueOf(1.2);
		final BigDecimal entryPrice = BigDecimal.valueOf(1.3);
		final Signal signal = new Signal(providerName, instrument, side, stopLoss, takeProfit, Optional.of(entryPrice));
		final RemoteTransaction rti = new RemoteTransaction(null);
		rti.setId(remoteId);

		when(forexClient.openTrade("123", service.getUnitsToTrade(), instrument, side, Optional.of(entryPrice), stopLoss, takeProfit)).thenReturn(rti);
		service.openTradeForSignal(signal);

		verify(repository, times(1)).save(any(Trade.class));
	}

}
