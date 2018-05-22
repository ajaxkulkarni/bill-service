package com.rns.web.billapp.service.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import com.rns.web.billapp.service.dao.domain.BillDBInvoice;

public class BillInvoiceDaoImpl {

	private Session session;

	public BillInvoiceDaoImpl(Session session) {
		this.session = session;
	}
	
	public BillDBInvoice getInvoiceForMonth(Integer subscriptionId, Integer month, Integer year) {
		 Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("month", month))
				 .add(Restrictions.eq("year", year));
        Object result = criteria.uniqueResult();
        if(result != null) {
       	 return (BillDBInvoice) result;
        }
        return null;
	}

}
