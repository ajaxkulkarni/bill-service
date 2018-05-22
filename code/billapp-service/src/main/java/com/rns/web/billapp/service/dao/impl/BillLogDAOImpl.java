package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
import com.rns.web.billapp.service.util.BillConstants;

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

}
