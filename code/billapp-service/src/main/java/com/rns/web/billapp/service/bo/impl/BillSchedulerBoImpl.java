package com.rns.web.billapp.service.bo.impl;

import java.lang.reflect.InvocationTargetException;
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
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.rns.web.billapp.service.bo.api.BillSchedulerBo;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBHoliday;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillPayTmStatusCheck;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.BillSMSUtil;
import com.rns.web.billapp.service.util.BillUserLogUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillSchedulerBoImpl implements BillSchedulerBo, BillConstants, SchedulingConfigurer {
	
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
	
	//@Scheduled(cron = "0 14 1 * * *")
	public void calculateOrders() {
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
	
	private void calculateInvoice(Session session, Date date) throws IllegalAccessException, InvocationTargetException {
		BillVendorDaoImpl vendorDaoImpl = new BillVendorDaoImpl(session);
		LoggingUtil.logMessage("##### START OF INVOICE CALCULATION FOR - " + date + " ##########", LoggingUtil.schedulerLogger);
		//Get all the ACTIVE subscriptions with items
		List<BillDBSubscription> subscriptions = vendorDaoImpl.getDeliveries(null);
		if(CollectionUtils.isEmpty(subscriptions)) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		Integer month = CommonUtils.getCalendarValue(date, Calendar.MONTH);
		Integer year = CommonUtils.getCalendarValue(date, Calendar.YEAR);
		
		//TODO: Get holidays
		BillDBHoliday holiday = new BillLogDAOImpl(session).getHolidays(month, cal.get(Calendar.DATE), date);
		
		if(holiday != null) {
			LoggingUtil.logMessage("..... Found holiday for .." + holiday.getHolidayName() , LoggingUtil.schedulerLogger);
			for(BillDBSubscription sub: subscriptions) {
				if(StringUtils.equals(STATUS_DELETED, sub.getStatus())) {
					continue;
				}
				BillUser user = new BillUser();
				user.setCurrentBusiness(BillDataConverter.getBusiness(sub.getBusiness()));
				NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
				nullAwareBeanUtils.copyProperties(user, sub);
				user.setHoliday(holiday.getHolidayName());
				BillSMSUtil.sendSMS(user, null, MAIL_TYPE_HOLIDAY, null);
				//executor.execute(new BillMailUtil(MAIL_TYPE_HOLIDAY, user));
			}
			return;
		}
		
		List<BillUserLog> logs = getLogsForDate(session, date);
		
		List<BillDBOrders> orders = vendorDaoImpl.getOrders(date, null, null);
		
		
		//List<BillDBInvoice> invoices = new BillInvoiceDaoImpl(session).getAllInvoicesForMonth(month, year);
		
		//LoggingUtil.logMessage("Deactivating existing orders for - " + date, LoggingUtil.schedulerLogger);
		
		System.out.println("No queries now ....");
		
		for(BillDBSubscription subscription: subscriptions) {
			BillDBOrders currentOrder = findOrder(orders, subscription, date);
			//BillDBInvoice invoice = findInvoice(invoices, subscription, month, year);
			if(CollectionUtils.isEmpty(subscription.getSubscriptions())) {
				if(currentOrder.getId() != null) {
					currentOrder.setStatus(STATUS_DELETED);
					//deductInvoiceAmount(currentOrder.getAmount(), invoice);
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
				System.out.println("Item before = >" + item.getQuantity() + " .. " + item.getId());
				//BillDBItemInvoice invoiceItem = findItem(invoice, itemSub);
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
				} /*else {
					//Holiday for customer / vendor / parent Item
					//Changes made for temporary quantity change for milk vendors etc -- 22/04/19
					item.setQuantity(BillRuleEngine.getDeliveryQuantity(logs, itemSub));
				}*/ else if(!BillRuleEngine.isDelivery(logs, itemSub)) {
					//Holiday for customer / vendor / parent Item
					item.setQuantity(BigDecimal.ZERO);
				}
				if(item.getQuantity() != null && item.getQuantity().equals(BigDecimal.ZERO)) {
					noDeliveries++;
					item.setAmount(BigDecimal.ZERO);
				} else {
					BigDecimal itemPrice = calculatePrice(itemSub, cal, previousAmount, item);
					if(itemPrice != null) {
						item.setAmount(itemPrice.multiply(item.getQuantity()));
						orderTotal = orderTotal.add(itemPrice);
					}
				}
				//updateInvoiceItem(invoiceItem, invoice, item, previousAmount, previousQuantity);
				if(item.getId() == null) {
					session.persist(item);
				}
				System.out.println("Item after = >" + item.getQuantity() + " .. " + item.getId());
				/*if(invoiceItem.getId() == null) {
					session.persist(invoiceItem);
				}*/
			}
			if(noDeliveries == subscription.getSubscriptions().size()) {
				currentOrder.setStatus(STATUS_DELETED);
			} else {
				currentOrder.setStatus(STATUS_ACTIVE);
			}
			//currentOrder.setOrderItems(orderItems);
			currentOrder.setAmount(orderTotal);
			//currentOrder.setOrderItems(orderItems);
			if(currentOrder.getId() == null) {
				session.persist(currentOrder);
			} 
			/*if(invoice.getId() == null) {
				session.persist(invoice);
			}*/
			LoggingUtil.logMessage("..... Generated order for .." + subscription.getId() + " Order ID .." + currentOrder.getId() , LoggingUtil.schedulerLogger);
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
			if(orderItem.getOrder() != null && orderItem.getOrder().getId() == currentOrder.getId() && itemSub.getId() == orderItem.getSubscribedItem().getId()) {
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
	
	private BigDecimal calculatePrice(BillDBItemSubscription itemSub, Calendar cal, BigDecimal previousAmount, BillDBOrderItems item) {
		if(itemSub.getPrice() != null) {
			if(StringUtils.equals(itemSub.getPriceType(), FREQ_MONTHLY)) {
				//Only deduct FIRST DAY of each month
				Integer day = cal.get(Calendar.DAY_OF_MONTH);
				if(day == 1) {
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
					//Calculate cost price
					BigDecimal costPrice = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_WEEK), parentItem.getWeekDays(), parentItem.getWeeklyCostPrice(), parentItem.getPrice());
					if(costPrice == null) {
						if(parentItem.getCostPrice() != null && item.getQuantity() != null) {
							item.setCostPrice(parentItem.getCostPrice().multiply(item.getQuantity()));
						}
					} else {
						item.setCostPrice(costPrice.multiply(item.getQuantity()));
					}
					
					BigDecimal price = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_WEEK), parentItem.getWeekDays(), parentItem.getWeeklyPricing(), parentItem.getPrice());
					if(price != null) {
						return price;
					}
				} else if ( (StringUtils.equals(FREQ_WEEKLY, parentItem.getFrequency()) || StringUtils.equals(FREQ_MONTHLY, parentItem.getFrequency())) && StringUtils.isNotBlank(parentItem.getMonthDays()) /*&& StringUtils.isNotBlank(parentItem.getWeeklyPricing())*/) {
					//Calculate cost price
					BigDecimal costPrice = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_MONTH), parentItem.getMonthDays(), parentItem.getWeeklyCostPrice(), parentItem.getPrice());
					if(costPrice == null) {
						//Wrong logic //Do nothing here
						/*if(parentItem.getCostPrice() != null && item.getQuantity() != null) {
							item.setCostPrice(parentItem.getCostPrice().multiply(item.getQuantity()));
						}*/
					} else {
						item.setCostPrice(costPrice.multiply(item.getQuantity()));
					}
					
					BigDecimal price = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_MONTH), parentItem.getMonthDays(), parentItem.getWeeklyPricing(), parentItem.getPrice());
					if(price != null) {
						return price;
					} else {
						return BigDecimal.ZERO;
					}
				}
				
				item.setCostPrice(parentItem.getCostPrice());
				return parentItem.getPrice();
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
		if(CollectionUtils.isNotEmpty(businessItemLogs)) {
			logs.addAll(businessItemLogs);
		}
		return logs;
	}
	
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addTriggerTask(new Runnable() {
			public void run() {
				calculateOrders();
			}
		}, new Trigger() {
			public Date nextExecutionTime(TriggerContext arg0) {
				return nextOrdersExecution();
			}

			
		});

		//Paytm task to check payment status of pending invoices
		BillPayTmStatusCheck task = new BillPayTmStatusCheck();
		task.setExecutor(executor);
		task.setSessionFactory(sessionFactory);
		taskRegistrar.addFixedDelayTask(task, 60000*30);//30 minutes
		
	}
	
	private Date nextOrdersExecution() {
		Calendar cal = Calendar.getInstance();
		//if(cal.get(Calendar.HOUR_OF_DAY) > 1 && cal.get(Calendar.MINUTE) > 14) {
		cal.add(Calendar.DATE, 1);
		//}
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 15);
		LoggingUtil.logMessage("The next order generation routine is at .. " + cal.getTime(), LoggingUtil.schedulerLogger);
		return cal.getTime();
	}
	
	

}
