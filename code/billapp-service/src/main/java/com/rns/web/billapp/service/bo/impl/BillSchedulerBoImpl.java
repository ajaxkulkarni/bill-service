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
import com.rns.web.billapp.service.bo.util.BillRuleEngine;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.util.BillConstants;
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
	
	public void calculateInvoices(Date date) {
		Session session = null;
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
		
		List<BillDBOrders> orders = vendorDaoImpl.getOrders(date);
		
		//LoggingUtil.logMessage("Deactivating existing orders for - " + date, LoggingUtil.schedulerLogger);
		
		System.out.println("No queries now ....");
		
		for(BillDBSubscription subscription: subscriptions) {
			BillDBOrders currentOrder = findOrder(orders, subscription, date);
			if(CollectionUtils.isEmpty(subscription.getSubscriptions())) {
				if(currentOrder.getId() != null) {
					currentOrder.setStatus(STATUS_DELETED);
				}
				continue;
			}
			Integer noDeliveries = 0;
			BigDecimal orderTotal = BigDecimal.ZERO;
			for(BillDBItemSubscription itemSub: subscription.getSubscriptions()) {
				BillDBOrderItems item = findItem(currentOrder, itemSub);
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
				} else {
					BigDecimal itemPrice = calculatePrice(itemSub, cal);
					if(itemPrice != null) {
						item.setAmount(itemPrice);
						orderTotal = orderTotal.add(itemPrice);
					}
				}
				if(item.getId() == null) {
					session.persist(item);
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
			LoggingUtil.logMessage("..... Generated invoice for .." + subscription.getId() + " Invoice ID .." + currentOrder.getId(), LoggingUtil.schedulerLogger);
		}
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
	
	private BigDecimal calculatePrice(BillDBItemSubscription itemSub, Calendar cal) {
		if(itemSub.getPrice() != null) {
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
		if(CollectionUtils.isNotEmpty(parentItemLogs)) {
			logs.addAll(subscribedItemLogs);
		}
		List<BillUserLog> businessItemLogs = BillUserLogUtil.getUserLogs(new BillLogDAOImpl(session).getBusinessItemQuantityLogs(dateString));
		if(CollectionUtils.isNotEmpty(parentItemLogs)) {
			logs.addAll(businessItemLogs);
		}
		return logs;
	}
	
	

}
