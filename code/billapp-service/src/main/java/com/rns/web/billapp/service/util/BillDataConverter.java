package com.rns.web.billapp.service.util;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public class BillDataConverter implements BillConstants {

	public static BillUser getUserProfile(BillServiceResponse response, Session session, BillDBUser dbUser)
			throws IllegalAccessException, InvocationTargetException {
		BillUser user = new BillUser();
		NullAwareBeanUtils nullBeans = new NullAwareBeanUtils();
		nullBeans.copyProperties(user, dbUser);
		response.setUser(user);
		List<BillDBUserBusiness> businesses = new BillVendorDaoImpl(session).getUserBusinesses(dbUser.getId());
		if (CollectionUtils.isEmpty(businesses)) {
			response.setWarningCode(WARNING_CODE_1);
			response.setWarningText(WARNING_NO_BUSINESS);
		} else {
			BillBusiness business = new BillBusiness();
			BillDBUserBusiness dbBusiness = businesses.get(0);
			business.setBusinessLocations(getLocations(new ArrayList<BillDBLocation>(dbBusiness.getLocations())));
			business.setBusinessSector(getSector(dbBusiness.getSector()));
			nullBeans.copyProperties(business, dbBusiness);
			if(StringUtils.isNotBlank(dbBusiness.getLogoImg())) {
				BillFile logo = new BillFile();
				logo.setFileName("logo");
				business.setLogo(logo);
			}
			user.setCurrentBusiness(business);
		}
		BillDBUserFinancialDetails dbFinancials = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserFinancialDetails.class, "user.id", dbUser.getId(),
				true);
		if (dbFinancials == null) {
			response.setWarningCode(WARNING_CODE_2);
			response.setWarningText(WARNING_NO_FINANCIALS);
		} else {
			BillFinancialDetails financials = new BillFinancialDetails();
			nullBeans.copyProperties(financials, dbFinancials);
			user.setFinancialDetails(financials);
		}
		return user;
	}

	public static List<BillLocation> getLocations(List<BillDBLocation> dbLocations) throws IllegalAccessException, InvocationTargetException {
		if (CollectionUtils.isEmpty(dbLocations)) {
			return null;
		}
		List<BillLocation> locations = new ArrayList<BillLocation>();
		for (BillDBLocation loc : dbLocations) {
			BillLocation location = new BillLocation();
			new NullAwareBeanUtils().copyProperties(location, loc);
			locations.add(location);
		}
		return locations;
	}

	public static List<BillItem> getItems(List<BillDBItemParent> items) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(items)) {
			return null;
		}
		List<BillItem> parentItems = new ArrayList<BillItem>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBItemParent item : items) {
			BillItem parentItem = new BillItem();
			beanUtils.copyProperties(parentItem, item);
			parentItems.add(parentItem);
		}
		return parentItems;
	}

	public static List<BillItem> getBusinessItems(List<BillDBItemBusiness> items) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(items)) {
			return null;
		}
		List<BillItem> businessItems = new ArrayList<BillItem>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBItemBusiness item : items) {
			BillItem businessItem = getBusinessItem(beanUtils, item);
			businessItems.add(businessItem);
		}
		Collections.sort(businessItems, new BillNameSorter());
		return businessItems;
	}

	public static BillItem getBusinessItem(NullAwareBeanUtils beanUtils, BillDBItemBusiness item) throws IllegalAccessException, InvocationTargetException {
		if(item == null) {
			return null;
		}
		BillItem businessItem = new BillItem();
		beanUtils.copyProperties(businessItem, item);
		if(item.getParent() != null) {
			BillItem parent = new BillItem();
			beanUtils.copyProperties(parent, item.getParent());
			businessItem.setParentItem(parent);
		}
		return businessItem;
	}

	public static List<BillUser> getCustomers(List<BillDBSubscription> customers, boolean sort) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(customers)) {
			return null;
		}
		List<BillUser> businessCustomers = new ArrayList<BillUser>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBSubscription dbCustomer : customers) {
			BillUser customer = getCustomerDetails(beanUtils, dbCustomer);
			businessCustomers.add(customer);
		}
		if(sort) {
			Collections.sort(businessCustomers, new BillNameSorter());
		}
		return businessCustomers;
	}

	public static BillUser getCustomerDetails(NullAwareBeanUtils beanUtils, BillDBSubscription dbCustomer)
			throws IllegalAccessException, InvocationTargetException {
		if(dbCustomer == null) {
			return null;
		}
		BillUser customer = new BillUser();
		BillSubscription subscription = new BillSubscription();
		beanUtils.copyProperties(subscription, dbCustomer);
		beanUtils.copyProperties(customer, dbCustomer);
		if(dbCustomer.getSubscriptions() != null) {
			subscription.setItems(getSubscribedItems(new ArrayList<BillDBItemSubscription>(dbCustomer.getSubscriptions())));
			if(dbCustomer.getLocation() != null) {
				BillLocation area = new BillLocation();
				beanUtils.copyProperties(area, dbCustomer.getLocation());
				subscription.setArea(area);
			}
		}
		customer.setCurrentSubscription(subscription);
		customer.setId(dbCustomer.getId());
		return customer;
	}
	
	public static List<BillItem> getSubscribedItems(List<BillDBItemSubscription> items) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(items)) {
			return null;
		}
		List<BillItem> subsribedItems = new ArrayList<BillItem>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBItemSubscription subscribed : items) {
			if(!StringUtils.equals(STATUS_ACTIVE, subscribed.getStatus())) {
				continue;
			}
			if(subscribed.getBusinessItem() != null) {
				if(!StringUtils.equals(STATUS_ACTIVE, subscribed.getBusinessItem().getStatus())) {
					continue;
				}
				if(subscribed.getBusinessItem().getParent() != null && !StringUtils.equals(STATUS_ACTIVE, subscribed.getBusinessItem().getParent().getStatus())) {
					continue;
				}
			}
			BillItem parentItem = new BillItem();
			BillItem item = new BillItem();
			beanUtils.copyProperties(item, subscribed);
			if(subscribed.getBusinessItem().getParent() != null) {
				beanUtils.copyProperties(parentItem, subscribed.getBusinessItem().getParent());
				item.setParentItemId(subscribed.getBusinessItem().getParent().getId());
			} else {
				beanUtils.copyProperties(parentItem, subscribed.getBusinessItem());
			}
			parentItem.setId(subscribed.getBusinessItem().getId());
			item.setParentItem(parentItem);
			item.setId(subscribed.getId());
			subsribedItems.add(item);
		}
		return subsribedItems;
	}
	
	public static List<BillInvoice> getInvoices(List<BillDBInvoice> invoices, Session session) throws IllegalAccessException, InvocationTargetException {
		List<BillInvoice> userInvoices = new ArrayList<BillInvoice>();
		if(CollectionUtils.isNotEmpty(invoices)) {
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			for(BillDBInvoice dbInvoice: invoices) {
				BillInvoice invoice = getInvoice(beanUtils, dbInvoice);
				prepareInvoiceDetails(session, beanUtils, dbInvoice, invoice);
				userInvoices.add(invoice);
			}
		}
		return userInvoices;
	}
	
	public static List<BillUser> getBusinessInvoices(List<BillDBInvoice> invoices, Session session) throws IllegalAccessException, InvocationTargetException {
		List<BillUser> userInvoices = new ArrayList<BillUser>();
		if(CollectionUtils.isNotEmpty(invoices)) {
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			for(BillDBInvoice dbInvoice: invoices) {
				BillInvoice invoice = getInvoice(beanUtils, dbInvoice);
				BillUser customer = prepareInvoiceDetails(session, beanUtils, dbInvoice, invoice);
				customer.setCurrentInvoice(invoice);
				userInvoices.add(customer);
			}
		}
		return userInvoices;
	}

	private static BillUser prepareInvoiceDetails(Session session, NullAwareBeanUtils beanUtils, BillDBInvoice dbInvoice, BillInvoice invoice)
			throws IllegalAccessException, InvocationTargetException {
		BillRuleEngine.calculatePayable(invoice, null, null);
		if(dbInvoice.getSubscription() != null && dbInvoice.getSubscription().getBusiness() != null) {
			invoice.setPaymentUrl(BillRuleEngine.preparePaymentUrl(invoice.getId()));
			BillUser customer = new BillUser();
			customer.setCurrentBusiness(getBusiness(dbInvoice.getSubscription().getBusiness()));
			beanUtils.copyProperties(customer, dbInvoice.getSubscription());
			//This is done so that result message will get to see the total amount with outstanding balance
			BillInvoice tempInvoice = new BillInvoice();
			new NullAwareBeanUtils().copyProperties(tempInvoice, invoice);
			if(StringUtils.equals(INVOICE_STATUS_PENDING, invoice.getStatus())) {
				//Only useful for pending invoices
				BillRuleEngine.calculatePayable(tempInvoice, dbInvoice, session);
			}
			invoice.setPaymentMessage(BillSMSUtil.generateResultMessage(customer, tempInvoice, BillConstants.MAIL_TYPE_INVOICE, null));
			return customer;
		}
		return null;
	}

	public static BillInvoice getInvoice(NullAwareBeanUtils beanUtils, BillDBInvoice dbInvoice) throws IllegalAccessException, InvocationTargetException {
		BillInvoice invoice = new BillInvoice();
		invoice.setInvoiceItems(new ArrayList<BillItem>());
		beanUtils.copyProperties(invoice, dbInvoice);
		if(CollectionUtils.isNotEmpty(dbInvoice.getItems())) {
			for(BillDBItemInvoice dbInvoiceItem: dbInvoice.getItems()) {
				if(StringUtils.equalsIgnoreCase(STATUS_ACTIVE, dbInvoiceItem.getStatus())) {
					BillItem invoiceItem = getInvoiceItem(beanUtils, invoice, dbInvoiceItem);
					invoice.getInvoiceItems().add(invoiceItem);
				}
			}
			
		}
		return invoice;
	}

	public static BillItem getInvoiceItem(NullAwareBeanUtils beanUtils, BillInvoice invoice, BillDBItemInvoice dbInvoiceItem)
			throws IllegalAccessException, InvocationTargetException {
		BillItem invoiceItem = new BillItem();
		beanUtils.copyProperties(invoiceItem, dbInvoiceItem);
		if(dbInvoiceItem.getBusinessItem() != null) {
			BillItem parentItem = new BillItem();
			if(dbInvoiceItem.getBusinessItem() != null && dbInvoiceItem.getBusinessItem().getParent() != null) {
				beanUtils.copyProperties(parentItem, dbInvoiceItem.getBusinessItem().getParent());
			} else {
				beanUtils.copyProperties(parentItem, dbInvoiceItem.getBusinessItem());
			}
			invoiceItem.setParentItem(parentItem);
		}
		if(dbInvoiceItem.getSubscribedItem() != null) {
			invoiceItem.setParentItemId(dbInvoiceItem.getSubscribedItem().getId());
		}
		return invoiceItem;
	}

	public static List<BillItem> getOrderItems(Set<BillDBOrderItems> items) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(items)) {
			return null;
		}
		List<BillItem> orderedItems = new ArrayList<BillItem>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBOrderItems orderItem : items) {
			if(!StringUtils.equals(STATUS_ACTIVE, orderItem.getStatus())) {
				continue;
			}
			if(orderItem.getQuantity() == null || orderItem.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			BillItem parentItem = new BillItem();
			BillItem item = new BillItem();
			beanUtils.copyProperties(item, orderItem);
			if(orderItem.getBusinessItem().getParent() != null) {
				beanUtils.copyProperties(parentItem, orderItem.getBusinessItem().getParent());
				item.setParentItemId(orderItem.getBusinessItem().getParent().getId());
			} else {
				beanUtils.copyProperties(parentItem, orderItem.getBusinessItem());
			}
			parentItem.setId(orderItem.getBusinessItem().getId());
			item.setParentItem(parentItem);
			item.setId(orderItem.getId());
			orderedItems.add(item);
		}
		return orderedItems;
	}
	
	public static BillItem getItem(BillDBItemSubscription itemSub) throws IllegalAccessException, InvocationTargetException {
		if(itemSub == null) {
			return null;
		}
		BillItem item = new BillItem();
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		if(itemSub.getBusinessItem() != null) {
			if(itemSub.getBusinessItem().getParent() != null) {
				nullAwareBeanUtils.copyProperties(item, itemSub.getBusinessItem().getParent());
				item.setParentItemId(itemSub.getBusinessItem().getParent().getId());
			}
			BillItem parentItem = new BillItem();
			nullAwareBeanUtils.copyProperties(item, itemSub.getBusinessItem());
			nullAwareBeanUtils.copyProperties(parentItem, itemSub.getBusinessItem());
			item.setParentItem(parentItem);
		} 
		nullAwareBeanUtils.copyProperties(item, itemSub);
		return item;
	}
	
	public static BillItem getItem(BillDBItemBusiness itemBusiness) throws IllegalAccessException, InvocationTargetException {
		if(itemBusiness == null) {
			return null;
		}
		BillItem item = new BillItem();
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		if(itemBusiness.getParent() != null) {
			nullAwareBeanUtils.copyProperties(item, itemBusiness.getParent());
			item.setParentItemId(itemBusiness.getParent().getId());
		}
		nullAwareBeanUtils.copyProperties(item, itemBusiness);
		return item;
	}
	
	public static BillBusiness getBusiness(BillDBUserBusiness billDBUserBusiness) throws IllegalAccessException, InvocationTargetException {
		BillBusiness business = getBusinessBasic(billDBUserBusiness);
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		if(billDBUserBusiness.getUser() != null) {
			BillUser owner = new BillUser();
			nullAwareBeanUtils.copyProperties(owner, billDBUserBusiness.getUser());
			business.setOwner(owner);
		}
		if(CollectionUtils.isNotEmpty(billDBUserBusiness.getLocations())) {
			business.setBusinessLocations(getLocations(new ArrayList<BillDBLocation>(billDBUserBusiness.getLocations())));
		}
		return business;
	}

	public static BillBusiness getBusinessBasic(BillDBUserBusiness billDBUserBusiness) throws IllegalAccessException, InvocationTargetException {
		if(billDBUserBusiness == null) {
			return null;
		}
		BillBusiness business = new BillBusiness();
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		nullAwareBeanUtils.copyProperties(business, billDBUserBusiness);
		if(StringUtils.isNotBlank(billDBUserBusiness.getLogoImg())) {
			BillFile logo = new BillFile();
			logo.setFileName("logo");
			business.setLogo(logo);
		}
		if(billDBUserBusiness.getSector() != null) {
			BillSector sector = new BillSector();
			nullAwareBeanUtils.copyProperties(sector, billDBUserBusiness.getSector());
			business.setBusinessSector(sector);	
		}
		return business;
	}

	public static void setCredentials(BillDBUser vendor, BillPaymentCredentials credentials) {
		credentials.setAccess_token(vendor.getAccessToken());
		credentials.setRefresh_token(vendor.getRefreshToken());
		credentials.setInstaId(vendor.getInstaId());
	}

	public static List<BillUserLog> formatLogs(Set<BillUserLog> logs) {
		List<BillUserLog> userLogs = new ArrayList<BillUserLog>();
		if(CollectionUtils.isNotEmpty(logs)) {
			for(BillUserLog log: logs) {
				if(!containsLog(userLogs, log)) {
					userLogs.add(log);
				}
			}
		}
		return userLogs;
	}

	private static boolean containsLog(List<BillUserLog> userLogs, BillUserLog log) {
		for(BillUserLog existing: userLogs) {
			if(DateUtils.isSameDay(existing.getFromDate(), log.getFromDate()) && DateUtils.isSameDay(existing.getToDate(), log.getToDate())) {
				if(existing.getItem() != null && existing.getItem().getName() != null && log.getItem() != null && log.getItem().getName() != null) {
					existing.getItem().setName(existing.getItem().getName() + " | " + log.getItem().getName());
				}
				return true;
			}
		}
		return false;
	}
	
	public static List<BillUser> getTransactions(List<BillDBTransactions> transactions) throws IllegalAccessException, InvocationTargetException {
		List<BillUser> users = new ArrayList<BillUser>();
		if(CollectionUtils.isNotEmpty(transactions)) {
			NullAwareBeanUtils beanutils = new NullAwareBeanUtils();
			for(BillDBTransactions txn: transactions) {
				BillUser user = new BillUser();
				if(txn.getSubscription() != null) {
					beanutils.copyProperties(user, txn.getSubscription());
				}
				BillInvoice invoice = new BillInvoice();
				if(txn.getInvoice() != null) {
					invoice.setMonth(txn.getInvoice().getMonth());
					invoice.setYear(txn.getInvoice().getYear());
					invoice.setInvoiceDate(txn.getInvoice().getInvoiceDate());
				}
				beanutils.copyProperties(invoice, txn);
				user.setCurrentInvoice(invoice);
				if(txn.getBusiness() != null) {
					user.setCurrentBusiness(BillDataConverter.getBusinessBasic(txn.getBusiness()));
				}
				users.add(user);
			}
		}
		return users;
	}
	
	public static BillScheme getScheme(BillDBSchemes schemes, BillDBCustomerCoupons coupons, NullAwareBeanUtils nullAwareBeanUtils)
			throws IllegalAccessException, InvocationTargetException {
		BillScheme pickedScheme = new BillScheme();
		nullAwareBeanUtils.copyProperties(pickedScheme, schemes);
		pickedScheme.setCouponCode(coupons.getCouponCode());
		pickedScheme.setValidTill(coupons.getValidTill());
		pickedScheme.setStatus(coupons.getStatus());
		return pickedScheme;
	}
	
	public static List<BillSector> getSectors(List<BillDBSector> dbSectors) throws IllegalAccessException, InvocationTargetException {
		List<BillSector> sectors = new ArrayList<BillSector>();
		if(CollectionUtils.isNotEmpty(dbSectors)) {
			for(BillDBSector dbSec: dbSectors) {
				BillSector sector = getSector(dbSec);
				sectors.add(sector);
			}
		}
		return sectors;
	}

	private static BillSector getSector(BillDBSector dbSec) throws IllegalAccessException, InvocationTargetException {
		BillSector sector = new BillSector();
		new NullAwareBeanUtils().copyProperties(sector, dbSec);
		return sector;
	}
	
}
