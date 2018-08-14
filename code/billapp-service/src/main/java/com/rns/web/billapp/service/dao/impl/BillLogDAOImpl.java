package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.rns.web.billapp.service.dao.domain.BillDBHoliday;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

public class BillLogDAOImpl {

	private Session session;

	public BillLogDAOImpl(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return session;
	}

	private SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}

	public List<BillDBUserLog> getActiveItemSubscription(BillDBUserLog userLog) {
		Criteria criteria = session.createCriteria(BillDBUserLog.class)
				.add(Restrictions.or(Restrictions.ge("fromDate", userLog.getFromDate()), Restrictions.le("toDate", userLog.getToDate())))
				.add(Restrictions.eq("subscription", userLog.getSubscription()))
				.add(Restrictions.eq("item", userLog.getBusinessItem()));
		return criteria.list();
	}

	public BillDBUserLog getLatestSubscribedItemQuantityLog(BillDBUserLog userLog) {
		Criteria criteria = session.createCriteria(BillDBUserLog.class)
				.add(Restrictions.le("fromDate", userLog.getFromDate()))
				.add(Restrictions.ge("toDate", userLog.getFromDate()))
				.add(Restrictions.eq("business.id", userLog.getBusiness().getId()))
				.add(Restrictions.isNotNull("quantityChange"))
				//.add(Restrictions.in("businessItem", businessItems))
				.add(Restrictions.eq("businessItem.id", userLog.getBusinessItem().getId()))
				.addOrder(Order.desc("fromDate"))
				.addOrder(Order.desc("createdDate"));
		List<BillDBUserLog> list = criteria.list();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}
	
	public BillDBUserLog getLatestBusinessItemQuantityLog(BillDBUserLog userLog) {
		Criteria criteria = session.createCriteria(BillDBUserLog.class)
				.add(Restrictions.le("fromDate", userLog.getFromDate()))
				.add(Restrictions.ge("toDate", userLog.getFromDate()))
				.add(Restrictions.eq("business.id", userLog.getBusiness().getId()))
				.add(Restrictions.isNotNull("quantityChange"))
				.add(Restrictions.isNull("subscription"))
				.add(Restrictions.eq("businessItem.id", userLog.getBusinessItem().getId()))
				.addOrder(Order.desc("fromDate"))
				.addOrder(Order.desc("createdDate"));
		List<BillDBUserLog> list = criteria.list();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	public BillDBUserLog getLatestParentItemsQuantityLog(BillDBUserLog userLog) {
		Criteria criteria = session.createCriteria(BillDBUserLog.class)
				.add(Restrictions.ge("fromDate", userLog.getFromDate()))
				.add(Restrictions.le("toDate", userLog.getFromDate()))
				//.add(Restrictions.in("parentItem", parentItems))
				.add(Restrictions.eq("parentItem.id", userLog.getParentItem().getId()))
				.add(Restrictions.isNotNull("quantityChange"))
				.addOrder(Order.desc("fromDate"))
				.addOrder(Order.desc("createdDate"));
		List<BillDBUserLog> list = criteria.list();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	public List<Object[]> getParentItemQuantityLogs(String date) {
        Query query = session.createSQLQuery("select * from ( select * from user_log where parent_item is not null AND from_date <=:date AND to_date >=:date order by from_Date, created_Date desc) as logs group by parent_Item");
        query.setString("date", date);
        return query.list();
	}
	
	public List<Object[]> getBusinessItemQuantityLogs(String date) {
        Query query = session.createSQLQuery("select * from ( select * from user_log where item is not null AND subscription is null AND from_date <=:date AND to_date >=:date order by from_Date, created_Date desc) as logs group by item");
        query.setString("date", date);
        return query.list();
	}
	
	public List<Object[]> getSubscribedItemQuantityLogs(String date) {
        Query query = session.createSQLQuery("select * from ( select * from user_log where subscription is not null AND from_date <=:date AND to_date >=:date order by from_Date, created_Date desc) as logs group by subscription");
        query.setString("date", date);
        return query.list();
	}
	
	public List<BillDBUserLog> getLogsBetweenRange(BillDBUserLog userLog) {
		Criteria criteria = session.createCriteria(BillDBUserLog.class)
				.add(Restrictions.or
				(Restrictions.and(Restrictions.ge("fromDate", userLog.getFromDate()), Restrictions.le("fromDate", userLog.getToDate())),
				Restrictions.and(Restrictions.ge("toDate", userLog.getFromDate()), Restrictions.le("toDate", userLog.getToDate()))))
				.add(Restrictions.isNotNull("quantityChange"))
				.add(Restrictions.isNotNull("toDate"))
				.addOrder(Order.desc("fromDate"))
				.addOrder(Order.desc("createdDate"));
		if(userLog.getParentItem() != null && userLog.getParentItem().getId() != null) {
			criteria.add(Restrictions.eq("parentItem.id", userLog.getParentItem().getId()));
		}
		if(userLog.getBusiness() != null && userLog.getBusiness().getId() != null) {
			criteria.add(Restrictions.eq("business.id", userLog.getBusiness().getId()));
		}
		if(userLog.getSubscription() != null && userLog.getSubscription().getId() != null) {
			criteria.add(Restrictions.eq("subscription.id", userLog.getSubscription().getId()));
		} else {
			criteria.add(Restrictions.isNull("subscription.id"));
		}
		List<BillDBUserLog> list = criteria.list();
		return list;
	}
	
	public BillDBHoliday getHolidays(Integer month, Integer day, Date date) {
		/*Criteria criteria = session.createCriteria(BillDBHoliday.class)
				.add(Restrictions.or(Restrictions.and(Restrictions.eq("month", month), Restrictions.eq("day", day)), Restrictions.eq("date", date)));
		List<BillDBHoliday> list = criteria.list();*/
		Query query = session.createQuery("from BillDBHoliday where (month=:month AND day=:day) OR date=:date");
		query.setInteger("month", month);
		query.setInteger("day", day);
		query.setString("date", CommonUtils.getDate(date));
		List<BillDBHoliday> holidays = query.list();
		if (CollectionUtils.isEmpty(holidays)) {
			return null;
		}
		return holidays.get(0);
	}
}
