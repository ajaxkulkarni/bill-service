package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.util.BillConstants;

public class BillSubscriptionDAOImpl {
	
	private Session session;
	
	public BillSubscriptionDAOImpl(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return session;
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
		criteria.createCriteria("subscriptions", JoinType.LEFT_OUTER_JOIN)/*.add(activeCriteria())*/;
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
		criteria.createCriteria("subscriptions", JoinType.LEFT_OUTER_JOIN)/*.add(activeCriteria())*/;
		return (BillDBSubscription) criteria.uniqueResult();
		
	}
	
	private SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}


}
