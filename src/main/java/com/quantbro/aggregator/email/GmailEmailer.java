package com.quantbro.aggregator.email;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.quantbro.aggregator.adapters.ScrapingException;
import com.quantbro.aggregator.domain.Aggregation;
import com.quantbro.aggregator.domain.Instrument;
import com.quantbro.aggregator.domain.Side;
import com.quantbro.aggregator.domain.Signal;
import com.quantbro.aggregator.utils.StringUtils;

public class GmailEmailer implements Emailer {

	private static final Logger logger = LoggerFactory.getLogger(GmailEmailer.class);

	@Value("${email.account}")
	private String username;

	@Value("${email.password}")
	private String password;

	@Value("${email.recipients}")
	private String recipients;

	@Override
	public void escalate(final String title, final String body, final Optional<Throwable> t) {
		final String actualTitle = "[ERROR] " + title;
		final String actualBody = t.isPresent() ? body + "\nRelevant stacktrace:\n<pre>" + ExceptionUtils.getStackTrace(t.get()) + "</pre>" : body;
		sendEmail(actualTitle, actualBody);

	}

	@Override
	public void escalate(final String title, final String body, final ScrapingException e) {
		final String actualTitle = "[SCRAPING ERROR] " + title;
		final String actualBody = body + "\nRelevant stacktrace:\n<pre>" + ExceptionUtils.getStackTrace(e) + "</pre>";
		sendEmailInternal(actualTitle, actualBody, e.getScreenshot());
	}

	public String getSignalAsConciseString(final Signal signal) {
		String entryPriceString = ".";
		if (signal.getEntryPrice().isPresent()) {
			entryPriceString = ". Preferred entry price is " + signal.getEntryPrice().get().intValue() + ",";
		}
		return "<strong>" + signal.getSide() + "</strong> for " + signal.getInstrument().toString() + " with SL(" + signal.getStopLoss() + ") and TP("
				+ signal.getTakeProfit() + ")" + entryPriceString;
	}

	@Override
	public void informOfNewAggregation(final Aggregation aggregation) {
		final Instrument instrument = aggregation.getInstrument();
		final Side side = aggregation.getSide();
		final List<Signal> signals = aggregation.getSignals();
		final String title = "New signal aggregation for " + side + " " + instrument + "!";
		String totalRankString = "";

		try {
			totalRankString = (aggregation.getTotalRank() == null) ? "" : " with a total rank of <strong>" + aggregation.getTotalRank() + "</strong> ";
		} catch (final NoSuchElementException e) {
			// hack. fix sometime
		}
		final StringBuffer bodyBuffer = new StringBuffer("Detected an aggregation " + totalRankString + "for <strong>" + side + " " + instrument
				+ "</strong> at <strong>" + StringUtils.getReadableDateTime(aggregation.getDetectionDate()) + "</strong> with " + signals.size() + " signals:");

		bodyBuffer.append("<ul>");
		signals.forEach(signal -> {
			final String entryPrice = signal.getEntryPrice().isPresent() ? ", entry price: " + signal.getEntryPrice().get().stripTrailingZeros().toPlainString()
					: "";
			final String rankingString = (aggregation.getRankForProviderOfSignal(signal) == null) ? ""
					: "Provider rank: " + aggregation.getRankForProviderOfSignal(signal).toPlainString();
			bodyBuffer.append("<li>" + signal.getProviderName() + " at " + StringUtils.getReadableDateTime(signal.getStartDate()) + entryPrice + ", SL: "
					+ signal.getStopLoss().stripTrailingZeros().toPlainString() + ", TP: " + signal.getTakeProfit().stripTrailingZeros().toPlainString() + ". "
					+ rankingString + "</li>");
		});

		bodyBuffer.append("</ul>");
		sendEmail(title, bodyBuffer.toString());
	}

	@Override
	public void informOfNewSignalAndItsTrade(final Signal newSignal) {
		if (newSignal.getTrade() == null) {
			throw new IllegalArgumentException("No point in informing for a new signal if it doesn't have a trade attached to it.");
		}
		final String providerName = newSignal.getProviderName().toString();
		final String title = "New signal from " + providerName + "!";
		final String prettySignalString = getSignalAsConciseString(newSignal);
		final String body = "Our friends at " + providerName + " just sent this signal: " + prettySignalString + "\nA new transaction with ID <strong>"
				+ newSignal.getTrade().getRemoteId() + "</strong> has been created via our remote broker. Use this ID to monitor the trade/order yourself.";
		sendEmail(title, body);
	}

	@Override
	public void sendEmail(final String title, final String body) {
		sendEmailInternal(title, body, Optional.empty());
	}

	private void sendEmailInternal(final String title, final String body, final Optional<byte[]> imageByteArray) {
		final Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		final Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			final MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
			message.setSubject("[SIGNAL AGGREGATOR] " + title);
			final String withHtmlNewlines = body.replaceAll("(\r\n|\n)", "<br /><br />");
			message.setText("<html><body>" + withHtmlNewlines + "</body></html>", "UTF-8", "html");

			if (imageByteArray.isPresent()) {
				final MimeMultipart multipart = new MimeMultipart("related");
				BodyPart messageBodyPart = new MimeBodyPart();
				final String htmlText = "<img src=\"cid:image\">";
				messageBodyPart.setContent(htmlText, "text/html");
				multipart.addBodyPart(messageBodyPart);

				messageBodyPart = new MimeBodyPart();
				final DataSource fds = new ByteArrayDataSource(imageByteArray.get(), "image/jpg");
				messageBodyPart.setDataHandler(new DataHandler(fds));
				messageBodyPart.setHeader("Content-ID", "<image>");
				multipart.addBodyPart(messageBodyPart);
				message.setContent(multipart);
			}

			Transport.send(message);
		} catch (final Exception e) {
			logger.error("Could not send email: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
