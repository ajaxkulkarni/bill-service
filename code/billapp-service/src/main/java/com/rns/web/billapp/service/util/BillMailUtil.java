package com.rns.web.billapp.service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;

public class BillMailUtil implements BillConstants, Runnable {

	private static final String READ_RECEIPT_MAIL = "talnoterns@gmail.com";

	private static final String MAIL_HOST = "smtpout.asia.secureserver.net";// "smtp.gmail.com";
	private static final String MAIL_ID = "help@payperbill.in";// "visionlaturpattern@gmail.com";
	private static final String MAIL_PASSWORD = "WickedSmile2@"; // "Vision2018!";

	private static final String MAIL_AUTH = "true";
	private static final String MAIL_PORT = "25";// "587";

	private static final String[] ADMIN_MAILS = { "ajinkyashiva@gmail.com, mcm.abhishek@gmail.com, help@payperbill.in, rssplsocial@gmail.com" };

	private String type;
	private BillUser user;
	private List<BillUser> users;
	private String messageText;
	private String mailSubject;
	private BillInvoice invoice;
	private List<BillInvoice> invoices;
	private boolean copyAdmins;
	private BillScheme selectedScheme;
	private BillUser customerInfo;

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
	
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public BillMailUtil(String mailType, BillUser user2) {
		this.type = mailType;
		this.user = user2;
	}

