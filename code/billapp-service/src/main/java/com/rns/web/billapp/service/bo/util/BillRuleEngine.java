package com.rns.web.billapp.service.bo.util;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

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
	
}
