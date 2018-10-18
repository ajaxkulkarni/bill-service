package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;

public class BillSchemesDaoImpl {

	private Session session;

	public BillSchemesDaoImpl(Session session) {
		this.session = session;
	}
	
	public BillDBCustomerCoupons getAcceptedScheme(Integer schemeId, Integer customerId) {
		Query query = session.createQuery("from BillDBCustomerCoupons where scheme.id=:schemeId AND subscription.id=:customerId");
		query.setInteger("schemeId", schemeId);
		query.setInteger("customerId", customerId);
		List<BillDBCustomerCoupons> list = query.list();
		if(CollectionUtils.isNotEmpty(list)) {
			return list.get(0);
		}
		return null;
	}
	
	public List<BillDBSchemes> getSchemes(String schemeType) {
		Criteria criteria = session.createCriteria(BillDBSchemes.class);
		criteria.add(Restrictions.eq("schemeType", schemeType));
		criteria.add(Restrictions.le("validFrom", new Date())).add(Restrictions.ge("validTill", new Date()));
		Criteria businessCriteria = criteria.createCriteria("business", JoinType.LEFT_OUTER_JOIN).setFetchMode("sector", FetchMode.JOIN);
		businessCriteria.setFetchMode("user", FetchMode.JOIN);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.addOrder(Order.desc("createdDate"));
		return criteria.list();
	}
	
}
