package com.rns.web.billapp.service.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import net.swisstech.bitly.BitlyClient;
import net.swisstech.bitly.builder.v3.ShortenRequest;
import net.swisstech.bitly.model.v3.ShortenResponse;

public class BillSMSUtil implements BillConstants {
	
	private static final String SMS_COSMETIC_SEPARATOR = " ---------------- ";
	private static final String SMS_URL = "http://api.msg91.com/api/sendhttp.php?sender=PAYBIL&route=4&mobiles={mobiles}&authkey=193344AsiDSe0j5a5db681&country=0&message={message}";
	private static final String ADMIN_PHONES = "9923283604,9623736773";
	
	private BillUser customer;
	private String messageText;
	
	public static String sendSMS(BillUser user, BillInvoice invoice, String type, BillScheme selectedScheme) {
		String result = "";
		try {
			LoggingUtil.logMessage("Sending SMS to -- " + user.getPhone());
			result = generateResultMessage(user, invoice, type, selectedScheme);
			sendSMSProcess(user, type, result);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	private static void sendSMSProcess(BillUser user, String type, String result) throws UnsupportedEncodingException {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		String smsUrl = SMS_URL;
		smsUrl = StringUtils.replace(smsUrl, "{message}", URLEncoder.encode(result, "UTF-8"));
		
		if (StringUtils.contains(type, "Admin")) {
			smsUrl = StringUtils.replace(smsUrl, "{mobiles}", ADMIN_PHONES);
		} else {
			String phone = user.getPhone();
			if(user.getPhone() != null && user.getPhone().length() != 10) {
				phone = CommonUtils.trimPhoneNumber(user.getPhone());
			}
			LoggingUtil.logMessage("Sending SMS to phone number => " + phone);
			smsUrl = StringUtils.replace(smsUrl, "{mobiles}", phone);
		}
		
		WebResource webResource;
		webResource = client.resource(smsUrl);
		ClientResponse response = webResource.get(ClientResponse.class);
		String entity = response.getEntity(String.class);
		LoggingUtil.logMessage("SMS response -- " + entity);
	}

	public static String generateResultMessage(BillUser user, BillInvoice invoice, String type, BillScheme selectedScheme) {
		String result;
		result = SMS_TEXT.get(type);
		
		if(user != null) {
			result = BillMailUtil.prepareUserInfo(result, user);
		}
		
		if(selectedScheme != null && user.getCurrentBusiness() != null) {
			result = BillMailUtil.prepareSchemeInfo(result, selectedScheme, user.getCurrentBusiness());
		}
		
		if(invoice != null) {
			result = BillMailUtil.prepareInvoiceInfo(result, invoice, user);
			if(StringUtils.equals(BillConstants.PAYMENT_STATUS_CREDIT, invoice.getStatus())) {
				result = StringUtils.replace(result, "{status}", "Successful");	
			} else {
				result = StringUtils.replace(result, "{status}", "Failed");	
			}
			
			if(CollectionUtils.isNotEmpty(invoice.getInvoiceItems()) && BillRuleEngine.showBillDetails(user)) {
				StringBuilder builder = new StringBuilder();
				builder.append(SMS_COSMETIC_SEPARATOR).append("\n");
				if(BillRuleEngine.showBillDetails(user)) {
					for(BillItem invoiceItem: invoice.getInvoiceItems()) {
						builder.append(CommonUtils.getStringValue(invoiceItem.getQuantity(), true)).append("  ");
						if(invoiceItem.getParentItem() != null) {
							builder.append(CommonUtils.getStringValue(invoiceItem.getParentItem().getName()));
						} else {
							builder.append(CommonUtils.getStringValue(invoiceItem.getName()));
						}
						builder.append(" = ").append(CommonUtils.getStringValue(invoiceItem.getPrice(), false));
						builder.append("\n");
					}
				}
				builder.append(SMS_COSMETIC_SEPARATOR).append("\n");
				
				if(invoice.getAmount() != null && BigDecimal.ZERO.compareTo(invoice.getAmount()) < 0 ) {
					if(BillRuleEngine.showBillDetails(user)) {
						builder.append("Total = ").append(CommonUtils.getStringValue(invoice.getAmount(), false)).append("\n");;
					} else {
						builder.append("Total = ").append(CommonUtils.getStringValue(BillRuleEngine.getBillTotal(invoice), false)).append("\n");
					}
				} 
				if(invoice.getPendingBalance() != null && BigDecimal.ZERO.compareTo(invoice.getPendingBalance()) < 0 && BillRuleEngine.showBillDetails(user)) {
					builder.append("Pending = ").append(CommonUtils.getStringValue(invoice.getPendingBalance(), false)).append("\n");;
				}
				if(invoice.getServiceCharge() != null && BigDecimal.ZERO.compareTo(invoice.getServiceCharge()) < 0 && BillRuleEngine.showBillDetails(user)) {
					builder.append("Service charge = ").append(CommonUtils.getStringValue(invoice.getServiceCharge(), false)).append("\n");;
				}
				if(invoice.getCreditBalance() != null && BigDecimal.ZERO.compareTo(invoice.getCreditBalance()) < 0 && BillRuleEngine.showBillDetails(user)) {
					builder.append("Credit = ").append(CommonUtils.getStringValue(invoice.getCreditBalance(), false)).append("\n");;
				}
				if(invoice.getOutstandingBalance() != null && BigDecimal.ZERO.compareTo(invoice.getOutstandingBalance()) < 0) {
					builder.append("Outstanding = ").append(CommonUtils.getStringValue(invoice.getOutstandingBalance(), false)).append("\n");;
				}
				builder.append(SMS_COSMETIC_SEPARATOR).append("\n");
				result = StringUtils.replace(result, "{smsInvoiceItems}", builder.toString());
			} else {
				result = StringUtils.replace(result, "{smsInvoiceItems}", "");
			}
			
		}
		return result;
	}
	
	
	
	public void sendSms(BillUser user, BillInvoice invoice, String type, BillScheme selectedScheme) {
		try {
			LoggingUtil.logMessage("Sending SMS to -- " + user.getPhone());
			String result = generateResultMessage(user, invoice, type, selectedScheme);
			if(customer != null) {
				result = BillMailUtil.prepareCustomerInfo(result, customer);
			}
			if(StringUtils.isNotBlank(messageText)) {
				result = StringUtils.replace(result, "{message}", messageText);
			}
			sendSMSProcess(user, type, result);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
	}

	public BillUser getCustomer() {
		return customer;
	}

	public void setCustomer(BillUser customer) {
		this.customer = customer;
	}

	private static Map<String, String> SMS_TEXT = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(MAIL_TYPE_INVOICE, "Your Invoice for {month} {year} of Rs. {payable} is generated by {businessName}. \n{smsInvoiceItems} Pay the invoice now by going to - {paymentUrl} ");
			put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {payable} to {businessName} is {status} \nPayment ID: {paymentId} \nBill No: {invoiceId}\nGet exciting offers on this bill now - {offersUrl}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "The Bill payment for {month} {year} of Rs. {payable} by {name} is {status} \nPayment ID: {paymentId} \nBill No: {invoiceId}\nPaid by: {paymentMode}");
			put(MAIL_TYPE_REGISTRATION, "Hello {name}! Welcome to Pay Per Bill family! Pay Per Bill will help your business to be more efficient and profitable by going online.");
			put(MAIL_TYPE_APPROVAL, "Congratulations {name}! Your Pay Per Bill account has been verified and approved! You can start accepting payments once you complete your bank details on the app.");
			put(MAIL_TYPE_NEW_CUSTOMER, "Hello {name}! {businessName} has added you as a customer to their Pay Per Bill account. You can start tracking and paying your {sector} bills online now. For more details - contact your vendor {vendorContact}");
			put(MAIL_TYPE_NEW_CUSTOMER_VENDOR, "Hello {name}! New customer registered to your business {businessName}.\nName = {customerName}\nEmail - {customerEmail}\nPhone - {customerPhone}\nAddress - {customerAddress}\nArea - {customerLocation}");
			put(MAIL_TYPE_PAUSE_CUSTOMER, "Hello {name}! {businessName} has paused the delivery for {itemName} from {fromDate} to {toDate}.");
			put(MAIL_TYPE_PAUSE_BUSINESS, "Hello {name}! {businessName} has paused the delivery for {sector} from {fromDate} to {toDate}.");
			put(MAIL_TYPE_HOLIDAY, "Hello {name}! Your order from {businessName} for {sector} will not be delivered today due to a public holiday - {holidayName}");
			put(MAIL_TYPE_REGISTRATION_ADMIN, "We have a new registration - \n Name - {name} \n Email {email} \n Phone - {phone}");
			put(MAIL_TYPE_INVOICE_GENERATION, "Hi {name} ! Invoices generated for {month} {year}. Please review the invoices before sending out to customers. \n No of invoices = {amount} \n Amount raised = {payable}.");
			put(MAIL_TYPE_SETTLEMENT_SUMMARY, "Your settlement of Rs. {amount} is processed on {date}. Please check your email {email} for more details. \nReference no - {settlementId}. \nPayment will be credited to your account in 24 hours.");
			put(MAIL_TYPE_COUPON_ACCEPTED, "Congratulations {name}! Your offer details for {schemeName}.\nCoupon code - {coupon}\nBusiness contact - {vendorContact} | {vendorEmail}\nOffer valid till - {offerValidity}\nStore location - {mapLocation}");
			put(MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, "Hi {name}! New customer {customerName} collected the offer for {schemeName}.\nCoupon code - {coupon}\nContact - {customerPhone} | {customerEmail}\nOffer valid till - {offerValidity}");
			put(MAIL_TYPE_COUPON_REDEEMED, "Hello {name}! Your have redeemed the offer for {schemeName}.\nCoupon code - {coupon}\nContact vendor in case of any queries - {vendorContact} | {vendorEmail}");
			put(MAIL_TYPE_COUPON_REDEEMED_BUSINESS, "Hi {name}! A customer {customerName} redeemed the offer for {schemeName}.\nCoupon code - {coupon}\nContact - {customerPhone} | {customerEmail}");
			put(MAIL_TYPE_COUPON_REDEEMED_ADMIN, "Hi {name}! A customer {customerName} redeemed the offer for {schemeName}.\nCongratulations! You have won reward of Rs. {vendorCommission} !The reward amount will be settled into your back account within 1-2 days.");
			put(MAIL_TYPE_GENERIC, "{message}");
		}
	});

	public static String getPhones(List<BillUser> users) {
		if (CollectionUtils.isEmpty(users)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (BillUser user : users) {
			if (StringUtils.isEmpty(user.getPhone())) {
				continue;
			}
			builder.append(user.getPhone()).append(",");
		}
		return StringUtils.removeEnd(builder.toString(), ",");
	}
	
	public static String shortenUrl(String shortUrl, String url) {
		if(StringUtils.isNotBlank(shortUrl)) {
			return shortUrl;
		}
		if(StringUtils.isBlank(url)) {
			return url;
		}
		BitlyClient client = new BitlyClient(BillConstants.BITLY_ACCESS_TOKEN);
		/*net.swisstech.bitly.model.Response<ShortenResponse> resp = client.shorten()
		                          .setLongUrl(url)
		                          .call();*/
		ShortenRequest shortenRequest = new ShortenRequest(BillConstants.BITLY_ACCESS_TOKEN);
		//shortenRequest.setDomain("payperbill.in");
		net.swisstech.bitly.model.Response<ShortenResponse> resp = shortenRequest.setLongUrl(url).call();

		
		if(resp == null || resp.data == null) {
			return null;
		}
		LoggingUtil.logMessage("Converting link " + url + " to " + resp.data.url);
		return resp.data.url;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
	
}
