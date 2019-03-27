package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.rns.web.billapp.service.bo.domain.BillGoogleNotification;
import com.rns.web.billapp.service.bo.domain.BillGoogleNotificationRequest;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBDevices;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class BillFCMNotificationBroadcaster implements Runnable, BillConstants {
	
	private static final String DEFAULT_INTENT = "transactionsIntent";
	private static final String NOTIFICATION_ICON = "ic_payperbill_logo";
	private static final String CHANNEL_PAY_PER_BILL = "PayPerBill";
	private BillUser user;
	private BillInvoice invoice;
	private SessionFactory sessionFactory;
	private String notificationType;
	
	public BillFCMNotificationBroadcaster() {
	
	}
	
	public BillFCMNotificationBroadcaster(BillUser user, BillInvoice invoice, SessionFactory sessionFactory) {
		this.user = user;
		this.invoice = invoice;
		this.sessionFactory = sessionFactory;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}
	
	public void broadcastNotification() {
		
		if(user == null || user.getId() == null) {
			return;
		}
		
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillDBDevices> devices = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBDevices.class, "user.id", user.getId(), true, "id", "desc");
			if(CollectionUtils.isEmpty(devices)) {
				return;
			}
			BillGoogleNotificationRequest request = new BillGoogleNotificationRequest();
			request.setRegistration_ids(prepareRegistrationIds(devices));
			BillGoogleNotification notification = new BillGoogleNotification();
			notification.setAndroid_channel_id(CHANNEL_PAY_PER_BILL);
			String bodyText = NOTIFICATION_BODY.get(notificationType);
			bodyText = BillMailUtil.prepareInvoiceInfo(bodyText, invoice, user);
			if(StringUtils.isBlank(bodyText)) {
				return;
			}
			bodyText = setStatus(bodyText);
			bodyText = BillMailUtil.prepareUserInfo(bodyText, user);
			String titleText = NOTIFICATION_TEXT.get(notificationType);
			titleText = BillMailUtil.prepareInvoiceInfo(titleText, invoice, user);
			titleText = BillMailUtil.prepareUserInfo(titleText, user);
			titleText = setStatus(titleText);
			notification.setBody(bodyText);
			notification.setTitle(titleText);
			notification.setIcon(NOTIFICATION_ICON);
			if(StringUtils.equals(MAIL_TYPE_PAYMENT_RESULT_VENDOR, notificationType)) {
				notification.setClick_action(DEFAULT_INTENT);
			}
			request.setNotification(notification);
			postNotification(request);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
	    
	}

	private String setStatus(String bodyText) {
		if(StringUtils.equals(BillConstants.PAYMENT_STATUS_CREDIT, invoice.getStatus())) {
			bodyText = StringUtils.replace(bodyText, "{status}", "Successful");	
		} else {
			bodyText = StringUtils.replace(bodyText, "{status}", "Failed");	
		}
		return bodyText;
	}

	private void postNotification(BillGoogleNotificationRequest request) throws IOException, JsonGenerationException, JsonMappingException {
		String url = BillPropertyUtil.getProperty(BillPropertyUtil.FCM_URL);
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(config);

		WebResource webResource = client.resource(url);
		LoggingUtil.logMessage("Calling FCM URL :" + url + " request:" + new ObjectMapper().writeValueAsString(request), LoggingUtil.smsLogger);

		ClientResponse response = webResource.type("application/json").header("Authorization", "key=" + BillPropertyUtil.getProperty(BillPropertyUtil.FCM_SERVER_KEY)).post(ClientResponse.class, request);

		if (response.getStatus() != 200) {
			LoggingUtil.logMessage("Failed in FCM URL : HTTP error code : " + response.getStatus(), LoggingUtil.smsLogger);
		}
		String output = response.getEntity(String.class);
		LoggingUtil.logMessage("Output from FCM URL : " + response.getStatus() + ".... \n " + output, LoggingUtil.smsLogger);
	}

	private List<String> prepareRegistrationIds(List<BillDBDevices> devices) {
		if(CollectionUtils.isNotEmpty(devices)) {
			List<String> deviceIds = new ArrayList<String>();
			for(BillDBDevices device: devices) {
				deviceIds.add(device.getToken());
			}
			return deviceIds;
		}
		return null;
	}

	public void run() {
		broadcastNotification();
	}
	
	private static Map<String, String> NOTIFICATION_BODY = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			//put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {payable} to {businessName} is {status} \nPayment ID: {paymentId} \nBill No: {invoiceId}\nGet exciting offers on this bill now - {offersUrl}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "Payment by {name} for {month} {year} of Rs.{payable} is {status}");
			put(MAIL_TYPE_GENERIC, "{message}");
		}
	});
	
	private static Map<String, String> NOTIFICATION_TEXT = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			//put(MAIL_TYPE_PAYMENT_RESULT, "Your Bill payment for {month} {year} of Rs. {payable} to {businessName} is {status} \nPayment ID: {paymentId} \nBill No: {invoiceId}\nGet exciting offers on this bill now - {offersUrl}");
			put(MAIL_TYPE_PAYMENT_RESULT_VENDOR, "Payment {status} by {name} for {month} {year}");
			put(MAIL_TYPE_GENERIC, "{message}");
		}
	});

}
