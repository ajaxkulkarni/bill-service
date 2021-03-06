package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Distinct;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.DistinctResultTransformer;

import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.util.BillConstants;

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
        List<BillDBSubscription> result = criteria.list();
        if(CollectionUtils.isNotEmpty(result)) {
       	 return result.get(0);
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
	
	public List<BillDBSubscription> getBusinessSubscriptions(Integer businessId, Integer groupId) {
		Criteria criteria = session.createCriteria(BillDBSubscription.class)
				 .add(activeCriteria());
		criteria.createCriteria("business").add(activeCriteria()).add(Restrictions.eq("id", businessId));
		if(groupId != null) {
			criteria.add(Restrictions.eq("customerGroup.id", groupId));
			BillGenericDaoImpl.addOrder("groupSequence", "asc", criteria);
		} else {
			BillGenericDaoImpl.addOrder("name", "asc", criteria);
		}
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
		Criteria subscriptions = criteria.createCriteria("subscriptions", JoinType.LEFT_OUTER_JOIN)/*.add(activeCriteria())*/;
		Criteria businessItem = subscriptions.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN);
		businessItem.createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
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
	
	public List<Object[]> getOnlinePayers(Date fromDate, Date toDate) {
		Criteria criteria = session.createCriteria(BillDBTransactions.class)
				.add(Restrictions.or(Restrictions.eq("status", BillConstants.INVOICE_SETTLEMENT_STATUS_SETTLED), Restrictions.eq("status", BillConstants.INVOICE_STATUS_PAID)))
				.add(Restrictions.or(Restrictions.eqOrIsNull("paymentMedium", BillConstants.PAYMENT_MEDIUM_CASHFREE), Restrictions.eqOrIsNull("paymentMedium", BillConstants.PAYMENT_MEDIUM_ATOM), Restrictions.eqOrIsNull("paymentMedium", BillConstants.PAYMENT_MEDIUM_PAYTM)));
		if(fromDate != null && toDate != null) {
			criteria.add(Restrictions.ge("createdDate", fromDate))
					.add(Restrictions.le("createdDate", toDate));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria subscriptionCriteria = criteria.createCriteria("subscription", "sub");
		criteria.createAlias("invoice", "inv");
		subscriptionCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		//subscriptionCriteria.add(DistinctResultTransformer.INSTANCE)
		//criteria.setProjection(Projections.groupProperty("subscription.id"));
		ProjectionList projectionList = Projections.projectionList();
		//projectionList.add(Projections.property("id"));
		projectionList.add(Projections.property("sub.phone"));
		projectionList.add(Projections.property("sub.name"));
		projectionList.add(Projections.property("sub.email"));
		projectionList.add(Projections.property("inv.id"));
		projectionList.add(Projections.groupProperty("sub.id"));
		/*Projection mainProjectionList = Projections.projectionList().add(Projections.distinct(projectionList))
				.add(Projections.property("inv.id"));*/
		
		criteria.setProjection(projectionList);
		//criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}
	
}
