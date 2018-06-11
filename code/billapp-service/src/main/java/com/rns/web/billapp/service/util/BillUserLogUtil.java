package com.rns.web.billapp.service.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
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
			userLog.setQuantityChange(item.getQuantity());
			BillUserLog changeLog = item.getChangeLog();
			addParents(dao, dbSubscribedItem, userLog, item);
			if(changeLog != null) {
				if(changeLog.getFromDate() != null) {
					userLog.setFromDate(changeLog.getFromDate());
				}
				if(changeLog.getToDate() != null) {
					userLog.setToDate(changeLog.getToDate());
					userLog.setChangeType(LOG_CHANGE_TEMP);
					//userLog = getResultantLog(new BillLogDAOImpl(session).getActiveItemSubscription(userLog), userLog);
					
				}
			}
			session.persist(userLog);
		}
		//Subscription ended
		if(StringUtils.equals(STATUS_DELETED, dbSubscribedItem.getStatus())) {
			BillDBUserLog userLog = new BillDBUserLog();
			userLog.setFromDate(new Date());
			userLog.setCreatedDate(new Date());
			userLog.setChangeType(LOG_CHANGE_PERM);
			userLog.setQuantityChange(BigDecimal.ZERO);
			addParents(dao, dbSubscribedItem, userLog, null);
			session.persist(userLog);
		}
	}
	
	private static BillDBUserLog getResultantLog(List<BillDBUserLog> activeItemSubscriptions, BillDBUserLog userLog, Session session) {
		if(CollectionUtils.isEmpty(activeItemSubscriptions)) {
			return userLog;
		}
		for(BillDBUserLog log:activeItemSubscriptions) {
			if(CommonUtils.isGreaterThan(log.getFromDate(), userLog.getFromDate()) && CommonUtils.isGreaterThan(userLog.getToDate(), log.getToDate())) {
				//Between the 2 dates..overlap
				session.delete(log);
			} else if(CommonUtils.isGreaterThan(log.getFromDate(), userLog.getFromDate()) && CommonUtils.isGreaterThan(log.getToDate(), userLog.getToDate())) {
				//From date is between and to date surpasses
				log.setToDate(DateUtils.addDays(userLog.getFromDate(), -1));
			} else if(CommonUtils.isGreaterThan(log.getFromDate(), userLog.getFromDate()) && CommonUtils.isGreaterThan(log.getToDate(), userLog.getToDate())) {
				//From date is between and to date surpasses
				log.setToDate(DateUtils.addDays(userLog.getFromDate(), -1));
			}
		}
		return null;
	}

	private static void addParents(BillGenericDaoImpl dao, BillDBItemSubscription dbSubscribedItem, BillDBUserLog userLog, BillItem item) {
		userLog.setBusinessItem(dbSubscribedItem.getBusinessItem());
		userLog.setSubscription(dbSubscribedItem.getSubscription());
		if(dbSubscribedItem.getBusinessItem() != null) {
			userLog.setBusiness(dbSubscribedItem.getBusinessItem().getBusiness());
		}
		if(item != null && item.getParentItemId() != null) {
			BillDBItemParent parent = new BillDBItemParent();
			parent.setId(item.getParentItemId());
			userLog.setParentItem(parent);
		}
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

	public static void updateBillItemParentLog(BillDBItemParent dbItem, BillItem item, Session session) {
		if(item.getPrice() != null || StringUtils.isNotBlank(item.getWeekDays())) {
			BillDBUserLog userLog = new BillDBUserLog();
			userLog.setFromDate(new Date());
			userLog.setCreatedDate(new Date());
			userLog.setChangeType(LOG_CHANGE_PERM);
			userLog.setPriceChange(item.getPrice());
			userLog.setWeeklyPricing(item.getWeeklyPricing());
			userLog.setParentItem(dbItem);
			session.persist(userLog);
		}
	}

	public static List<BillUserLog> getUserLogs(List<Object[]> resultset) {
		if(CollectionUtils.isEmpty(resultset)) {
			return null;
		}
		List<BillUserLog> logs = new ArrayList<BillUserLog>();
		for(Object[] row: resultset) {
			if(ArrayUtils.isEmpty(row)) {
				continue;
			}
			BillUserLog log = new BillUserLog();
			log.setSubscriptionId((Integer) row[1]);
			log.setBusinessItemId((Integer) row[2]);
			log.setChangeType(row[3].toString());
			log.setFromDate((Date) row[4]);
			log.setToDate((Date) row[5]);
			log.setPriceChange((BigDecimal) row[6]);
			log.setQuantityChange((BigDecimal) row[7]);
			log.setParentItemId((Integer) row[9]);
			log.setBusinessId((Integer) row[10]);
			log.setWeeklyPricing((String) row[11]);
			logs.add(log);
		}
		return logs;
	}
	
}
