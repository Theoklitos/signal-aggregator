package com.quantbro.aggregator.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.adapters.SignalProviderName;

@RunWith(SpringRunner.class)
public final class TradeUnitTest {

	private Trade trade;

	@Test
	public void endTrade() {
		final BigDecimal pl = BigDecimal.valueOf(-33);
		final BigDecimal closingPrice = BigDecimal.valueOf(1.321);
		final TradeStatus status = TradeStatus.CLOSED_BY_ORDER;
		trade.endTrade(status, closingPrice, pl);
		assertEquals(status, trade.getStatus());
		assertEquals(pl, trade.getPl());
		assertEquals(closingPrice, trade.getClosingPrice());
		assertNotNull(trade.getEndDate());
	}

	@Test
	public void endTradeWithStatusAndMessage() {
		final TradeStatus status = TradeStatus.ERROR;
		final String message = "error!";
		trade.endTradeWithStatusAndMessage(status, message);
		assertEquals(status, trade.getStatus());
		assertEquals(message, trade.getMessage());
		assertNotNull(trade.getEndDate());
	}

	@Test
	public void newTradeHasInitializedStatus() {
		trade.getStatus().equals(TradeStatus.INITIALIZED);
	}

	@Before
	public void setup() {
		trade = new Trade(SignalProviderName.FORESIGNAL);
	}

}
