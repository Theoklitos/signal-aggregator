package com.quantbro.aggregator.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantbro.aggregator.adapters.SignalProviderName;
import com.quantbro.aggregator.domain.validation.PositiveNumber;
import com.quantbro.aggregator.serialization.TradeSerializer;
import com.quantbro.aggregator.trading.TradingException;
import com.quantbro.aggregator.utils.StringUtils;

/**
 * Represents a trade that has been opened via an external broker API, i.e. OANDA
 */
@Entity
@Table(name = "trades")
@JsonSerialize(using = TradeSerializer.class)
public class Trade extends AbstractTimer {

	private static final Logger logger = LoggerFactory.getLogger(Trade.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	/**
	 * The id that the broker has assigned to this trade
	 */
	private String remoteId;

	@NotNull
	@Enumerated(EnumType.STRING)
	private TradeStatus status;

	@NotNull
	@Enumerated(EnumType.STRING)
	private SignalProviderName providerName;

	/**
	 * Stores information about this trade's state. Usually some kind of error message.
	 */
	@Column(columnDefinition = "mediumtext")
	private String message;

	/**
	 * Json we might have received from the external broker, when tracking this trade. The context of this json would be the signal's status.
	 */
	@Column(columnDefinition = "mediumtext")
	private String json;

	@OneToOne(mappedBy = "trade")
	private Signal signal;

	@Digits(integer = 9, fraction = 5)
	private BigDecimal pl;

	@Digits(integer = 9, fraction = 5)
	@PositiveNumber(allowNull = true)
	private BigDecimal entryPrice;

	@Digits(integer = 9, fraction = 5)
	@PositiveNumber(allowNull = true)
	private BigDecimal closingPrice;

	public Trade() {

	}

	public Trade(final SignalProviderName providerName) {
		this.providerName = providerName;
		this.status = TradeStatus.INITIALIZED;
	}

	public void cancelTrade() {
		end();
		setStatus(TradeStatus.CANCELLED);
	}

	public void endTrade(final TradeStatus status, final BigDecimal closingPrice, final BigDecimal pl) {
		end();
		setStatus(status);
		setClosingPrice(closingPrice);
		setPL(pl);
		logger.debug("=========================== DEBUG LINE. REMOVE WHEN FIXED =========================================================");
		logger.debug("Closed trade with status " + status + ", closing price " + closingPrice + " and pl " + pl);
		logger.debug("===================================================================================================================");
	}

	public void endTradeForException(final TradingException e) {
		end();
		setStatus(TradeStatus.ERROR);
		setMessage(e.getMessage());
		setJson(e.getPrettyJsonOfError());
	}

	public void endTradeWithStatusAndMessage(final TradeStatus status, final String message) {
		end();
		setStatus(status);
		setMessage(message);
	}

	public BigDecimal getClosingPrice() {
		return closingPrice;
	}

	public BigDecimal getEntryPrice() {
		return entryPrice;
	}

	public long getId() {
		return id;
	}

	public String getJson() {
		return json;
	}

	public String getMessage() {
		return message;
	}

	public BigDecimal getPl() {
		return pl;
	}

	public SignalProviderName getProviderName() {
		return providerName;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public Signal getSignal() {
		return signal;
	}

	public TradeStatus getStatus() {
		return status;
	}

	public boolean hasEntryPrice() {
		return !entryPrice.equals(BigDecimal.ZERO);
	}

	public void setClosingPrice(final BigDecimal closingPrice) {
		BigDecimal roundedClosingPrice = closingPrice;
		if (closingPrice != null) {
			roundedClosingPrice = closingPrice.setScale(5, RoundingMode.UP);
		}
		this.closingPrice = roundedClosingPrice;
	}

	public void setEntryPrice(final BigDecimal entryPrice) {
		this.entryPrice = entryPrice;
	}

	public void setJson(final String jsonString) {
		this.json = jsonString;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public void setPL(final BigDecimal pl) {
		BigDecimal roundedPl = pl;
		if (pl != null) {
			roundedPl = pl.setScale(5, RoundingMode.UP);
		}
		this.pl = roundedPl;
	}

	public void setProviderName(final SignalProviderName providerName) {
		this.providerName = providerName;
	}

	public void setRemoteId(final String remoteId) {
		this.remoteId = remoteId;
	}

	public void setSignal(final Signal signal) {
		this.signal = signal;
	}

	public void setStatus(final TradeStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		final String readableStartDate = StringUtils.getReadableDateTime(getStartDate());
		final String dateString = (getEndDate() == null) ? readableStartDate : readableStartDate + " to " + StringUtils.getReadableDateTime(getEndDate());
		final String message = StringUtils.isBlank(this.message) ? "" : ". Message: " + this.message;
		final String entryPrice = (getEntryPrice() == null || getEntryPrice().equals(BigDecimal.ZERO)) ? ""
				: ", entry price: " + getEntryPrice().toPlainString();
		final String signalId = (this.getSignal() == null) ? "" : ", parent signal ID: " + this.signal.getId();
		return "[" + remoteId + "] " + status + ", " + dateString + entryPrice + message + signalId;
	}

}