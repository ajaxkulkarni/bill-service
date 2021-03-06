package com.rns.web.billapp.service.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.springframework.aop.config.AdvisorComponentDefinition;

import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
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
				 .add(Restrictions.eq("user.id", userId))
		 			.setFetchMode("sector", FetchMode.JOIN);
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
		
		Criteria parentItemCriteria = businessItemCriteria.createCriteria("parent", JoinType.LEFT_OUTER_JOIN).add(Restrictions.or(Restrictions.isNull("status"), BillGenericDaoImpl.activeCriteria()));
		/*criteria.setFetchMode("subscriptions", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem", FetchMode.JOIN);
		criteria.setFetchMode("subscriptions.businessItem.parent", FetchMode.JOIN);*/
		return criteria.list();
	}
	
	public List<BillDBOrders> getOrders(Date date, Integer businessId, Integer groupId) {
		Criteria criteria = session.createCriteria(BillDBOrders.class)
				.add(Restrictions.sqlRestriction("order_Date='" + CommonUtils.convertDate(date) + "'"));
				 //.add(Restrictions.ge("orderDate", CommonUtils.startDate(date)))
				 //.add(Restrictions.le("orderDate", CommonUtils.endDate(date)));
		
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria itemCriteria = criteria.createCriteria("orderItems", JoinType.LEFT_OUTER_JOIN);
		Criteria businessItemCriteria = itemCriteria.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN);
		businessItemCriteria.createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
		//Criteria businessCriteria = 
		if(businessId != null) {
			criteria.createAlias("business", "b").add(Restrictions.eq("b.id", businessId));
		}
		Criteria subscriptionCriteria = criteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN);
		if(groupId != null) {
			subscriptionCriteria.add(Restrictions.eq("customerGroup.id", groupId));
			BillGenericDaoImpl.addOrder("customerGroup.id", "asc", subscriptionCriteria);
		} else {
			BillGenericDaoImpl.addOrder("name", "asc", subscriptionCriteria);
		}
		criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}

	public List<Object[]> getItemOrderSummary(Date date, Integer businessId, Integer groupId) {
		String queryString = "select sum(items.quantity),items.businessItem,items.order,sum(items.costPrice),sum(items.amount) from BillDBOrderItems items where items.order.orderDate=:date AND items.order.business.id=:businessId {groupQuery} group by items.businessItem";
		if(groupId != null) {
			queryString = StringUtils.replace(queryString, "{groupQuery}", " AND items.order.subscription.customerGroup.id=:groupId"); 
		} else {
			queryString = StringUtils.replace(queryString, "{groupQuery}", ""); 
		}
		Query query = session.createQuery(queryString);
		query.setDate("date", date);
		query.setInteger("businessId", businessId);
		if(groupId != null) {
			query.setInteger("groupId", groupId);
		}
		return query.list();
	}
	
	public List<BillDBItemBusiness> getBusinessesByItemAccess(Integer parentItem, String access, List<BillDBLocation> locations) {
		 Criteria criteria = session.createCriteria(BillDBItemBusiness.class);
		 criteria.add(Restrictions.eq("access", access));
		 criteria.createCriteria("parent").add(Restrictions.eq("id", parentItem));
		 List<Integer> list = new ArrayList<Integer>();
		 if(CollectionUtils.isNotEmpty(locations)) {
			 for(BillDBLocation loc: locations) {
				 list.add(loc.getId());
			 }
		 }
		 Criteria businessCriteria = criteria.createCriteria("business").add(BillGenericDaoImpl.activeCriteria());
		 if(CollectionUtils.isNotEmpty(list)) {
			 businessCriteria.createCriteria("locations").add(Restrictions.in("id", list));
		 }
		 return criteria.list();
	}
	
	public List<BillDBUserBusiness> getBusinessesByType(String type, List<BillDBLocation> locations, List<BillDBItemBusiness> items) {
		Criteria criteria = session.createCriteria(BillDBUserBusiness.class);
		if(StringUtils.contains(type, "!")) {
			criteria.add(Restrictions.or(Restrictions.isNull("type"), Restrictions.ne("type", StringUtils.removeStart(type, "!"))));
		} else {
			criteria.add(Restrictions.eq("type", type));
		}
		criteria.createCriteria("user");
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<Integer> list = new ArrayList<Integer>();
		if (CollectionUtils.isNotEmpty(locations)) {
			for (BillDBLocation loc : locations) {
				list.add(loc.getId());
			}
		}
		if (CollectionUtils.isNotEmpty(list)) {
			criteria.createCriteria("locations", JoinType.LEFT_OUTER_JOIN).add(Restrictions.in("id", list));
		}
		Criteria businessItemCriteria = criteria.createCriteria("businessItems", JoinType.LEFT_OUTER_JOIN).add(BillGenericDaoImpl.activeCriteria());
		if(CollectionUtils.isNotEmpty(items)) {
			List<Integer> parentIds = new ArrayList<Integer>();
			if (CollectionUtils.isNotEmpty(items)) {
				for (BillDBItemBusiness item : items) {
					if(item.getParent() == null) {
						continue;
					}
					parentIds.add(item.getParent().getId());
				}
			}
			businessItemCriteria.createCriteria("parent", JoinType.LEFT_OUTER_JOIN).add(Restrictions.in("id", parentIds)).add(BillGenericDaoImpl.activeCriteria());
		}
		return criteria.list();
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
	
	public List<Object[]> getBillSummary(Integer businessId, Integer parentItemId, Integer month, Integer year, Integer groupId) {
		//Query query = session.createQuery("select sum(items.quantity),items.businessItem,items.order,sum(items.price),sum(items.amount) from BillDBItemInvoice items where items.invoice.month=:month AND items.order.business.id=:businessId group by items.businessItem");
		String sqlQuery = "from BillDBItemInvoice itemInvoice join itemInvoice.invoice join itemInvoice.invoice.subscription where itemInvoice.invoice.subscription.business.id=:businessId AND itemInvoice.invoice.month=:month AND itemInvoice.invoice.year=:year ";
		if(parentItemId != null) {
			sqlQuery = sqlQuery + " AND itemInvoice.businessItem.parent.id=:itemId";
		}
		if(groupId != null) {
			sqlQuery = sqlQuery + " AND itemInvoice.invoice.subscription.customerGroup.id=:groupId";
		}
		Query query = session.createQuery(sqlQuery);
		query.setInteger("businessId", businessId);
		query.setInteger("month", month);
		query.setInteger("year", year);
		if(groupId != null) {
			query.setInteger("groupId", groupId);
		}
		if(parentItemId != null) {
			query.setInteger("itemId", parentItemId);
		}
		return query.list();
	}
	
	public BillDBSubscription getCustomerByPhone(Integer businessId, String phone) {
		Criteria criteria = session.createCriteria(BillDBSubscription.class);
		criteria.add(Restrictions.eq("business.id", businessId));
		criteria.add(Restrictions.like("phone", phone, MatchMode.ANYWHERE));
		List<BillDBSubscription> list = criteria.list();
		if(CollectionUtils.isNotEmpty(list)) {
			return list.get(0);
		}
		return null;
	}
	
	public List<BillDBItemBusiness> getBusinessItems(Integer businessId) {
		 Criteria criteria = session.createCriteria(BillDBItemBusiness.class)
				 .add(Restrictions.eq("business.id", businessId))
				 .add(BillGenericDaoImpl.activeCriteria())
				 .createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
		 return criteria.list();
	}
	
}
