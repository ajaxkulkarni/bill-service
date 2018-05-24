package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

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
				 .add(Restrictions.eq("status", BillConstants.STATUS_ACTIVE));
				 //.add(Restrictions.eq("subscriptions.businessItem.status", BillConstants.STATUS_ACTIVE))
				 //.add(Restrictions.eq("subscriptions.businessItem.parent.status", BillConstants.STATUS_ACTIVE));
				
		Criteria businessCriteria = criteria.createCriteria("business").add(BillGenericDaoImpl.activeCriteria());
						
		if(businessId != null) {
			businessCriteria = businessCriteria.add(Restrictions.eq("id", businessId));
		}
		
		Criteria subscriptionCriteria = criteria.createCriteria("subscriptions", JoinType.INNER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		Criteria businessItemCriteria = subscriptionCriteria.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		Criteria parentItemCriteria = businessItemCriteria.createCriteria("parent", JoinType.LEFT_OUTER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		/*criteria.setFetchMode("subscriptions", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem.parent", FetchMode.JOIN);*/
		return criteria.list();
	}
	
	public List<BillDBOrders> getOrders(Date date) {
		Criteria criteria = session.createCriteria(BillDBOrders.class)
				 .add(Restrictions.ge("orderDate", DateUtils.addDays(date, -1)))
				 .add(Restrictions.le("orderDate", CommonUtils.startDate(DateUtils.addDays(date, 1))));
		criteria.setFetchMode("orderItems", FetchMode.JOIN);
		return criteria.list();
	}

}
