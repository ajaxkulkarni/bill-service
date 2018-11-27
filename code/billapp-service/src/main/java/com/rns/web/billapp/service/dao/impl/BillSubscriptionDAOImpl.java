package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

public class BillSubscriptionDAOImpl {
	
	private Session session;
	
	public BillSubscriptionDAOImpl(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}
	
	public BillDBSubscription getActiveSubscription(String phone, Integer businessItemId) {
		 Criteria criteria = session.createCriteria(BillDBSubscription.class)
				 .add(Restrictions.eq("phone", phone))
				 .add(Restrictions.eq("business.id", businessItemId));
        Object result = criteria.uniqueResult();
        if(result != null) {
       	 return (BillDBSubscription) result;
        }
        return null;
	}
	
	public BillDBItemSubscription getActiveItemSubscription(Integer subscriptionId, Integer businessItemId) {
		 Criteria criteria = session.createCriteria(BillDBItemSubscription.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("businessItem.id", businessItemId));
         Object result = criteria.uniqueResult();
         if(result != null) {
        	 return (BillDBItemSubscription) result;
         }
         return null;
	}
	
	public List<BillDBSubscription> getBusinessSubscriptions(Integer businessId) {
		Criteria criteria = session.createCriteria(BillDBSubscription.class)
				 .add(activeCriteria());
		criteria.createCriteria("business").add(activeCriteria()).add(Restrictions.eq("id", businessId));
		Criteria location = criteria.createCriteria("location", JoinType.LEFT_OUTER_JOIN);
		Criteria subscribed = criteria.createCriteria("subscriptions", JoinType.LEFT_OUTER_JOIN)/*.add(activeCriteria())*/;
		Criteria businessItem = subscribed.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN);
		Criteria parentItem = businessItem.createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
		//criteria.setFetchMode("subscriptions", FetchMode.EAGER);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<BillDBSubscription> subscriptions = criteria.list();
		if(CollectionUtils.isEmpty(subscriptions)) {
			return null;
		}
		return subscriptions;
		
	}

	public BillDBSubscription getSubscriptionDetails(Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBSubscription.class)
				 .add(Restrictions.eq("id", subscriptionId))
				 .add(activeCriteria());
		criteria.setFetchMode("location", FetchMode.JOIN);
		criteria.setFetchMode("business", FetchMode.JOIN);
		criteria.createCriteria("subscriptions", JoinType.LEFT_OUTER_JOIN)/*.add(activeCriteria())*/;
		return (BillDBSubscription) criteria.uniqueResult();
		
	}
	
	private SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}
	
	public List<BillDBOrders> getOrders(Date fromDate, Date toDate, Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBOrders.class)
				.add(Restrictions.ge("orderDate", fromDate))
				.add(Restrictions.le("orderDate", toDate))
				.add(BillGenericDaoImpl.activeCriteria());
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria itemCriteria = criteria.setFetchMode("orderItems", FetchMode.JOIN);
		Criteria businessItemCriteria = itemCriteria.setFetchMode("businessItem", FetchMode.JOIN);
		businessItemCriteria.setFetchMode("parent", FetchMode.JOIN);
		//Criteria businessCriteria = 
		if(subscriptionId != null) {
			criteria.createAlias("subscription", "s").add(Restrictions.eq("s.id", subscriptionId));
		}
		criteria.addOrder(Order.desc("orderDate"));
		//criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}
	
}
