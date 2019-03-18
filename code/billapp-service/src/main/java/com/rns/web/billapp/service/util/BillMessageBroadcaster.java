package com.rns.web.billapp.service.util;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.rns.web.billapp.service.bo.domain.BillUser;

public class BillMessageBroadcaster implements Runnable {

	private List<BillUser> users;
	private String messageType;
	private String requestType;

	public BillMessageBroadcaster() {

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
						mailUtil.sendMail();
					} else {
						BillSMSUtil.sendSMS(customer, customer.getCurrentInvoice(), messageType, null);
					}
					count++;
				}
				LoggingUtil.logMessage("End of bulk broacasting .. Broadcasted to " + count + " type " + requestType);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
	}

}