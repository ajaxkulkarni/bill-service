package com.rns.web.billapp.service.dao.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

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

	public List<BillDBInvoice> getAllInvoices(Integer subscriptionId, String status) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(invoiceNotDeleted());
		if(StringUtils.isNotBlank(status)) {
			criteria.add(Restrictions.eq("status", status));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria subscriptionCriteria = criteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN);
		Criteria businessCriteria = subscriptionCriteria.createCriteria("business", JoinType.LEFT_OUTER_JOIN);
		businessCriteria.createCriteria("user", JoinType.LEFT_OUTER_JOIN);
		Criteria sector = businessCriteria.createCriteria("sector", JoinType.LEFT_OUTER_JOIN);
		Criteria locations = businessCriteria.createCriteria("locations", JoinType.LEFT_OUTER_JOIN);
		Criteria invoiceItems = criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		Criteria businessItem = invoiceItems.createCriteria("businessItem", JoinType.LEFT_OUTER_JOIN);
		invoiceItems.createCriteria("subscribedItem", JoinType.LEFT_OUTER_JOIN);
		businessItem.createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
		return criteria.list();
	}
	
	public List<BillDBInvoice> getAllBusinessInvoices(Integer businessId, String status, BillUserLog log) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(invoiceNotDeleted()).addOrder(Order.desc("id"));
		if(log != null) {
			if(log.getFromDate() != null && log.getToDate() != null) {
				criteria.add(Restrictions.ge("invoiceDate", log.getFromDate())).add(Restrictions.le("invoiceDate", log.getToDate()));
			}
		}
		criteria.createCriteria("subscription").add(Restrictions.eq("business.id", businessId));
		if(StringUtils.isNotBlank(status)) {
			criteria.add(Restrictions.eq("status", status));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		return criteria.list();
	}

	private SimpleExpression invoiceNotDeleted() {
		return Restrictions.ne("status", BillConstants.INVOICE_STATUS_DELETED);
	}

	public List<BillDBInvoice> getAllInvoicesForMonth(Integer month, Integer year) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
								.add(Restrictions.eq("month", month))
								.add(Restrictions.eq("year", year))
								.add(invoiceNotDeleted());
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		criteria.setFetchMode("subscription", FetchMode.JOIN);
		return criteria.list();
	}
	
	public Integer getInvoiceCountByStatus(Integer subscriptionId, String status) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("status", status))
				 .add(invoiceNotDeleted());
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
	
	public BillDBInvoice getLatestUnPaidInvoice(Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.ne("status", BillConstants.INVOICE_STATUS_PAID))
				 .add(invoiceNotDeleted())
				 .add(Restrictions.ne("month", CommonUtils.getCalendarValue(new Date(), Calendar.MONTH)))
				 .addOrder(Order.desc("createdDate"));
		List list = criteria.list();
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return (BillDBInvoice) list.get(0);
	}
	
	public List<Object[]> getCustomerInvoiceSummary(Date date, Integer businessId, Integer currentMonth, Integer currentYear, CharSequence status) {
		//AND (invoice.month!=:currentMonth OR  (invoice.month=:currentMonth AND invoice.year!=:currentYear) ) 
		String queryString = "select sum(invoice.amount),invoice.subscription,sum(invoice.pendingBalance),sum(invoice.serviceCharge),sum(invoice.creditBalance) from BillDBInvoice invoice where invoice.status!=:deleted AND invoice.subscription!=:disabled AND invoice.subscription.business.id=:businessId {statusQuery} {monthQuery} group by invoice.subscription.id";
		if(currentMonth != null && currentYear != null) {
			queryString = StringUtils.replace(queryString, "{monthQuery}", " AND invoice.month=:currentMonth AND invoice.year=:currentYear");
		} else {
			queryString = StringUtils.replace(queryString, "{monthQuery}", "");
		}
		if(StringUtils.isNotBlank(status)) {
			queryString = StringUtils.replace(queryString, "{statusQuery}", " AND invoice.status!=:paid ");
		} else {
			queryString = StringUtils.replace(queryString, "{statusQuery}", "");
		}
		Query query = session.createQuery(queryString);
		if(StringUtils.isNotBlank(status)) {
			query.setString("paid", BillConstants.INVOICE_STATUS_PAID);
		}
		query.setInteger("businessId", businessId);
		if(currentMonth != null && currentYear != null) {
			query.setInteger("currentMonth", currentMonth);
			query.setInteger("currentYear", currentYear);
		}
		query.setString("deleted", BillConstants.INVOICE_STATUS_DELETED);
		query.setString("disabled", BillConstants.STATUS_DELETED);
		return query.list();
	}
	
	public List<Object[]> getCustomerOutstanding(Integer currentMonth, Integer currentYear, Integer subscriptionId) {
		Query query = session.createQuery("select sum(invoice.amount), sum(invoice.pendingBalance), sum(invoice.serviceCharge) , sum(invoice.creditBalance),invoice.subscription from BillDBInvoice invoice where invoice.status!=:paid AND invoice.status!=:deleted AND (invoice.month!=:currentMonth OR  (invoice.month=:currentMonth AND invoice.year!=:currentYear) ) AND invoice.subscription.id=:customer group by invoice.subscription.id");
		query.setString("paid", BillConstants.INVOICE_STATUS_PAID);
		query.setInteger("currentMonth", currentMonth);
		query.setInteger("currentYear", currentYear);
		query.setString("deleted", BillConstants.INVOICE_STATUS_DELETED);
		query.setInteger("customer", subscriptionId);
		return query.list();
	}
	
	public List<Object[]> getCustomerOrderSummary(Date fromDate, Date toDate) {
		Query query = session.createQuery("select sum(orders.amount),orders.subscription from BillDBOrders orders where orders.status!=:disabled AND (orders.orderDate>=:fromDate AND orders.orderDate<=:toDate) group by orders.subscription.id");
		query.setDate("fromDate", fromDate);
		query.setDate("toDate", toDate);
		query.setString("disabled", BillConstants.STATUS_DELETED);
		return query.list();
	}
	
	public List<Object[]> getCustomerOrderItemSummary(Date fromDate, Date toDate) {
		Query query = session.createQuery("select sum(orderItems.amount), sum(orderItems.quantity), orderItems, orderItems.order.subscription from BillDBOrderItems orderItems where orderItems.status!=:disabled AND orderItems.order.status!=:disabled AND orderItems.businessItem.status!=:disabled AND orderItems.subscribedItem.status!=:disabled AND (orderItems.order.orderDate>=:fromDate AND orderItems.order.orderDate<=:toDate)  group by orderItems.businessItem.id, orderItems.order.subscription.id");
		query.setDate("fromDate", fromDate);
		query.setDate("toDate", toDate);
		query.setString("disabled", BillConstants.STATUS_DELETED);
		return query.list();
	}
	
	/*public List<Object[]> getInvoiceSettlements(String settlementType) {
		Query query = session.createQuery("select sum(tx.amount),tx.business from BillDBTransactions tx where tx.status=:status AND (tx.medium=:cashfreePayment OR tx.medium) group by tx.business.id");
		query.setString("status", settlementType);
		query.setString("cashfreePayment", BillConstants.PAYMENT_MEDIUM_CASHFREE);
		query.setString("atom", BillConstants.PAYMENT_MEDIUM_ATOM);
		return query.list();
	}*/
	
	public List<BillDBTransactions> getInvoiceSettlements(String settlementType, Integer businessId) {
		String queryString = "from BillDBTransactions tx where tx.status=:status AND (tx.paymentMedium=:cashfreePayment OR tx.paymentMedium=:atom OR tx.paymentMedium=:paytm)";
		if(businessId != null) {
			queryString = queryString + " AND business.id=:businessId";
		}
		Query query = session.createQuery(queryString);
		query.setString("status", settlementType);
		query.setString("cashfreePayment", BillConstants.PAYMENT_MEDIUM_CASHFREE);
		query.setString("atom", BillConstants.PAYMENT_MEDIUM_ATOM);
		query.setString("paytm", BillConstants.PAYMENT_MEDIUM_PAYTM);
		if(businessId != null) {
			query.setInteger("businessId", businessId);
		}
		return query.list();
	}
	
	public BillDBInvoice getBusinessInvoice(Integer invoiceId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class).add(Restrictions.eq("id", invoiceId))
				 .add(invoiceNotDeleted());
		criteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		List<BillDBInvoice> list = criteria.list();
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	public List<BillDBItemInvoice> getInvoiceItems(Integer month, Integer year, Integer parentItemId, String priceType) {
		String queryString = "from BillDBItemInvoice where status=:active AND invoice.status=:pending AND invoice.month=:month AND invoice.year=:year AND quantity > 0 AND businessItem.id=:parentId";
		if(StringUtils.isNotBlank(priceType)) {
			queryString = queryString + " AND subscribedItem.price IS NOT NULL AND subscribedItem.priceType=:priceType";
		}
		Query query = session.createQuery(queryString);
		query.setInteger("month", month);
		query.setInteger("year", year);
		query.setInteger("parentId", parentItemId);
		query.setString("active", BillConstants.STATUS_ACTIVE);
		if(StringUtils.isNotBlank(priceType)) {
			query.setString("priceType", priceType);
		}
		query.setString("pending", BillConstants.INVOICE_STATUS_PENDING);
		return query.list();
	}
	
	/*public int updateSettlement(Integer businessId, String status, String oldStatus) {
		Query query = session.createQuery("update BillDBTransactions set status=:status where status=:oldStatus AND business.id=:business");
		query.setString("status", status);
		query.setString("oldStatus", oldStatus);
		query.setInteger("business", businessId);
		return query.executeUpdate();
	}*/
}
