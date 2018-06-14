package com.rns.web.billapp.service.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;

public class BillRuleEngine {

	
	public static boolean isDelivery(List<BillUserLog> logs, BillDBItemSubscription subscription) {
		if(CollectionUtils.isEmpty(logs)) {
			return true;
		}
		if(subscription == null) {
			return false;
		}
		//int noOrder = 0;	
		for(BillUserLog log: logs) {
			if(subscription.getBusinessItem() != null && subscription.getBusinessItem().getParent() != null && log.getParentItemId() == subscription.getBusinessItem().getParent().getId()) {
				if(log.getQuantityChange() != null && BigDecimal.ZERO.equals(log.getQuantityChange())) {
					return false;
				}
						
			} else if (subscription.getBusinessItem() != null && subscription.getBusinessItem().getId() == log.getBusinessItemId()) {
				if(log.getQuantityChange() != null && BigDecimal.ZERO.equals(log.getQuantityChange())) {
					return false;
				}
			}
		}
		/*if(noOrder == currentSubscription.getItems().size()) {
			return false;
		}*/
		return true;
	}
	

	private static void setQuantity(BillUserLog log, BillItem item) {
		if(item.getQuantity() == null || !item.getQuantity().equals(BigDecimal.ZERO)) {
			item.setQuantity(log.getQuantityChange());
			/*if(item.getQuantity() != null && item.getQuantity().equals(BigDecimal.ZERO)) {
				noOrder++;
			}*/
		}
		//return noOrder;
	}


	public static void calculatePayable(BillInvoice invoice) {
		MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
		invoice.setPayable(invoice.getAmount());
		if(invoice.getPendingBalance() != null) {
			invoice.setPayable(invoice.getPayable().add(invoice.getPendingBalance()));
		}
		if(invoice.getCreditBalance() != null) {
			invoice.setPayable(invoice.getPayable().subtract(invoice.getCreditBalance()));
		}
		if(invoice.getServiceCharge() != null) {
			invoice.setPayable(invoice.getPayable().add(invoice.getServiceCharge()));
		}
		BigDecimal internetHandlingFees = invoice.getPayable().multiply(new BigDecimal(BillConstants.PAYMENT_CHARGE_PERCENT), mc).add(new BigDecimal(BillConstants.PAYMENT_CHARGE_FIXED), mc);
		//Add GST 18%
		internetHandlingFees = internetHandlingFees.add(internetHandlingFees.multiply(new BigDecimal(0.18)));
		invoice.setInternetFees(internetHandlingFees);
		invoice.setPayable(invoice.getPayable().add(invoice.getInternetFees()));
	}
	
}
