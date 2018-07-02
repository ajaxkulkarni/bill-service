package com.rns.web.billapp.service.util;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
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
			nullBeans.copyProperties(business, dbBusiness);
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

	public static List<BillUser> getCustomers(List<BillDBSubscription> customers) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(customers)) {
			return null;
		}
		List<BillUser> businessCustomers = new ArrayList<BillUser>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBSubscription dbCustomer : customers) {
			BillUser customer = getCustomerDetails(beanUtils, dbCustomer);
			businessCustomers.add(customer);
		}
		return businessCustomers;
	}

	public static BillUser getCustomerDetails(NullAwareBeanUtils beanUtils, BillDBSubscription dbCustomer)
			throws IllegalAccessException, InvocationTargetException {
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
	
	public static List<BillInvoice> getInvoices(List<BillDBInvoice> invoices) throws IllegalAccessException, InvocationTargetException {
		List<BillInvoice> userInvoices = new ArrayList<BillInvoice>();
		if(CollectionUtils.isNotEmpty(invoices)) {
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			for(BillDBInvoice dbInvoice: invoices) {
				BillInvoice invoice = getInvoice(beanUtils, dbInvoice);
				userInvoices.add(invoice);
			}
		}
		return userInvoices;
	}

	public static BillInvoice getInvoice(NullAwareBeanUtils beanUtils, BillDBInvoice dbInvoice) throws IllegalAccessException, InvocationTargetException {
		BillInvoice invoice = new BillInvoice();
		invoice.setInvoiceItems(new ArrayList<BillItem>());
		beanUtils.copyProperties(invoice, dbInvoice);
		if(CollectionUtils.isNotEmpty(dbInvoice.getItems())) {
			for(BillDBItemInvoice dbInvoiceItem: dbInvoice.getItems()) {
				if(StringUtils.equalsIgnoreCase(STATUS_ACTIVE, dbInvoiceItem.getStatus())) {
					BillItem invoiceItem = new BillItem();
					beanUtils.copyProperties(invoiceItem, dbInvoiceItem);
					BillItem parentItem = new BillItem();
					if(dbInvoiceItem.getBusinessItem().getParent() != null) {
						beanUtils.copyProperties(parentItem, dbInvoiceItem.getBusinessItem().getParent());
					} else {
						beanUtils.copyProperties(parentItem, dbInvoiceItem.getBusinessItem());
					}
					invoiceItem.setParentItem(parentItem);
					invoiceItem.setParentItemId(dbInvoiceItem.getSubscribedItem().getId());
					invoice.getInvoiceItems().add(invoiceItem);
				}
			}
			
		}
		return invoice;
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
	
	public static BillBusiness getBusiness(BillDBUserBusiness billDBUserBusiness) throws IllegalAccessException, InvocationTargetException {
		BillBusiness business = new BillBusiness();
		BillSector sector = new BillSector();
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		nullAwareBeanUtils.copyProperties(sector, billDBUserBusiness.getSector());
		BillUser owner = new BillUser();
		nullAwareBeanUtils.copyProperties(owner, billDBUserBusiness.getUser());
		business.setOwner(owner);
		business.setBusinessSector(sector);
		nullAwareBeanUtils.copyProperties(business, billDBUserBusiness);
		return business;
	}



}
