package com.rns.web.billapp.service.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
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

	private SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}


}
