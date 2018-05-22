package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.util.BillConstants;

public class BillVendorDaoImpl {

	private Session session;

	public BillVendorDaoImpl(Session session) {
		this.session = session;
	}
	
	public List<BillDBUserBusiness> getUserBusinesses(Integer userId) {
		 Criteria criteria = session.createCriteria(BillDBUserBusiness.class)
				 .add(Restrictions.eq("user.id", userId));
        return criteria.list();
	}
	
	public List<BillDBSubscription> getDeliveries(Integer businessId) {
		Criteria criteria = session.createCriteria(BillDBSubscription.class)
				 .add(Restrictions.eq("business.id", businessId))
				 .add(Restrictions.eq("status", BillConstants.STATUS_ACTIVE));
		criteria.setFetchMode("subscriptions", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem.parent", FetchMode.JOIN);
		return criteria.list();
	}

}
