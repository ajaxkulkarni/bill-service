package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBTransactions;

public class BillTransactionsDaoImpl {

	private Session session;
	
	public BillTransactionsDaoImpl(Session session) {
		this.session = session;
	}

	public List<BillDBTransactions> getTransactions() {
		Criteria criteria = session.createCriteria(BillDBTransactions.class);
		criteria.createCriteria("invoice", JoinType.LEFT_OUTER_JOIN);
		criteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN);
		criteria.createCriteria("business", JoinType.LEFT_OUTER_JOIN).setFetchMode("sector", FetchMode.JOIN);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.addOrder(Order.desc("id"));
		return criteria.list();
	}

}
