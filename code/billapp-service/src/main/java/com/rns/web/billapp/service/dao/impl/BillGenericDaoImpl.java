package com.rns.web.billapp.service.dao.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

public class BillGenericDaoImpl {

	private Session session;

	public BillGenericDaoImpl(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return session;
	}

	public <T> T getEntityByKey(Class<T> type, String key, Object value, boolean activeEntity) {
		Criteria criteria = session.createCriteria(type).add(Restrictions.eq(key, value));
		if (activeEntity) {
			criteria.add(activeCriteria());
		}
		Object result = criteria.uniqueResult();
		if (result != null) {
			return (T) result;
		}
		return null;
	}

	public <T> List getEntities(Class<T> type, boolean activeEntity, String sortKey, String order) {
		Criteria criteria = session.createCriteria(type);
		if (activeEntity) {
			criteria.add(activeCriteria());
		}
		addOrder(sortKey, order, criteria);
		return criteria.list();
	}

	private void addOrder(String sortKey, String order, Criteria criteria) {
		if(StringUtils.isNotBlank(sortKey) && order != null) {
			if(StringUtils.equals(order, "asc")) {
				criteria.addOrder(org.hibernate.criterion.Order.asc(sortKey));
			} else {
				criteria.addOrder(org.hibernate.criterion.Order.desc(sortKey));
			}
		}
	}

	public static SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}

	public <T> List<T> getEntitiesByKey(Class<T> type, String key, Object value, boolean activeEntity, String sortKey, String order) {
		Criteria criteria = session.createCriteria(type).add(Restrictions.eq(key, value));
		if (activeEntity) {
			criteria.add(activeCriteria());
		}
		addOrder(sortKey, order, criteria);
		return criteria.list();
	}

	public <T> Object getSum(Class<T> type, String key, Map<String, Object> restrictions, Date fromDate, Date toDate, String queryType) {
		Criteria criteria = session.createCriteria(type);
		if (restrictions != null && CollectionUtils.isNotEmpty(restrictions.entrySet())) {
			for (Entry<String, Object> e : restrictions.entrySet()) {
				criteria.add(Restrictions.eq(e.getKey(), e.getValue()));
			}
		}
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.ge("createdDate", CommonUtils.startDate(fromDate)));
			criteria.add(Restrictions.le("createdDate", CommonUtils.endDate(toDate)));
		}
		if(StringUtils.equals(queryType, "sum")) {
			criteria.setProjection(Projections.sum(key));
		} else {
			criteria.setProjection(Projections.count(key));
		}
		return criteria.uniqueResult();
	}

}
