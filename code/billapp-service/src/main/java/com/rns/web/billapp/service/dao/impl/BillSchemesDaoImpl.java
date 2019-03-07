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
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

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
	
	public BillDBCustomerCoupons getAcceptedScheme(Integer schemeId, Integer fromBusiness, Integer toBusiness) {
		Query query = session.createQuery("from BillDBCustomerCoupons where scheme.id=:schemeId AND business.id=:fromBusiness AND acceptedBy.id=:toBusiness");
		query.setInteger("schemeId", schemeId);
		query.setInteger("fromBusiness", fromBusiness);
		query.setInteger("toBusiness", toBusiness);
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

	public List<BillDBCustomerCoupons> getReferrals(Integer businessId) {
		Criteria criteria = session.createCriteria(BillDBCustomerCoupons.class);
		criteria.add(BillGenericDaoImpl.activeCriteria());
		Date currentDate = CommonUtils.setZero(new Date());
		criteria.add(Restrictions.le("validFrom", currentDate));
		criteria.add(Restrictions.ge("validTill", currentDate));
		criteria.add(Restrictions.eq("acceptedBy.id", businessId));
		Criteria schemeCriteria = criteria.createCriteria("scheme", JoinType.LEFT_OUTER_JOIN);
		schemeCriteria.add(Restrictions.eq("schemeType", BillConstants.SCHEME_TYPE_REFERRAL));
		schemeCriteria.add(Restrictions.le("validFrom", currentDate));
		schemeCriteria.add(Restrictions.ge("validTill", currentDate));
		schemeCriteria.add(BillGenericDaoImpl.activeCriteria());
		return criteria.list();
	}
	
}
