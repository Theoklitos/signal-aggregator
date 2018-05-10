package com.quantbro.aggregator.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.Fixtures;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.domain.SignalStatus;
import com.quantbro.aggregator.domain.Trade;
import com.quantbro.aggregator.domain.TradeStatus;
import com.quantbro.aggregator.trading.ForexClient;
import com.quantbro.aggregator.trading.RemoteTransaction;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTradeCloseReason;

@RunWith(SpringRunner.class)
public final class SignalEvaluatorServiceUnitTest {

	@Mock
	private TradingService tradingService;

	@Mock
	private ForexClient client;

	@InjectMocks
	private TradeSynchronizationService signalEvaluatorService;

	@Test
	public void ifATradesHasItsSignalClosedThenItShouldCloseToo() {
		final Trade openTrade = Fixtures.createOpenTrade(SignalProviderName.FXLEADERS, "666");
		final RemoteTransaction rti = Fixtures.createOpenRti("666");
		rti.setCloseReason(RemoteTradeCloseReason.ORDER);
		final Signal signal = Fixtures.createSignalForProviderWithStatus(SignalProviderName.FXLEADERS, SignalStatus.CLOSED);
		openTrade.setSignal(signal);
		signal.setTrade(openTrade);

		when(client.getAllRemoteTransactions("123")).thenReturn(Lists.newArrayList(rti));
		when(tradingService.getTradeByRemoteId("666")).thenReturn(openTrade);
		when(tradingService.getTradesByStatus(TradeStatus.OPENED)).thenReturn(Lists.newArrayList(openTrade));
		final UpdateOperationResult result = signalEvaluatorService.synchronizeTrades();

		assertEquals(1, result.getClosed());
	}

	@Test
	public void tradesThatAreNotInTheListOfSignalsShouldBeClosed() {
		final Trade openTrade = Fixtures.createOpenTrade(SignalProviderName.FXLEADERS, "1");
		final RemoteTransaction closedRti = Fixtures.createClosedRti(RemoteTradeCloseReason.ORDER, "1");
		closedRti.setPl(BigDecimal.valueOf(666.666));
		final Trade openTrade2 = Fixtures.createOpenTrade(SignalProviderName.FXLEADERS, "2");
		final RemoteTransaction openRti = Fixtures.createOpenRti("2");

		when(client.getAllRemoteTransactions("123")).thenReturn(Lists.newArrayList(closedRti, openRti));
		when(tradingService.getTradesByStatus(TradeStatus.OPENED)).thenReturn(Lists.newArrayList(openTrade, openTrade2));
		final UpdateOperationResult result = signalEvaluatorService.synchronizeTrades();

		assertEquals(1, result.getClosed());
		// assertEquals(666.666, openTrade.getPl(), 0.0001);
		Assert.fail();// test this
	}

}
