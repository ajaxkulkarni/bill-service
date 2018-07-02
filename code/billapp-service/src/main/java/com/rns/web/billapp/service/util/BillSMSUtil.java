package com.rns.web.billapp.service.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class BillSMSUtil implements BillConstants {
	
	private static final String SMS_URL = "http://api.msg91.com/api/sendhttp.php?sender=PAYBIL&route=4&mobiles={mobiles}&authkey=193344AsiDSe0j5a5db681&country=0&message={message}"; 
	
	public static String sendSMS(BillUser user, BillInvoice invoice, String type) {
		String result = "";
		try {
			LoggingUtil.logMessage("Sending SMS to -- " + user.getPhone());
			
			result = SMS_TEXT.get(type);
			
			if(user != null) {
				result = BillMailUtil.prepareUserInfo(result, user);
			}
			
			if(invoice != null) {
				result = BillMailUtil.prepareInvoiceInfo(result, invoice);
				if(StringUtils.equals(BillConstants.PAYMENT_STATUS_CREDIT, invoice.getStatus())) {
					result = StringUtils.replace(result, "{status}", "Successful");	
				} else {
					result = StringUtils.replace(result, "{status}", "Failed");	
				}
				
			}
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			String smsUrl = SMS_URL;
			smsUrl = StringUtils.replace(smsUrl, "{message}", URLEncoder.encode(result, "UTF-8"));
			smsUrl = StringUtils.replace(smsUrl, "{mobiles}", user.getPhone());
			WebResource webResource;
			webResource = client.resource(smsUrl);
			ClientResponse response = webResource.get(ClientResponse.class);
			String entity = response.getEntity(String.class);
			LoggingUtil.logMessage("SMS response -- " + entity);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}
	
	private static Map<String, String> SMS_TEXT = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(MAIL_TYPE_INVOICE, "Your Invoice for {month} {year} of Rs. {payable} is generated by {businessName}. Pay the invoice now by going to - {paymentUrl} ");
			put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {amount} to {businessName} is {status}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "The Bill payment for {month} {year} of Rs. {amount} by {name} is {status}");
			put(MAIL_TYPE_REGISTRATION, "Hello {name}! Welcome to Pay Per Bill family! Pay Per Bill will help your business to be more efficient and profitable by going online.");
			put(MAIL_TYPE_APPROVAL, "Congratulations {name}! Your Pay Per Bill account has been verified and approved! You can start accepting payments once you complete your bank details on the app.");
			put(MAIL_TYPE_NEW_CUSTOMER, "Hello {name}! {businessName} has added you as a customer to their Pay Per Bill account. You can start tracking and paying your {sector} bills online now. For more details - contact your vendor {vendorContact}");
			put(MAIL_TYPE_PAUSE_CUSTOMER, "Hello {name}! {businessName} has paused the delivery for {itemName} from {startDate} to {toDate}.");
		}
	});
	
}
