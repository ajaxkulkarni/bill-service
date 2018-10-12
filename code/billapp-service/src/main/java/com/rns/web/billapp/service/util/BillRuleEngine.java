package com.rns.web.billapp.service.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;

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
				//Parent Item holiday
				return isOrder(log);
			} else if (log.getSubscriptionId() == null && subscription.getBusinessItem() != null && subscription.getBusinessItem().getId() == log.getBusinessItemId()) {
				//Business Item holiday
				return isOrder(log);
			} else if (log.getSubscriptionId() != null && subscription.getSubscription() != null && log.getSubscriptionId().intValue() == subscription.getSubscription().getId().intValue()
					&& subscription.getBusinessItem() != null && subscription.getBusinessItem().getId() == log.getBusinessItemId()) {
				//Customer holiday
				return isOrder(log);
			}
		}
		/*if(noOrder == currentSubscription.getItems().size()) {
			return false;
		}*/
		return true;
	}


	private static boolean isOrder(BillUserLog log) {
		if(log.getQuantityChange() != null && BigDecimal.ZERO.equals(log.getQuantityChange())) {
			return false;
		}
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


	public static void calculatePayable(BillInvoice invoice, BillDBInvoice dbInvoice, Session session) {
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
		internetHandlingFees = internetHandlingFees.add(internetHandlingFees.multiply(new BigDecimal(0.18), mc), mc);
		internetHandlingFees = BigDecimal.ZERO; //TODO: Change later
		invoice.setInternetFees(internetHandlingFees);
		invoice.setPayable(invoice.getPayable().add(invoice.getInternetFees()));
		
		//TODO Later
		if(dbInvoice != null && dbInvoice.getSubscription() != null) {
			BigDecimal outstanding = BigDecimal.ZERO;
			//Outstanding amount is the total amount of the bills excluding this bill month / year
			List<Object[]> result = new BillInvoiceDaoImpl(session).getCustomerOutstanding(dbInvoice.getMonth(), dbInvoice.getYear(), dbInvoice.getSubscription().getId());
			if(CollectionUtils.isNotEmpty(result)) {
				for(Object[] row: result) {
					outstanding = outstanding.add(CommonUtils.getAmount(row[0]));
					outstanding = outstanding.add(CommonUtils.getAmount(row[1]));
					outstanding = outstanding.add(CommonUtils.getAmount(row[2]));
				 	outstanding = outstanding.subtract(CommonUtils.getAmount(row[3]));
				}
				invoice.setOutstandingBalance(outstanding);
				invoice.setPayable(invoice.getPayable().add(outstanding));
			}
		}
	}

	public static boolean showBillDetails(BillUser user) {
		if(StringUtils.equals(BillConstants.NO, user.getShowBillDetails())) {
			return false;
		} else if (user.getShowBillDetails() == null && user.getCurrentBusiness() != null && StringUtils.equals(BillConstants.NO, user.getCurrentBusiness().getShowBillDetails())) {
			return false;
		}
		return true;
	}
	
	
}
