package com.rns.web.billapp.service.util;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;

public class BillUserLogUtil implements BillConstants {
	
	public static void updateBillItemLog(BillItem item, Session session, BillGenericDaoImpl dao, BillDBItemSubscription dbSubscribedItem) {
		if(item.getQuantity() != null) {
			BillDBUserLog userLog = new BillDBUserLog();
			userLog.setFromDate(new Date());
			userLog.setCreatedDate(new Date());
			userLog.setChangeType(LOG_CHANGE_PERM);
			BillUserLog changeLog = item.getChangeLog();
			if(changeLog != null) {
				if(changeLog.getFromDate() != null) {
					userLog.setFromDate(changeLog.getFromDate());
				}
				if(changeLog.getToDate() != null) {
					userLog.setToDate(changeLog.getToDate());
					userLog.setChangeType(LOG_CHANGE_TEMP);
				}
			}
			userLog.setQuantityChange(item.getQuantity());
			addParents(dao, dbSubscribedItem, userLog);
			session.persist(userLog);
		}
		//Subscription ended
		if(StringUtils.equals(STATUS_DELETED, dbSubscribedItem.getStatus())) {
			BillDBUserLog userLog = new BillDBUserLog();
			userLog.setFromDate(new Date());
			userLog.setCreatedDate(new Date());
			userLog.setChangeType(LOG_CHANGE_PERM);
			userLog.setQuantityChange(BigDecimal.ZERO);
			addParents(dao, dbSubscribedItem, userLog);
			session.persist(userLog);
		}
	}
	
	private static void addParents(BillGenericDaoImpl dao, BillDBItemSubscription dbSubscribedItem, BillDBUserLog userLog) {
		userLog.setBusinessItem(dbSubscribedItem.getBusinessItem());
		userLog.setSubscription(dbSubscribedItem.getSubscription());
		if(userLog.getSubscription() != null) {
			BillDBSubscription dbSubscription = null;
			if(userLog.getSubscription().getBusiness() == null || userLog.getSubscription().getBusiness().getId() == null) {
				dbSubscription = dao.getEntityByKey(BillDBSubscription.class, ID_ATTR, userLog.getSubscription().getId(), true);
				if(dbSubscription != null) {
					userLog.setBusiness(dbSubscription.getBusiness());
				}
			} else {
				userLog.setBusiness(userLog.getSubscription().getBusiness());
			}
			
		}
	}

}
