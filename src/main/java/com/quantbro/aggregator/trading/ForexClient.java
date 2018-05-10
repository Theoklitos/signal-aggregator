package com.quantbro.aggregator.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.trading.RemoteTransaction.RemoteTransactionStatus;

/**
 * connects to some forex API to perform actual trades
 */
public interface ForexClient {

	/**
	 * @return information about the trade that was just closed
	 */
	RemoteTransaction closeTrade(final String accountId, final String remoteId) throws TradingException;

	/**
	 * Will return everything the broke has. This may perform more than one call, so beware.
	 */
	List<RemoteTransaction> getAllRemoteTransactions(final String accountId) throws TradingException;

	/**
	 * returns all the open trades from the trader's API
	 */
	List<RemoteTransaction> getAllRemoteTransactionsForStatus(final String accountId, final RemoteTransactionStatus status) throws TradingException;

	Price getCurrentPrice(final String accountId, Instrument instrument);

	/**
	 * returns information about a single trade from the trader's API
	 */
	RemoteTransaction getRemoteTransaction(final String accountId, final String remoteId, final RemoteTransactionStatus status)
			throws TradeDoesNotExistException, TradingException;

	/**
	 * @param entryPrice
	 *            if no entry price is provided, trade will open immediately (market order)
	 */
	RemoteTransaction openTrade(final String accountId, final int units, final Instrument instrument, final Side side, final Optional<BigDecimal> entryPrice,
			final BigDecimal stopLoss, final BigDecimal takeProfit) throws TradingException;

}