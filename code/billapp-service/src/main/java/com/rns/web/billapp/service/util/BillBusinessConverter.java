package com.rns.web.billapp.service.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;

public class BillBusinessConverter {
	
	public static void updateBusinessDetails(BillUser user, BillGenericDaoImpl dao, BillDBUser existingUser) throws IllegalAccessException, InvocationTargetException {
		BillBusiness currentBusiness = user.getCurrentBusiness();
		if(currentBusiness != null) {
			BillDBUserBusiness billDBUserBusiness = new BillDBUserBusiness();
			if(currentBusiness.getId() != null) {
				billDBUserBusiness = dao.getEntityByKey(BillDBUserBusiness.class, "id", currentBusiness.getId(), true);
			}
			if(billDBUserBusiness == null) {
				return;
			}
			updateBillDbBusiness(user, existingUser, billDBUserBusiness);
			Session session = dao.getSession();
			if(billDBUserBusiness.getId() == null) {
				billDBUserBusiness.setStatus(BillConstants.STATUS_ACTIVE);
				billDBUserBusiness.setCreatedDate(new Date());
				if(billDBUserBusiness.getSector() != null) {
					session.persist(billDBUserBusiness);
				}
			}
			//Update business locations
			updateBusinessLocations(currentBusiness, billDBUserBusiness, session);
		}
	}

	private static void updateBusinessLocations(BillBusiness currentBusiness, BillDBUserBusiness billDBUserBusiness, Session session)
			throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isNotEmpty(currentBusiness.getBusinessLocations())) {
			billDBUserBusiness.getLocations().removeAll(billDBUserBusiness.getLocations());
			Set<BillDBLocation> locations = new HashSet<BillDBLocation>();
			NullAwareBeanUtils notNullBean = new NullAwareBeanUtils();
			for(BillLocation loc: currentBusiness.getBusinessLocations()) {
				BillDBLocation dbLoc = new BillDBLocation();
				notNullBean.copyProperties(dbLoc, loc);
				locations.add(dbLoc);
			}
			billDBUserBusiness.setLocations(locations);
		}
	}

	private static void updateBillDbBusiness(BillUser user, BillDBUser dbUser, BillDBUserBusiness billDBUserBusiness) throws IllegalAccessException, InvocationTargetException {
		NullAwareBeanUtils notNullBean = new NullAwareBeanUtils();
		BillBusiness currentBusiness = user.getCurrentBusiness();
		notNullBean.copyProperties(billDBUserBusiness, currentBusiness);
		billDBUserBusiness.setUser(dbUser);
		if(currentBusiness.getBusinessSector() != null && currentBusiness.getBusinessSector().getId() != null) {
			BillDBSector dbSector = new BillDBSector();
			dbSector.setId(currentBusiness.getBusinessSector().getId());
			billDBUserBusiness.setSector(dbSector);
		}
		
	}
	
	public static void updateBusinessItem(BillItem item, Session session, BillBusiness business) throws IllegalAccessException, InvocationTargetException {
		BeanUtilsBean notNullBean = new NullAwareBeanUtils();
		BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
		BillDBItemBusiness dbItem = dao.getEntityByKey(BillDBItemBusiness.class, BillConstants.ID_ATTR, item.getId(), true);
		if(dbItem == null) {
			dbItem = new BillDBItemBusiness();
		} 
		notNullBean.copyProperties(dbItem, item);
		if(item.getParentItem() != null) {
			BillDBItemParent parent = new BillDBItemParent();
			parent.setId(item.getParentItem().getId());
			dbItem.setParent(parent);
		}
		if(dbItem.getBusiness() == null) {
			BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
			dbBusiness.setId(business.getId());
			dbItem.setBusiness(dbBusiness);
		}
		if(dbItem.getId() == null) {
			session.persist(dbItem);
		}
	}

}
