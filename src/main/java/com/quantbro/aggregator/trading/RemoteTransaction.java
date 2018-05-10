package com.quantbro.aggregator.trading;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;

/**
 * A wrapper around information received directly from the remote trader's API.
 */
public final class RemoteTransaction {

	public enum RemoteTradeCloseReason {
		/**
		 * An explicit order (via the API or manually) was issued to close this trade.
		 */
		ORDER,

		/**
		 * The stoploss level was reached
		 */
		STOPLOSS,

		/**
		 * the takeprofit level was reached
		 */
		TAKEPROFIT;
	}

	public enum RemoteTransactionStatus {
		CLOSED, CANCELLED, OPEN, PENDING, FILLED;
	}

	private final JSONObject json;
	private String id;
	private String relatedId;
	private BigDecimal entryPrice;
	private BigDecimal closingPrice;
	private BigDecimal pl;
	private RemoteTransactionStatus status;
	private RemoteTradeCloseReason closeReason;
	private DateTime closeDateTime;
	private String accountId;

	public RemoteTransaction(final JSONObject json) {
		this.json = json;
	}

	public String getAccountId() {
		return accountId;
	}

	public DateTime getCloseDateTime() {
		return closeDateTime;
	}

	public RemoteTradeCloseReason getCloseReason() {
		return closeReason;
	}

	public BigDecimal getClosingPrice() {
		return closingPrice;
	}

	public String getId() {
		return id;
	}

	public JSONObject getJsonObject() {
		return json;
	}

	public BigDecimal getPl() {
		return pl;
	}

	public String getPrettyJson() {
		if (json != null) {
			return json.toString(5);
		}
		return "";
	}

	/**
	 * returns the price of the instrument for this trade. if the trade is closed, this will not change anymore.
	 */
	public BigDecimal getPrice() {
		return entryPrice;
	}

	public String getRelatedId() {
		return relatedId;
	}

	public RemoteTransactionStatus getStatus() {
		return status;
	}

	/**
	 * the (remote) account id that this transaction was made for
	 */
	public void setAccountId(final String accountId) {
		this.accountId = accountId;
	}

	public void setCloseDateTime(final DateTime closeDateTime) {
		this.closeDateTime = closeDateTime;
	}

	public void setCloseReason(final RemoteTradeCloseReason closeReason) {
		this.closeReason = closeReason;
	}

	public void setClosingPrice(final BigDecimal closingPrice) {
		this.closingPrice = closingPrice;
	}

	public void setEntryPrice(final BigDecimal entryPrice) {
		this.entryPrice = entryPrice;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setPl(final BigDecimal pl) {
		this.pl = pl;
	}

	public void setRelatedId(final String relatedId) {
		this.relatedId = relatedId;
	}

	public void setStatus(final RemoteTransactionStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		final StringBuffer info = new StringBuffer();
		if (status == null || StringUtils.isBlank(id)) {
			info.append("Uninitialized remote transaction");
		} else {
			info.append("[" + id + "] " + status);
			if (status.equals(RemoteTransactionStatus.OPEN) || status.equals(RemoteTransactionStatus.PENDING)) {
				info.append(", starting/target price: " + getPrice());
			} else {
				info.append(", starting price: " + getPrice() + ", close price: " + getClosingPrice().toPlainString());
			}
			if (status.equals(RemoteTransactionStatus.CLOSED)) {
				info.append(", PL: " + getPl().toPlainString());
			}
			if (StringUtils.isNotBlank(relatedId)) {
				info.append(", related transaction ID: " + getRelatedId());
			}
		}
		return info.toString();
	}

}
