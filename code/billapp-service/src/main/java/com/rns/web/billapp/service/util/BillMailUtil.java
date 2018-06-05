package com.rns.web.billapp.service.util;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.domain.BillInvoice;

public class BillMailUtil implements BillConstants, Runnable {

	private static final String READ_RECEIPT_MAIL = "talnoterns@gmail.com";

	private static final String MAIL_HOST = "smtp.gmail.com";
	private static final String MAIL_ID = "visionlaturpattern@gmail.com";
	private static final String MAIL_PASSWORD = "Vision2018!";
	
	private static final String MAIL_AUTH = "true";
	private static final String MAIL_PORT = "587";

	private String type;
	private BillUser user;
	private List<String> users;
	private String messageText;
	private String mailSubject;
	private BillInvoice invoice;

	public void setUser(BillUser user) {
		this.user = user;
	}

	public void setInvoice(BillInvoice invoice) {
		this.invoice = invoice;
	}

	public BillMailUtil(String mailType) {
		this.type = mailType;
	}

	public BillMailUtil() {

	}

	public void sendMail() {

		Session session = prepareMailSession();

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(MAIL_ID, "Pay Per Bill"));
			prepareMailContent(message);
			Transport.send(message);
			LoggingUtil.logMessage("Mail sent to .." + user.getEmail());
		} catch (MessagingException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} catch (UnsupportedEncodingException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
	}

	private static Session prepareMailSession() {
		Properties props = new Properties();

		props.put("mail.smtp.auth", MAIL_AUTH);
		props.put("mail.smtp.socketFactory.port", "465"); //PROD
		props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory"); //PROD
		//props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", MAIL_HOST);
		props.put("mail.smtp.port", MAIL_PORT);

		LoggingUtil.logMessage("Mail credentials being used .." + MAIL_ID + " -- " + MAIL_PASSWORD);
		
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(MAIL_ID, MAIL_PASSWORD);
			}
		});
		return session;
	}

	private String prepareMailContent(Message message) throws UnsupportedEncodingException {

		try {
			// boolean attachCv = false;
			String result = readMailContent(message);
			String subject = message.getSubject();
			BillBusiness currentBusiness = user.getCurrentBusiness();
			if (user != null) {
				result = StringUtils.replace(result, "{name}", CommonUtils.getStringValue(user.getName()));
				result = StringUtils.replace(result, "{email}", CommonUtils.getStringValue(user.getEmail()));
				result = StringUtils.replace(result, "{phone}", CommonUtils.getStringValue(user.getPhone()));
				if (currentBusiness != null) {
					result = StringUtils.replace(result, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
				}
				subject = StringUtils.replace(subject, "{name}", CommonUtils.getStringValue(user.getName()));
			}

			if (invoice != null) {
				result = StringUtils.replace(result, "{invoiceId}", CommonUtils.getStringValue(invoice.getId()));
				result = StringUtils.replace(result, "{month}", BillConstants.MONTHS[invoice.getMonth() - 1]);
				result = StringUtils.replace(result, "{year}", CommonUtils.getStringValue(invoice.getYear()));
				result = StringUtils.replace(result, "{amount}", CommonUtils.getStringValue(invoice.getAmount()));
				result = StringUtils.replace(result, "{serviceCharge}", CommonUtils.getStringValue(invoice.getServiceCharge()));
				result = StringUtils.replace(result, "{pending}", CommonUtils.getStringValue(invoice.getPendingBalance()));
				result = StringUtils.replace(result, "{credit}", CommonUtils.getStringValue(invoice.getCreditBalance()));
				result = StringUtils.replace(result, "{internetFees}", CommonUtils.getStringValue(invoice.getInternetFees()));
				result = StringUtils.replace(result, "{payable}", CommonUtils.getStringValue(invoice.getPayable()));
				result = StringUtils.replace(result, "{createdDate}", CommonUtils.convertDate(invoice.getCreatedDate()));
				result = StringUtils.replace(result, "{paymentUrl}", CommonUtils.getStringValue(invoice.getPaymentUrl()));

				subject = StringUtils.replace(subject, "{month}", BillConstants.MONTHS[invoice.getMonth() - 1]);
				subject = StringUtils.replace(subject, "{year}", CommonUtils.getStringValue(invoice.getYear()));
				subject = StringUtils.replace(subject, "{amount}", CommonUtils.getStringValue(invoice.getAmount()));
				
				if(CollectionUtils.isNotEmpty(invoice.getInvoiceItems())) {
					String invoiceItemsTemplate = CommonUtils.readFile("email/invoice_items.html");
					StringBuilder builder = new StringBuilder();
					for(BillItem invoiceItem: invoice.getInvoiceItems()) {
						String invoiceItemRow = StringUtils.replace(invoiceItemsTemplate, "{amount}", CommonUtils.getStringValue(invoiceItem.getPrice()));
						invoiceItemRow = StringUtils.replace(invoiceItemRow, "{quantity}", CommonUtils.getStringValue(invoiceItem.getQuantity()));
						if(invoiceItem.getParentItem() != null) {
							invoiceItemRow = StringUtils.replace(invoiceItemRow, "{name}", CommonUtils.getStringValue(invoiceItem.getParentItem().getName()));
						} else {
							invoiceItemRow = StringUtils.replace(invoiceItemRow, "{name}", CommonUtils.getStringValue(invoiceItem.getName()));
						}
						builder.append(invoiceItemRow);
					}
					result = StringUtils.replace(result, "{invoiceItems}", builder.toString());
				}
				
				String businessName = "";
				if (currentBusiness != null) {
					 businessName = CommonUtils.getStringValue(currentBusiness.getName());
				}
				
				if(StringUtils.equals(BillConstants.PAYMENT_STATUS_CREDIT, invoice.getStatus())) {
					result = StringUtils.replace(result, "{status}", "Successful");	
					result = StringUtils.replace(result, "{message}", "This bill payment for " + BillPaymentUtil.invoicePurpose(invoice) + " to vendor " + businessName + " is successful.");
					subject = StringUtils.replace(subject, "{status}", "successful");
				} else {
					result = StringUtils.replace(result, "{status}", "Failed");	
					result = StringUtils.replace(result, "{message}", "This bill payment for " + BillPaymentUtil.invoicePurpose(invoice) + " to vendor " + businessName + " is failed. "
							+ "Please contact our customer support with given reference Payment ID for more info.");
					subject = StringUtils.replace(subject, "{status}", "failed");
				}

				result = StringUtils.replace(result, "{paidAmount}", CommonUtils.getStringValue(invoice.getPaidAmount()));
				result = StringUtils.replace(result, "{paymentId}", CommonUtils.getStringValue(invoice.getPaymentId()));
				
			}

			if (currentBusiness != null) {
				result = StringUtils.replace(result, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
				subject = StringUtils.replace(subject, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
			}

			if (StringUtils.isNotBlank(messageText)) {
				result = StringUtils.replace(result, "{message}", messageText);
			}
			// message.setContent(result, "text/html");
			message.setContent(result, "text/html; charset=utf-8");
			/*
			 * if(StringUtils.contains(type, "Admin")) {
			 * message.setRecipients(Message.RecipientType.TO,
			 * InternetAddress.parse(getEmails(Arrays.asList(ADMIN_MAILS)))); }
			 * else if(CollectionUtils.isNotEmpty(users)) {
			 * message.setRecipients(Message.RecipientType.TO,
			 * InternetAddress.parse("talnoterns@gmail.com"));
			 * message.setRecipients(Message.RecipientType.BCC,
			 * InternetAddress.parse(getEmails(users))); } else {
			 */
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
			// }

			message.setSubject(subject);
			return result;

		} catch (FileNotFoundException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} catch (MessagingException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} catch (IllegalStateException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return "";
	}

	private String getEmails(List<String> users) {
		if (CollectionUtils.isEmpty(users)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String user : users) {
			if (StringUtils.isEmpty(user)) {
				continue;
			}
			builder.append(user).append(",");
		}
		return StringUtils.removeEnd(builder.toString(), ",");
	}

	public void run() {
		sendMail();
	}

	/*
	 * private String prepareActivationMailContent() { StringBuilder builder =
	 * new StringBuilder();
	 * builder.append(ROOT_URL_ACTIVATION).append("#?").append(
	 * ACTIVATION_URL_VAR).append("=").append(candidate.getActivationCode()).
	 * append("&").append(ACTIVATION_USER_VAR).append("=")
	 * .append(candidate.getEmail()); return builder.toString(); }
	 */

	private String readMailContent(Message message) throws FileNotFoundException, MessagingException {
		String contentPath = "";
		contentPath = "email/" + MAIL_TEMPLATES.get(type);
		String subject = MAIL_SUBJECTS.get(type);
		message.setSubject(subject);
		return CommonUtils.readFile(contentPath);
	}

	/*
	 * private void attachCv(Message message, Candidate candidate, String
	 * result) throws MessagingException, IOException { Multipart mp = new
	 * MimeMultipart(); BodyPart fileBody = new MimeBodyPart(); DataSource
	 * source = new FileDataSource(candidate.getResume());
	 * fileBody.setDataHandler(new DataHandler(source));
	 * fileBody.setFileName(candidate.getFilePath()); BodyPart messsageBody =
	 * new MimeBodyPart(); messsageBody.setText(result);
	 * messsageBody.setContent(result, "text/html"); mp.addBodyPart(fileBody);
	 * mp.addBodyPart(messsageBody); message.setContent(mp); }
	 */

	public void setUsers(List<String> users) {
		this.users = users;
	}

	public String getMailSubject() {
		return mailSubject;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	private static Map<String, String> MAIL_TEMPLATES = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(MAIL_TYPE_INVOICE, "invoice.html");
			put(MAIL_TYPE_PAYMENT_RESULT, "payment_result.html");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "payment_result.html");
		}
	});

	private static Map<String, String> MAIL_SUBJECTS = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(MAIL_TYPE_INVOICE, "Your Invoice for {month} {year} of Rs. {amount} is generated by {businessName}");
			put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {amount} to {businessName} is {status}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "Your Bill payment for {month} {year} of Rs. {amount} by {name} is {status}");
		}
	});

}
