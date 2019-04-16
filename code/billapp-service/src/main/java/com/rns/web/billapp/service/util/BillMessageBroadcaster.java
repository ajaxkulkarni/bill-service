package com.rns.web.billapp.service.util;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;

public class BillMessageBroadcaster implements Runnable {

	private List<BillUser> users;
	private String messageType;
	private String requestType;
	private BillScheme scheme;
	private String subject;

	public BillMessageBroadcaster() {

	}
	
	public void setScheme(BillScheme scheme) {
		this.scheme = scheme;
	}

	public BillMessageBroadcaster(List<BillUser> receivers, String type, String request) {
		this.users = receivers;
		this.messageType = type;
		this.requestType = request;
	}

	public void run() {
		try {
			if (CollectionUtils.isNotEmpty(users)) {
				LoggingUtil.logMessage("Running bulk broacasting .." + requestType);
				Integer count = 0;
				for (BillUser customer : users) {
					if (BillConstants.REQUEST_TYPE_EMAIL.equals(requestType)) {
						BillMailUtil mailUtil = new BillMailUtil(messageType);
						mailUtil.setUser(customer);
						mailUtil.setInvoice(customer.getCurrentInvoice());
						mailUtil.setSelectedScheme(scheme);
						mailUtil.setMailSubject(subject);
						mailUtil.sendMail();
					} else {
						BillSMSUtil.sendSMS(customer, customer.getCurrentInvoice(), messageType, scheme);
					}
					count++;
				}
				LoggingUtil.logMessage("End of bulk broacasting .. Broadcasted to " + count + " type " + requestType);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}