	public void sendMail() {

		if (user == null || (!isAdminMail() && !genericMail() && StringUtils.isBlank(user.getEmail()))) {
			return;
		}

		Session session = prepareMailSession();

		try {
			LoggingUtil.logMessage("Sending mail to .." + user.getEmail());
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
		props.put("mail.smtp.socketFactory.port", "465"); // PROD
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // PROD
		// props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", MAIL_HOST);
		props.put("mail.smtp.port", MAIL_PORT);

		LoggingUtil.logMessage("Mail credentials being used .." + MAIL_ID);

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
				result = prepareUserInfo(result, user);

				if (currentBusiness != null) {
					subject = StringUtils.replace(subject, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
					if(selectedScheme != null) {
						result = prepareSchemeInfo(result, selectedScheme, currentBusiness);
						subject = StringUtils.replace(subject, "{schemeName}", CommonUtils.getStringValue(selectedScheme.getSchemeName()));
					}
				}
				subject = StringUtils.replace(subject, "{name}", CommonUtils.getStringValue(user.getName()));
			}
			
			if(customerInfo != null) {
				result = prepareCustomerInfo(result, customerInfo);
			}
			
			if (invoice != null) {
				result = prepareInvoiceInfo(result, invoice);

				if (invoice.getMonth() != null) {
					subject = StringUtils.replace(subject, "{month}", BillConstants.MONTHS[invoice.getMonth() - 1]);
				} else {
					subject = StringUtils.replace(subject, "{month}", CommonUtils.convertDate(invoice.getCreatedDate(), DATE_FORMAT_DISPLAY_NO_YEAR));
				}
				subject = StringUtils.replace(subject, "{year}", CommonUtils.getStringValue(invoice.getYear()));
				subject = StringUtils.replace(subject, "{amount}", CommonUtils.getStringValue(invoice.getPayable(), false));
				subject = StringUtils.replace(subject, "{date}", CommonUtils.convertDate(invoice.getPaidDate(), DATE_FORMAT_DISPLAY_NO_YEAR));

				result = appendInvoiceItems(result);

				result = appendCustomers(result);

				String businessName = "";
				if (currentBusiness != null) {
					businessName = CommonUtils.getStringValue(currentBusiness.getName());
				}

				if (StringUtils.equals(BillConstants.INVOICE_STATUS_PAID, invoice.getStatus())) {
					result = StringUtils.replace(result, "{status}", "Successful");
					result = StringUtils.replace(result, "{message}",
							"This bill payment for " + BillPaymentUtil.invoicePurpose(invoice) + " to vendor " + businessName + " is successful.");
					subject = StringUtils.replace(subject, "{status}", "successful");
				} else {
					result = StringUtils.replace(result, "{status}", "Failed");
					result = StringUtils.replace(result, "{message}", "This bill payment for " + BillPaymentUtil.invoicePurpose(invoice) + " to vendor "
							+ businessName + " is failed. " + "Please contact our customer support with given reference Payment ID for more info.");
					subject = StringUtils.replace(subject, "{status}", "failed");
				}

			}

			if (StringUtils.isNotBlank(messageText)) {
				result = StringUtils.replace(result, "{message}", messageText);
			}

			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(result, "text/html; charset=utf-8");
			multipart.addBodyPart(messageBodyPart);
			/*
			 * BodyPart image = new MimeBodyPart(); DataSource fds = new
			 * FileDataSource(BillMailUtil.class.getClassLoader().getResource(
			 * "email/PayPerBill.png").getPath()); image.setDataHandler(new
			 * DataHandler(fds)); image.setHeader("Content-ID", "<image>");
			 * multipart.addBodyPart(image);
			 */

			message.setContent(result, "text/html; charset=utf-8");
			// message.setContent(result, "text/html; charset=utf-8");

			if (isAdminMail()) {
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(getEmails(Arrays.asList(ADMIN_MAILS))));
			} else if (genericMail()) {
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("rssplsocial@gmail.com"));
				message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(getUserEmails(users))); }
			else {
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
			}

			if (copyAdmins) {
				message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(getEmails(Arrays.asList(ADMIN_MAILS))));
			}

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

	private boolean genericMail() {
		return CollectionUtils.isNotEmpty(users) && StringUtils.equals(MAIL_TYPE_GENERIC, type);
	}

	public static String prepareCustomerInfo(String result, BillUser customerInfo) {
		result = StringUtils.replace(result, "{customerName}", CommonUtils.getStringValue(customerInfo.getName()));
		result = StringUtils.replace(result, "{customerEmail}", CommonUtils.getStringValue(customerInfo.getEmail()));
		result = StringUtils.replace(result, "{customerPhone}", CommonUtils.getStringValue(customerInfo.getPhone()));
		return result;
	}

	private String appendInvoiceItems(String result) throws FileNotFoundException {
		if (CollectionUtils.isNotEmpty(invoice.getInvoiceItems()) && BillRuleEngine.showBillDetails(user)) {
			String invoiceItemsTemplate = CommonUtils.readFile("email/invoice_items.html");
			StringBuilder builder = new StringBuilder();
			for (BillItem invoiceItem : invoice.getInvoiceItems()) {
				String invoiceItemRow = StringUtils.replace(invoiceItemsTemplate, "{amount}", CommonUtils.getStringValue(invoiceItem.getPrice(), false));
				invoiceItemRow = StringUtils.replace(invoiceItemRow, "{quantity}", CommonUtils.getStringValue(invoiceItem.getQuantity(), true));
				if (invoiceItem.getParentItem() != null) {
					invoiceItemRow = StringUtils.replace(invoiceItemRow, "{name}", CommonUtils.getStringValue(invoiceItem.getParentItem().getName()));
				} else {
					invoiceItemRow = StringUtils.replace(invoiceItemRow, "{name}", CommonUtils.getStringValue(invoiceItem.getName()));
				}
				builder.append(invoiceItemRow);
			}
			result = StringUtils.replace(result, "{invoiceItems}", builder.toString());
		} else {
			result = StringUtils.replace(result, "{invoiceItems}", "");
		}
		return result;
	}

	private String appendCustomers(String result) throws FileNotFoundException {
		if (CollectionUtils.isNotEmpty(users)) {
			String invoiceItemsTemplate = CommonUtils.readFile("email/invoices_list.html");
			StringBuilder builder = new StringBuilder();
			for (BillUser customer : users) {
				String row = new String(invoiceItemsTemplate);
				row = prepareUserInfo(row, customer);
				row = prepareInvoiceInfo(row, customer.getCurrentInvoice());
				builder.append(row);
			}
			result = StringUtils.replace(result, "{invoices}", builder.toString());
		} else {
			result = StringUtils.replace(result, "{invoices}", "");
		}
		return result;
	}

	private boolean isAdminMail() {
		return StringUtils.contains(type, "Admin");
	}

	public static String prepareInvoiceInfo(String result, BillInvoice invoice) {
		result = StringUtils.replace(result, "{invoiceId}", CommonUtils.getStringValue(invoice.getId()));
		if (invoice.getMonth() != null) {
			result = StringUtils.replace(result, "{month}", BillConstants.MONTHS[invoice.getMonth() - 1]);
		}
		if (StringUtils.equals(PAYMENT_MODE_REWARD, invoice.getPaymentMode())) {
			result = StringUtils.replace(result, "{month}", invoice.getComments());
		} else if (invoice.getMonth() == null) {
			result = StringUtils.replace(result, "{month}", CommonUtils.convertDate(invoice.getCreatedDate(), DATE_FORMAT_DISPLAY_NO_YEAR));
		}
		result = StringUtils.replace(result, "{year}", CommonUtils.getStringValue(invoice.getYear()));
		result = StringUtils.replace(result, "{amount}", CommonUtils.getStringValue(invoice.getAmount(), false));
		result = StringUtils.replace(result, "{serviceCharge}", CommonUtils.getStringValue(invoice.getServiceCharge(), false));
		result = StringUtils.replace(result, "{pending}", CommonUtils.getStringValue(invoice.getPendingBalance(), false));
		result = StringUtils.replace(result, "{credit}", CommonUtils.getStringValue(invoice.getCreditBalance(), false));
		result = StringUtils.replace(result, "{internetFees}", CommonUtils.getStringValue(invoice.getInternetFees(), false));
		result = StringUtils.replace(result, "{payable}", CommonUtils.getStringValue(invoice.getPayable(), false));
		result = StringUtils.replace(result, "{outstanding}", CommonUtils.getStringValue(invoice.getOutstandingBalance(), false));
		result = StringUtils.replace(result, "{createdDate}", CommonUtils.convertDate(invoice.getCreatedDate()));
		result = StringUtils.replace(result, "{paymentUrl}", CommonUtils.getStringValue(BillSMSUtil.shortenUrl(invoice.getPaymentUrl())));
		result = StringUtils.replace(result, "{paidAmount}", CommonUtils.getStringValue(invoice.getPaidAmount(), false));
		result = StringUtils.replace(result, "{paymentId}", CommonUtils.getStringValue(invoice.getPaymentId()));
		result = StringUtils.replace(result, "{paymentMode}", CommonUtils.getStringValue(invoice.getPaymentMode()));
		result = StringUtils.replace(result, "{settlementId}", CommonUtils.getStringValue(invoice.getPaymentId()));
		result = StringUtils.replace(result, "{settlementAmount}", CommonUtils.getStringValue(invoice.getPayable(), false));
		result = StringUtils.replace(result, "{date}", CommonUtils.convertDate(invoice.getPaidDate()));
		result = StringUtils.replace(result, "{transactionCharges}", CommonUtils.getStringValue(invoice.getTransactionCharges(), false));
		return result;
	}

	public static String prepareUserInfo(String result, BillUser user) {
		result = StringUtils.replace(result, "{name}", CommonUtils.getStringValue(user.getName()));
		result = StringUtils.replace(result, "{email}", CommonUtils.getStringValue(user.getEmail()));
		result = StringUtils.replace(result, "{phone}", CommonUtils.getStringValue(user.getPhone()));
		result = StringUtils.replace(result, "{holidayName}", CommonUtils.getStringValue(user.getHoliday()));
		BillBusiness currentBusiness = user.getCurrentBusiness();
		if (currentBusiness != null) {
			result = StringUtils.replace(result, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
			if (currentBusiness.getBusinessSector() != null) {
				result = StringUtils.replace(result, "{sector}", CommonUtils.getStringValue(currentBusiness.getBusinessSector().getName()));
			}
			if (currentBusiness.getOwner() != null) {
				result = StringUtils.replace(result, "{vendorContact}", StringUtils.substringAfter(CommonUtils.getStringValue(currentBusiness.getOwner().getPhone()), "+91"));
				result = StringUtils.replace(result, "{vendorEmail}", CommonUtils.getStringValue(currentBusiness.getOwner().getEmail()));
			}
			result = StringUtils.replace(result, "{businessName}", CommonUtils.getStringValue(currentBusiness.getName()));
			result = StringUtils.replace(result, "{mapLocation}", CommonUtils.getStringValue(currentBusiness.getMapLocation()));
			if (CollectionUtils.isNotEmpty(currentBusiness.getItems())) {
				StringBuilder itemBuilder = new StringBuilder();
				for (BillItem item : currentBusiness.getItems()) {
					if (StringUtils.isNotBlank(item.getName())) {
						itemBuilder.append(item.getName()).append(",");
					}
					if (item.getChangeLog() != null) {
						result = StringUtils.replace(result, "{fromDate}",
								CommonUtils.convertDate(item.getChangeLog().getFromDate(), DATE_FORMAT_DISPLAY_NO_YEAR));
						result = StringUtils.replace(result, "{toDate}", CommonUtils.convertDate(item.getChangeLog().getToDate(), DATE_FORMAT_DISPLAY_NO_YEAR));
					}
				}
				result = StringUtils.replace(result, "{itemName}", StringUtils.removeEnd(itemBuilder.toString(), ","));
			} else {
				result = StringUtils.replace(result, "{itemName}", "");
			}
		}
		if (user.getFinancialDetails() != null) {
			result = StringUtils.replace(result, "{accountNumber}", user.getFinancialDetails().getAccountNumber());
			result = StringUtils.replace(result, "{ifscCode}", user.getFinancialDetails().getIfscCode());
		}
		return result;
	}

	public static String prepareSchemeInfo(String result, BillScheme scheme, BillBusiness business) {
		result = StringUtils.replace(result, "{coupon}", CommonUtils.getStringValue(scheme.getCouponCode()));
		result = StringUtils.replace(result, "{schemeName}", CommonUtils.getStringValue(scheme.getSchemeName()));
		result = StringUtils.replace(result, "{schemeDescription}", CommonUtils.getStringValue(scheme.getComments()));
		result = StringUtils.replace(result, "{offerValidity}", CommonUtils.convertDate(scheme.getValidTill(), DATE_FORMAT_DISPLAY_NO_YEAR));
		result = StringUtils.replace(result, "{logoUrl}", BillPropertyUtil.getProperty(BillPropertyUtil.LOGO_URL) + business.getId());
		result = StringUtils.replace(result, "{vendorCommission}", CommonUtils.getStringValue(scheme.getVendorCommission(), true));
 		return result;
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
	
	private String getUserEmails(List<BillUser> users) {
		if (CollectionUtils.isEmpty(users)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (BillUser user : users) {
			if (StringUtils.isEmpty(user.getEmail())) {
				continue;
			}
			builder.append(user.getEmail()).append(",");
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
		if(StringUtils.isNotBlank(mailSubject)) {
			message.setSubject(mailSubject);
		} else {
			String subject = MAIL_SUBJECTS.get(type);
			message.setSubject(subject);
		}
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

	public void setUsers(List<BillUser> users) {
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
			put(MAIL_TYPE_INVOICE, "invoice_new.html");
			put(MAIL_TYPE_PAYMENT_RESULT, "payment_result.html");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "payment_result_admin.html");
			put(MAIL_TYPE_REGISTRATION, "registration.html");
			put(MAIL_TYPE_APPROVAL, "profile_approved.html");
			put(MAIL_TYPE_NEW_CUSTOMER, "customer_added.html");
			put(MAIL_TYPE_PAUSE_CUSTOMER, "customer_pause_delivery.html");
			put(MAIL_TYPE_PAUSE_BUSINESS, "business_pause_delivery.html");
			put(MAIL_TYPE_HOLIDAY, "customer_holiday.html");
			put(MAIL_TYPE_REGISTRATION_ADMIN, "registration_admin.html");
			put(MAIL_TYPE_INVOICE_GENERATION, "invoice_generation.html");
			put(MAIL_TYPE_SETTLEMENT_SUMMARY, "settlement_summary.html");
			put(MAIL_TYPE_COUPON_ACCEPTED, "scheme_accepted.html");
			put(MAIL_TYPE_COUPON_ACCEPTED_ADMIN, "scheme_accepted_admin.html");
			put(MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, "scheme_accepted_business.html");
			put(MAIL_TYPE_COUPON_REDEEMED, "scheme_redeemed.html");
			put(MAIL_TYPE_COUPON_REDEEMED_ADMIN, "scheme_redeemed_admin.html");
			put(MAIL_TYPE_COUPON_REDEEMED_BUSINESS, "scheme_redeemed_business.html");
			put(MAIL_TYPE_GENERIC, "generic.html");
		}
	});

	private static Map<String, String> MAIL_SUBJECTS = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(MAIL_TYPE_INVOICE, "Your Invoice for {month} {year} of Rs. {amount} is generated by {businessName}");
			put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {amount} to {businessName} is {status}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "Your Bill payment for {month} {year} of Rs. {amount} by {name} is {status}");
			put(MAIL_TYPE_REGISTRATION, "Welcome to Pay Per Bill family!");
			put(MAIL_TYPE_APPROVAL, "Congratulations! Your account has been verified and approved!");
			put(MAIL_TYPE_NEW_CUSTOMER, "{businessName} has added you as a customer to their Pay Per Bill account");
			put(MAIL_TYPE_PAUSE_CUSTOMER, "{businessName} has paused your delivery");
			put(MAIL_TYPE_PAUSE_BUSINESS, "{businessName} has paused the service");
			put(MAIL_TYPE_HOLIDAY, "Public holiday alert");
			put(MAIL_TYPE_REGISTRATION_ADMIN, "Alert: New vendor registration!");
			put(MAIL_TYPE_INVOICE_GENERATION, "Invoices generated for {month} {year}");
			put(MAIL_TYPE_SETTLEMENT_SUMMARY, "Your settlement of Rs. {amount} is processed on {date}");
			put(MAIL_TYPE_COUPON_ACCEPTED, "Your offer details for {schemeName}");
			put(MAIL_TYPE_COUPON_ACCEPTED_ADMIN, "Your customer accepted offer for {schemeName}");
			put(MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, "New customer accepted your offer for {schemeName}");
			put(MAIL_TYPE_COUPON_REDEEMED, "You have redeemed the offer {schemeName}");
			put(MAIL_TYPE_COUPON_REDEEMED_ADMIN, "Congrats! Your customer redeemed the offer for {schemeName}");
			put(MAIL_TYPE_COUPON_REDEEMED_BUSINESS, "The customer redeemed your offer for {schemeName}");
		}
	});

	public static String encodeFileToBase64Binary(String fileName) throws IOException {
		File file = new File(fileName);
		byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(FileUtils.readFileToByteArray(file));
		return new String(encoded, StandardCharsets.US_ASCII);
	}

	public static void main(String[] args) throws IOException {
		System.out.println(encodeFileToBase64Binary(BillMailUtil.class.getClassLoader().getResource("email/PayPerBill.png").getPath()));
	}

	public List<BillInvoice> getInvoices() {
		return invoices;
	}

	public void setInvoices(List<BillInvoice> invoices) {
		this.invoices = invoices;
	}

	public boolean isCopyAdmins() {
		return copyAdmins;
	}

	public void setCopyAdmins(boolean copyAdmins) {
		this.copyAdmins = copyAdmins;
	}

	public BillScheme getSelectedScheme() {
		return selectedScheme;
	}

	public void setSelectedScheme(BillScheme selectedScheme) {
		this.selectedScheme = selectedScheme;
	}

	public BillUser getCustomerInfo() {
		return customerInfo;
	}

	public void setCustomerInfo(BillUser customerInfo) {
		this.customerInfo = customerInfo;
	}

}
