package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.util.BillConstants;

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
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		return criteria.list();
	}

	public List<BillDBInvoice> getAllInvoicesForMonth(Integer month, Integer year) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
								.add(Restrictions.eq("month", month))
								.add(Restrictions.eq("year", year));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}
	
	public Integer getInvoiceCountByStatus(Integer subscriptionId, String status) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("status", status));
		criteria.setProjection(Projections.rowCount());
		Long count = (Long) criteria.uniqueResult();
		if(count != null) {
			return count.intValue();
		}
		return 0;
	}
	
	public BillDBInvoice getLatestPaidInvoice(Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("status", BillConstants.INVOICE_STATUS_PAID))
				 .addOrder(Order.desc("paidDate"));
		List list = criteria.list();
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return (BillDBInvoice) list.get(0);
	}
	
	public List<Object[]> getCustomerInvoiceSummary(Date date, Integer businessId) {
		Query query = session.createQuery("select sum(invoice.amount),invoice.subscription from BillDBInvoice invoice where invoice.status!=:paid AND invoice.subscription.business.id=:businessId group by invoice.subscription.id");
		query.setString("paid", BillConstants.INVOICE_STATUS_PAID);
		query.setInteger("businessId", businessId);
		return query.list();
	}

}
