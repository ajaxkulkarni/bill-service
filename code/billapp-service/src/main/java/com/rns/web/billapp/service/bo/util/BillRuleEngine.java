package com.rns.web.billapp.service.bo.util;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;

public class BillRuleEngine {

	public static boolean isDelivery(List<BillDBUserLog> logs, BillSubscription currentSubscription) {
		if(CollectionUtils.isEmpty(logs)) {
			return true;
		}
		if(currentSubscription == null || CollectionUtils.isEmpty(currentSubscription.getItems())) {
			return false;
		}
		int noOrder = 0;	
		for(BillDBUserLog log: logs) {
			for(BillItem item: currentSubscription.getItems()) {
				if(item.getParentItemId() != null && log.getParentItem() != null && log.getParentItem().getId() == item.getParentItemId()) {
					noOrder = setQuantity(log, item, noOrder);
							
				} else if (log.getBusinessItem() != null && item.getParentItem() != null && item.getParentItem().getId() == log.getBusinessItem().getId()) {
					noOrder = setQuantity(log, item, noOrder);
				}
			}
		}
		if(noOrder == currentSubscription.getItems().size()) {
			return false;
		}
		return true;
	}

	private static int setQuantity(BillDBUserLog log, BillItem item, int noOrder) {
		if(item.getQuantity() == null || !item.getQuantity().equals(BigDecimal.ZERO)) {
			item.setQuantity(log.getQuantityChange());
			if(item.getQuantity() != null && item.getQuantity().equals(BigDecimal.ZERO)) {
				noOrder++;
			}
		}
		return noOrder;
	}
	
}
