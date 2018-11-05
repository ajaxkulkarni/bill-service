package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;

public class BillAdminDaoImpl {

	private Session session;
	
	public BillAdminDaoImpl(Session session) {
		this.session = session;
	}

	public List<BillDBUserBusiness> getBusinesses(Integer sectorId) {
		Criteria criteria = session.createCriteria(BillDBUserBusiness.class);
		criteria.createCriteria("user", JoinType.INNER_JOIN);
		Criteria sectorCriteria = criteria.createCriteria("sector", JoinType.LEFT_OUTER_JOIN);
		if(sectorId != null) {
			sectorCriteria.add(Restrictions.eq("id", sectorId));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return criteria.list();
	}

}
