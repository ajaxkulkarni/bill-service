package com.rns.web.billapp.service.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;

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
	
	public BillDBItemInvoice getInvoiceItem(Integer invoiceId, Integer subscribedItemId) {
		 Criteria criteria = session.createCriteria(BillDBItemInvoice.class)
				 .add(Restrictions.eq("invoice.id", invoiceId))
				 .add(Restrictions.eq("subscribedItem.id", subscribedItemId));
       Object result = criteria.uniqueResult();
       if(result != null) {
      	 return (BillDBItemInvoice) result;
       }
       return null;
	}

	public List<BillDBInvoice> getAllInvoices(Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId));
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		return criteria.list();
	}

}
