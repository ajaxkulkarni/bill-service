package com.rns.web.billapp.service.bo.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillSchedulerBo;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.BillUserLogUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;

public class BillSchedulerBoImpl implements BillSchedulerBo, BillConstants {
	
	private SessionFactory sessionFactory;
	private ThreadPoolTaskExecutor executor;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}
	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}
	
	
	public void calculateInvoices() {
		Session session = null;
		Date date = new Date();
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			calculateInvoice(session, date);
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			LoggingUtil.logMessage(ExceptionUtils.getStackTrace(e), LoggingUtil.schedulerLogger);
			
		} finally {
			CommonUtils.closeSession(session);
			LoggingUtil.logMessage("##### END OF INVOICE CALCULATION FOR - " + date + " ##########", LoggingUtil.schedulerLogger);
		}
	}
	
	public BillServiceResponse calculateInvoices(Date date) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			calculateInvoice(session, date);
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			LoggingUtil.logMessage(ExceptionUtils.getStackTrace(e), LoggingUtil.schedulerLogger);
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
			LoggingUtil.logMessage("##### END OF INVOICE CALCULATION FOR - " + date + " ##########", LoggingUtil.schedulerLogger);
		}
		return response;
	}
	
	private void calculateInvoice(Session session, Date date) {
		BillVendorDaoImpl vendorDaoImpl = new BillVendorDaoImpl(session);
		LoggingUtil.logMessage("##### START OF INVOICE CALCULATION FOR - " + date + " ##########", LoggingUtil.schedulerLogger);
		//Get all the ACTIVE subscriptions with items
		List<BillDBSubscription> subscriptions = vendorDaoImpl.getDeliveries(null);
		if(CollectionUtils.isEmpty(subscriptions)) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		//TODO: Get holidays
		List<BillUserLog> logs = getLogsForDate(session, date);
		
		List<BillDBOrders> orders = vendorDaoImpl.getOrders(date, null);
		
		Integer month = CommonUtils.getCalendarValue(date, Calendar.MONTH);
		Integer year = CommonUtils.getCalendarValue(date, Calendar.YEAR);
		List<BillDBInvoice> invoices = new BillInvoiceDaoImpl(session).getAllInvoicesForMonth(month, year);
		
		//LoggingUtil.logMessage("Deactivating existing orders for - " + date, LoggingUtil.schedulerLogger);
		
		System.out.println("No queries now ....");
		
		for(BillDBSubscription subscription: subscriptions) {
			BillDBOrders currentOrder = findOrder(orders, subscription, date);
			BillDBInvoice invoice = findInvoice(invoices, subscription, month, year);
			if(CollectionUtils.isEmpty(subscription.getSubscriptions())) {
				if(currentOrder.getId() != null) {
					currentOrder.setStatus(STATUS_DELETED);
					deductInvoiceAmount(currentOrder.getAmount(), invoice);
					currentOrder.setAmount(BigDecimal.ZERO);
				}
				continue;
			}
			Integer noDeliveries = 0;
			BigDecimal orderTotal = BigDecimal.ZERO;
			for(BillDBItemSubscription itemSub: subscription.getSubscriptions()) {
				BillDBOrderItems item = findItem(currentOrder, itemSub);
				BigDecimal previousQuantity = item.getQuantity();
				BigDecimal previousAmount = item.getAmount();
				
				BillDBItemInvoice invoiceItem = findItem(invoice, itemSub);
				item.setQuantity(itemSub.getQuantity());
				if(itemSub.getQuantity() == null || itemSub.getQuantity().equals(BigDecimal.ZERO)) {
					item.setQuantity(BigDecimal.ZERO);
				} else if(weekDayNotPresent(cal, itemSub.getWeekDays())) {
					//Not a delivery day for the customer
					item.setQuantity(BigDecimal.ZERO);
				} else if(itemSub.getBusinessItem() != null && weekDayNotPresent(cal, itemSub.getBusinessItem().getWeekDays())) {
					//Not a delivery day for the vendor
					item.setQuantity(BigDecimal.ZERO);
				} else if(itemSub.getBusinessItem() != null && itemSub.getBusinessItem().getParent() != null && weekDayNotPresent(cal, itemSub.getBusinessItem().getParent().getWeekDays())) {
					//Not a delivery day for the parent item
					item.setQuantity(BigDecimal.ZERO);
				} else if(!BillRuleEngine.isDelivery(logs, itemSub)) {
					//Holiday for customer / vendor / parent Item
					item.setQuantity(BigDecimal.ZERO);
				}
				if(item.getQuantity() != null && item.getQuantity().equals(BigDecimal.ZERO)) {
					noDeliveries++;
					item.setAmount(BigDecimal.ZERO);
				} else {
					BigDecimal itemPrice = calculatePrice(itemSub, cal, previousAmount);
					if(itemPrice != null) {
						item.setAmount(itemPrice);
						orderTotal = orderTotal.add(itemPrice);
					}
				}
				updateInvoiceItem(invoiceItem, invoice, item, previousAmount, previousQuantity);
				if(item.getId() == null) {
					session.persist(item);
				}
				if(invoiceItem.getId() == null) {
					session.persist(invoiceItem);
				}
			}
			if(noDeliveries == subscription.getSubscriptions().size()) {
				currentOrder.setStatus(STATUS_DELETED);
			} else {
				currentOrder.setStatus(STATUS_ACTIVE);
			}
			currentOrder.setAmount(orderTotal);
			if(currentOrder.getId() == null) {
				session.persist(currentOrder);
			}
			if(invoice.getId() == null) {
				session.persist(invoice);
			}
			LoggingUtil.logMessage("..... Generated invoice for .." + subscription.getId() + " Order ID .." + currentOrder.getId() + " .. Invoice ID " +  invoice.getId(), LoggingUtil.schedulerLogger);
		}
	}
	private void updateInvoiceItem(BillDBItemInvoice invoiceItem, BillDBInvoice invoice, BillDBOrderItems item, BigDecimal previousAmount, BigDecimal previousQuantity) {
		if(invoiceItem.getPrice() != null && invoiceItem.getQuantity() != null) {
			if(previousAmount != null && previousQuantity != null) {
				//Means this order was generated before.. so undo previous calculations
				invoiceItem.setPrice(invoiceItem.getPrice().subtract(previousAmount));
				invoiceItem.setQuantity(invoiceItem.getQuantity().subtract(previousQuantity));
				invoice.setAmount(invoice.getAmount().subtract(previousAmount));
			}
		}
		if(item.getQuantity() != null) {
			if(invoiceItem.getQuantity() == null) {
				invoiceItem.setQuantity(BigDecimal.ZERO);
			}
			invoiceItem.setQuantity(invoiceItem.getQuantity().add(item.getQuantity()));
		}
		if(item.getAmount() != null) {
			if(invoiceItem.getPrice() == null) {
				invoiceItem.setPrice(BigDecimal.ZERO);
			}
			if(invoice.getAmount() == null) {
				invoice.setAmount(BigDecimal.ZERO);
			}
			invoiceItem.setPrice(invoiceItem.getPrice().add(item.getAmount()));
			invoice.setAmount(invoice.getAmount().add(item.getAmount()));
		}
	}
	private BillDBItemInvoice findItem(BillDBInvoice invoice, BillDBItemSubscription itemSub) {
		BillDBItemInvoice item = new BillDBItemInvoice();
		item.setCreatedDate(new Date());
		item.setStatus(STATUS_ACTIVE);
		item.setBusinessItem(itemSub.getBusinessItem());
		item.setSubscribedItem(itemSub);
		item.setInvoice(invoice);
		if(CollectionUtils.isEmpty(invoice.getItems())) {
			return item;
		}
		for(BillDBItemInvoice itemInvoice: invoice.getItems()) {
			if(itemSub.getId() != null && itemInvoice.getSubscribedItem() != null && itemInvoice.getSubscribedItem().getId() == itemSub.getId()) {
				return itemInvoice;
			}
		}
		return item;
	}
	private void deductInvoiceAmount(BigDecimal amount, BillDBInvoice invoice) {
		if(invoice.getAmount() != null) {
			if(amount == null) {
				return;
			}
			//Means this order was generated before.. so undo previous calculations
			invoice.setAmount(invoice.getAmount().subtract(amount));
		}
	}
	
	private BillDBInvoice findInvoice(List<BillDBInvoice> invoices, BillDBSubscription subscription, Integer month, Integer year) {
		BillDBInvoice invoice = new BillDBInvoice();
		invoice.setCreatedDate(new Date());
		invoice.setStatus(INVOICE_STATUS_PENDING);
		invoice.setMonth(month);
		invoice.setYear(year);
		invoice.setSubscription(subscription);
		if(CollectionUtils.isEmpty(invoices)) {
			return invoice;
		}
		for(BillDBInvoice inv: invoices) {
			if(subscription.getId() != null && inv.getSubscription().getId() == subscription.getId()) {
				return inv;
			}
		}
		return invoice;
	}
	private BillDBOrderItems findItem(BillDBOrders currentOrder, BillDBItemSubscription itemSub) {
		BillDBOrderItems item = new BillDBOrderItems();
		item.setCreatedDate(new Date());
		item.setStatus(STATUS_ACTIVE);
		item.setOrder(currentOrder);
		item.setBusinessItem(itemSub.getBusinessItem());
		item.setSubscribedItem(itemSub);
		if(CollectionUtils.isEmpty(currentOrder.getOrderItems())) {
			return item;
		}
		for(BillDBOrderItems orderItem: currentOrder.getOrderItems()) {
			if(orderItem.getOrder() != null && orderItem.getOrder().getId() == currentOrder.getId()) {
				return orderItem;
			}
		}
		return item;
	}
	private BillDBOrders findOrder(List<BillDBOrders> orders, BillDBSubscription subscription, Date date) {
		BillDBOrders billDBOrders = new BillDBOrders();
		billDBOrders.setSubscription(subscription);
		billDBOrders.setBusiness(subscription.getBusiness());
		billDBOrders.setCreatedDate(new Date());
		billDBOrders.setOrderDate(date);
		billDBOrders.setStatus(STATUS_ACTIVE);
		if(CollectionUtils.isEmpty(orders)) {
			return billDBOrders;
		}
		for(BillDBOrders order: orders) {
			if(order.getSubscription() != null && order.getSubscription().getId() == subscription.getId() && DateUtils.isSameDay(date, order.getOrderDate())) {
				return order;
			}
		}
		return billDBOrders;
	}
	
	private BigDecimal calculatePrice(BillDBItemSubscription itemSub, Calendar cal, BigDecimal previousAmount) {
		if(itemSub.getPrice() != null) {
			if(StringUtils.equals(itemSub.getPriceType(), FREQ_MONTHLY)) {
				//Only deduct ONCE each month
				if(previousAmount == null || previousAmount.compareTo(BigDecimal.ZERO) == 0) {
					return itemSub.getPrice();
				} else {
					return BigDecimal.ZERO;
				}
			}
			return itemSub.getPrice();
		}
		BillDBItemBusiness businessItem = itemSub.getBusinessItem();
		if(businessItem != null) {
			if(businessItem.getPrice() != null) {
				return businessItem.getPrice();
			}
			BillDBItemParent parentItem = businessItem.getParent();
			if(parentItem != null) {
				if(StringUtils.equals(FREQ_DAILY, parentItem.getFrequency()) && StringUtils.isNotBlank(parentItem.getWeekDays()) && StringUtils.isNotBlank(parentItem.getWeeklyPricing())) {
					BigDecimal price = calculatePricing(cal.get(Calendar.DAY_OF_WEEK), parentItem.getWeekDays(), parentItem.getWeeklyPricing());
					if(price != null) {
						return price;
					}
				} else if ( (StringUtils.equals(FREQ_WEEKLY, parentItem.getFrequency()) || StringUtils.equals(FREQ_MONTHLY, parentItem.getFrequency())) && StringUtils.isNotBlank(parentItem.getMonthDays()) && StringUtils.isNotBlank(parentItem.getWeeklyPricing())) {
					BigDecimal price = calculatePricing(cal.get(Calendar.DAY_OF_MONTH), parentItem.getMonthDays(), parentItem.getWeeklyPricing());
					if(price != null) {
						return price;
					}
				}
				return parentItem.getPrice();
			}
		}
		return null;
	}
	private BigDecimal calculatePricing(Integer day, String weekDaysString, String pricingString) {
		String[] weekdays = StringUtils.split(weekDaysString, ",");
		String[] pricing = StringUtils.split(pricingString, ",");
		if(ArrayUtils.isNotEmpty(weekdays)) {
			for(int i = 0; i < weekdays.length; i++) {
				if(day == Integer.parseInt(weekdays[i])) {
					if(pricing.length > i) {
						return new BigDecimal(pricing[i]);
					} else {
						return new BigDecimal(pricing[0]);
					}
				}
			}
		}
		return null;
	}
	private boolean weekDayNotPresent(Calendar cal, String weekDays) {
		return StringUtils.isNotEmpty(weekDays) && !StringUtils.contains(weekDays, String.valueOf(cal.get(Calendar.DAY_OF_WEEK)));
	}
	
	private List<BillUserLog> getLogsForDate(Session session, Date date) {
		List<BillUserLog> logs = new ArrayList<BillUserLog>();
		String dateString = CommonUtils.getDate(date);
		List<BillUserLog> parentItemLogs = BillUserLogUtil.getUserLogs(new BillLogDAOImpl(session).getParentItemQuantityLogs(dateString));
		if(CollectionUtils.isNotEmpty(parentItemLogs)) {
			logs.addAll(parentItemLogs);
		}
		List<BillUserLog> subscribedItemLogs = BillUserLogUtil.getUserLogs(new BillLogDAOImpl(session).getSubscribedItemQuantityLogs(dateString));
		if(CollectionUtils.isNotEmpty(subscribedItemLogs)) {
			logs.addAll(subscribedItemLogs);
		}
		List<BillUserLog> businessItemLogs = BillUserLogUtil.getUserLogs(new BillLogDAOImpl(session).getBusinessItemQuantityLogs(dateString));
		if(CollectionUtils.isNotEmpty(parentItemLogs)) {
			logs.addAll(businessItemLogs);
		}
		return logs;
	}
	
	

}
