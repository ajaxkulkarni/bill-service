package com.rns.web.billapp.service.dao.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBBusinessInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
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
        List<BillDBInvoice> result = criteria.list();
        if(CollectionUtils.isNotEmpty(result)) {
        	return (BillDBInvoice) result.get(0);
        }
        return null;
	}
	
	public BillDBInvoice getActiveInvoiceForMonth(Integer subscriptionId, Integer month, Integer year) {
		 Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.eq("month", month))
				 .add(Restrictions.eq("year", year))
				 .add(Restrictions.ne("status", BillConstants.INVOICE_STATUS_DELETED));
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
	   criteria.setMaxResults(1);
       List<BillDBItemInvoice> result = criteria.list();
       if(CollectionUtils.isNotEmpty(result)) {
      	 return result.get(0);
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

	public List<BillDBInvoice> getAllInvoicesForMonth(Integer month, Integer year, Integer businessId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
								.add(Restrictions.eq("month", month))
								.add(Restrictions.eq("year", year))
								.add(invoiceNotDeleted());
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		criteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN).add(Restrictions.eq("business.id", businessId));
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
	
	public BillDBInvoice getLatestUnPaidInvoiceIgnoreMonth(Integer subscriptionId) {
		Criteria criteria = session.createCriteria(BillDBInvoice.class)
				 .add(Restrictions.eq("subscription.id", subscriptionId))
				 .add(Restrictions.ne("status", BillConstants.INVOICE_STATUS_PAID))
				 .add(invoiceNotDeleted())
				 .add(Restrictions.or(Restrictions.isNull("month")).
					add(Restrictions.ne("month", CommonUtils.getCalendarValue(new Date(), Calendar.MONTH))))
				 .addOrder(Order.desc("createdDate"));
		List list = criteria.list();
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return (BillDBInvoice) list.get(0);
	}
	
	public List<Object[]> getCustomerInvoiceSummary(Date date, Integer businessId, Integer currentMonth, Integer currentYear, CharSequence status, Integer groupId) {
		//AND (invoice.month!=:currentMonth OR  (invoice.month=:currentMonth AND invoice.year!=:currentYear) ) 
		String queryString = "select sum(invoice.amount),invoice.subscription,sum(invoice.pendingBalance),sum(invoice.serviceCharge),sum(invoice.creditBalance),sum(invoice.noOfReminders) from BillDBInvoice invoice where invoice.status!=:deleted AND invoice.subscription!=:disabled AND invoice.subscription.business.id=:businessId {statusQuery} {monthQuery} {groupQuery} group by invoice.subscription.id {orderQuery}";
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
		if(groupId != null) {
			queryString = StringUtils.replace(queryString, "{groupQuery}", " AND invoice.subscription.customerGroup.id=:groupId ");
			queryString = StringUtils.replace(queryString, "{orderQuery}", "order by invoice.subscription.groupSequence");
		} else {
			queryString = StringUtils.replace(queryString, "{groupQuery}", "");
			queryString = StringUtils.replace(queryString, "{orderQuery}", "order by invoice.subscription.name");
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
		if(groupId != null) {
			query.setInteger("groupId", groupId);
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
	
	public BillDBBusinessInvoice getInvoiceByDate(Date date, Integer businessId, Integer toBusinessId) {
		String queryString = "from BillDBBusinessInvoice tx where fromBusiness.id=:businessId AND toBusiness.id=:toBusinessId AND invoiceDate=:invoiceDate AND status!=:deleted)";
		if(businessId != null) {
			queryString = queryString + " AND business.id=:businessId";
		}
		Query query = session.createQuery(queryString);
		query.setString("invoiceDate", CommonUtils.convertDate(date));
		query.setInteger("businessId", businessId);
		query.setInteger("toBusinessId", toBusinessId);
		query.setString("deleted", BillConstants.INVOICE_STATUS_DELETED);
		List<BillDBBusinessInvoice> result = query.list();
		if(CollectionUtils.isEmpty(result)) {
			return null;
		}
		return result.get(0);
	}
	
	public List<BillDBBusinessInvoice> getAllPurchaseInvoices(Integer toBusinessId, String status, BillUserLog log, Integer fromBusinessId) {
		Criteria criteria = session.createCriteria(BillDBBusinessInvoice.class)
				 .add(Restrictions.eq("toBusiness.id", toBusinessId))
				 .add(Restrictions.eq("fromBusiness.id", fromBusinessId))
				 .add(invoiceNotDeleted());
		if(StringUtils.isNotBlank(status)) {
			criteria.add(Restrictions.eq("status", status));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		Criteria toBusinessCriteria = criteria.createCriteria("toBusiness", JoinType.LEFT_OUTER_JOIN);
		Criteria businessCriteria = criteria.createCriteria("fromBusiness", JoinType.LEFT_OUTER_JOIN);
		Criteria invoiceItems = criteria.createCriteria("items", JoinType.LEFT_OUTER_JOIN);
		Criteria businessItem = invoiceItems.createCriteria("fromBusinessItem", JoinType.LEFT_OUTER_JOIN);
		businessItem.createCriteria("parent", JoinType.LEFT_OUTER_JOIN);
		if(log != null) {
			criteria.add(Restrictions.ge("invoiceDate", log.getFromDate())).add(Restrictions.le("invoiceDate", log.getToDate()));
		}
		return criteria.list();
	}
	
	public Object getTotalPendingForBusiness(Integer toBusinessId, Integer fromBusinessId, BillUserLog log) {
		Criteria criteria = session.createCriteria(BillDBBusinessInvoice.class)
				 .add(Restrictions.eq("toBusiness.id", toBusinessId))
				 .add(Restrictions.eq("fromBusiness.id", fromBusinessId))
				 .add(invoiceNotDeleted()).add(Restrictions.ne("status", BillConstants.INVOICE_STATUS_PAID));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		if(log != null) {
			criteria.add(Restrictions.ge("invoiceDate", log.getFromDate())).add(Restrictions.le("invoiceDate", log.getToDate()));
		}
		criteria.setProjection(Projections.sum("amount"));
		return criteria.uniqueResult();
	}
	
	
	public int getTotalPendingCustomers(Integer businessId) {
		//AND (invoice.month!=:currentMonth OR  (invoice.month=:currentMonth AND invoice.year!=:currentYear) ) 
		/*Criteria invoiceCriteria = session.createCriteria(BillDBInvoice.class);
		invoiceCriteria.add(Restrictions.ne("status", BillConstants.INVOICE_STATUS_PAID));
		Criteria subscriptionCriteria = invoiceCriteria.createCriteria("subscription", JoinType.LEFT_OUTER_JOIN);
		subscriptionCriteria.add(Restrictions.ne("status", BillConstants.STATUS_DELETED));
		subscriptionCriteria.add(Restrictions.eq("business.id", businessId));
		//subscriptionCriteria.setProjection(Projections.groupProperty("id"));
		ProjectionList projections = Projections.projectionList();
		projections.add(Projections.groupProperty("subscription.id"));
		projections.add(Projections.rowCount());
		invoiceCriteria.setProjection(projections);
		
		
		return (Long) invoiceCriteria.uniqueResult();*/
		
		String queryString = "select count(*), invoice.subscription.id from BillDBInvoice invoice where invoice.status!=:paid AND invoice.status!=:deleted AND invoice.subscription.status!=:disabled AND invoice.subscription.business.id=:businessId group by invoice.subscription.id";
		Query query = session.createQuery(queryString);
		query.setString("paid", BillConstants.INVOICE_STATUS_PAID);
		query.setString("deleted", BillConstants.INVOICE_STATUS_DELETED);
		query.setInteger("businessId", businessId);
		query.setString("disabled", BillConstants.STATUS_DELETED);
		List<Object[]> list = query.list();
		if(CollectionUtils.isNotEmpty(list)) {
			return list.size();
		}
		return 0;
	}
	
	/*public int updateSettlement(Integer businessId, String status, String oldStatus) {
		Query query = session.createQuery("update BillDBTransactions set status=:status where status=:oldStatus AND business.id=:business");
		query.setString("status", status);
		query.setString("oldStatus", oldStatus);
		query.setInteger("business", businessId);
		return query.executeUpdate();
	}*/
	
	public List<BillUser> getCustomerPendingInvoices(String phone) {
		String queryString = "select count(*), invoice.subscription.business.name,invoice.subscription.business.id,invoice.subscription.business.user.id from BillDBInvoice invoice where invoice.status!=:paid AND invoice.status!=:deleted AND invoice.subscription.status!=:disabled AND invoice.subscription.phone=:phone group by invoice.subscription.business.id";
		Query query = session.createQuery(queryString);
		query.setString("paid", BillConstants.INVOICE_STATUS_PAID);
		query.setString("deleted", BillConstants.INVOICE_STATUS_DELETED);
		query.setString("phone", phone);
		query.setString("disabled", BillConstants.STATUS_DELETED);
		List<Object[]> list = query.list();
		if(CollectionUtils.isNotEmpty(list)) {
			List<BillUser> businesses = new ArrayList<BillUser>();
			for(Object[] objArr: list) {
				if(ArrayUtils.isNotEmpty(objArr)) {
					BillBusiness business = new BillBusiness();
					business.setName(objArr[1].toString());
					business.setId(Integer.parseInt(objArr[2].toString()));
					BillUser user = new BillUser();
					user.setCurrentBusiness(business);
					if(objArr[3] != null) {
						user.setId(Integer.parseInt(objArr[3].toString()));
					}
					businesses.add(user);
				}
			}
			return businesses;
		}
		return null;
	}
}
