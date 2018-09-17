package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import javax.persistence.FetchType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SQLCriterion;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
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
				 .add(BillGenericDaoImpl.activeCriteria());
				 //.add(Restrictions.eq("subscriptions.businessItem.status", BillConstants.STATUS_ACTIVE))
				 //.add(Restrictions.eq("subscriptions.businessItem.parent.status", BillConstants.STATUS_ACTIVE));
				
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria businessCriteria = criteria.createCriteria("business").add(BillGenericDaoImpl.activeCriteria());
						
		if(businessId != null) {
			businessCriteria = businessCriteria.add(Restrictions.eq("id", businessId));
		}
		businessCriteria.createCriteria("sector", JoinType.LEFT_OUTER_JOIN);
		Criteria subscriptionCriteria = criteria.createCriteria("subscriptions", JoinType.INNER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		Criteria businessItemCriteria = subscriptionCriteria.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		
		Criteria parentItemCriteria = businessItemCriteria.createCriteria("parent", JoinType.LEFT_OUTER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		/*criteria.setFetchMode("subscriptions", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem.parent", FetchMode.JOIN);*/
		return criteria.list();
	}
	
	public List<BillDBOrders> getOrders(Date date, Integer businessId) {
		Criteria criteria = session.createCriteria(BillDBOrders.class)
				.add(Restrictions.sqlRestriction("order_Date='" + CommonUtils.convertDate(date) + "'"));
				 //.add(Restrictions.ge("orderDate", CommonUtils.startDate(date)))
				 //.add(Restrictions.le("orderDate", CommonUtils.endDate(date)));
		
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria itemCriteria = criteria.setFetchMode("orderItems", FetchMode.JOIN);
		Criteria businessItemCriteria = itemCriteria.setFetchMode("businessItem", FetchMode.JOIN);
		businessItemCriteria.setFetchMode("parent", FetchMode.JOIN);
		//Criteria businessCriteria = 
		if(businessId != null) {
			criteria.createAlias("business", "b").add(Restrictions.eq("b.id", businessId));
		}
		criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}

	public List<Object[]> getItemOrderSummary(Date date, Integer businessId) {
		Query query = session.createQuery("select sum(items.quantity),items.businessItem,items.order,sum(items.costPrice),sum(items.amount) from BillDBOrderItems items where items.order.orderDate=:date AND items.order.business.id=:businessId group by items.businessItem");
		query.setDate("date", date);
		query.setInteger("businessId", businessId);
		return query.list();
	}
	
	public BillDBItemBusiness getBusinessItemByParent(Integer parentId, Integer businessId) {
		 Criteria criteria = session.createCriteria(BillDBItemBusiness.class)
				 .add(Restrictions.eq("business.id", businessId))
				 .add(Restrictions.eq("parent.id", parentId))
				 .add(BillGenericDaoImpl.activeCriteria());
		 List<BillDBItemBusiness> list = criteria.list();
		 if(CollectionUtils.isEmpty(list)) {
			 return null;
		 }
		 return list.get(0);
	}
	
	public List<BillDBUserBusiness> getAllBusinesses() {
		Criteria criteria = session.createCriteria(BillDBUserBusiness.class).addOrder(Order.desc("createdDate"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria locationCriteria = criteria.setFetchMode("locations", FetchMode.JOIN);
		Criteria sectorCritaria = criteria.setFetchMode("sector", FetchMode.JOIN);
		Criteria userCriteria = criteria.setFetchMode("user", FetchMode.JOIN);
		return criteria.list();
	}
	
}
