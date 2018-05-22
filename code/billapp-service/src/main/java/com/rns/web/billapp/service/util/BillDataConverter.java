package com.rns.web.billapp.service.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
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
			BillItem businessItem = new BillItem();
			beanUtils.copyProperties(businessItem, item);
			if(item.getParent() != null) {
				BillItem parent = new BillItem();
				beanUtils.copyProperties(parent, item.getParent());
				businessItem.setParentItem(parent);
			}
			businessItems.add(businessItem);
		}
		return businessItems;
	}

	public static List<BillUser> getCustomers(List<BillDBSubscription> customers) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(customers)) {
			return null;
		}
		List<BillUser> businessCustomers = new ArrayList<BillUser>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBSubscription dbCustomer : customers) {
			BillUser customer = new BillUser();
			beanUtils.copyProperties(customer, dbCustomer);
			businessCustomers.add(customer);
		}
		return businessCustomers;
	}
	
	public static List<BillItem> getSubscribedItems(List<BillDBItemSubscription> items) throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(items)) {
			return null;
		}
		List<BillItem> subsribedItems = new ArrayList<BillItem>();
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		for (BillDBItemSubscription subscribed : items) {
			BillItem parentItem = new BillItem();
			BillItem item = new BillItem();
			beanUtils.copyProperties(item, subscribed);
			if(subscribed.getBusinessItem().getParent() != null) {
				beanUtils.copyProperties(item, subscribed.getBusinessItem().getParent());
				item.setParentItemId(subscribed.getBusinessItem().getParent().getId());
			} else {
				beanUtils.copyProperties(item, subscribed.getBusinessItem());
			}
			parentItem.setId(subscribed.getBusinessItem().getId());
			item.setParentItem(parentItem);
			item.setId(subscribed.getId());
			subsribedItems.add(item);
		}
		return subsribedItems;
	}

}
