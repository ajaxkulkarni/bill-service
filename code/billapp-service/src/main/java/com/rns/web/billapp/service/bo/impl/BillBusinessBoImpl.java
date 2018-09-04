package com.rns.web.billapp.service.bo.impl;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillBusinessBo;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillBusinessBoImpl implements BillBusinessBo, BillConstants {
	
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


	public BillServiceResponse updateScheme(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillScheme scheme = request.getScheme();
		if(request.getBusiness() == null || scheme == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
	
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillDBSchemes dbScheme = null;
			BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
			if(scheme.getId() != null) {
				dbScheme = billGenericDaoImpl.getEntityByKey(BillDBSchemes.class, ID_ATTR, request.getBusiness().getId(), true);
				if(dbScheme == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_FOUND);
					return response;
				}
			} else {
				dbScheme = new BillDBSchemes();
				dbScheme.setStatus(STATUS_ACTIVE);
				dbScheme.setCreatedDate(new Date());
			}
			new NullAwareBeanUtils().copyProperties(dbScheme, scheme);
			BillDBUserBusiness dbBusiness = null;
			if(dbScheme.getBusiness() == null) {
				dbBusiness = billGenericDaoImpl.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), true);
				if(dbBusiness == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_USER);
					return response;
				}
				dbScheme.setBusiness(dbBusiness);
			}
			if(scheme.getItemParent() != null) {
				BillDBItemBusiness itemBusiness = new BillVendorDaoImpl(session).getBusinessItemByParent(scheme.getItemParent().getId(), request.getBusiness().getId());
				if(itemBusiness == null || !StringUtils.equals(itemBusiness.getAccess(), ACCESS_ADMIN)) {
					//Parent item changing access not granted
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCESS_DENIED);
					return response;
				}
				BillDBItemParent parentItem = billGenericDaoImpl.getEntityByKey(BillDBItemParent.class, ID_ATTR, scheme.getItemParent().getId(), true);
				dbScheme.setParentItem(parentItem);
			}
			if(request.getBusiness().getBusinessSector() != null) {
				BillDBSector sector = billGenericDaoImpl.getEntityByKey(BillDBSector.class, ID_ATTR, request.getBusiness().getBusinessSector().getId(), true);
				dbScheme.setSector(sector);
			} else if (dbScheme.getSector() == null && dbBusiness != null) {
				dbScheme.setSector(dbBusiness.getSector());
			}
			if(dbScheme.getId() == null) {
				session.persist(dbScheme);
			}
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		
		return response;
	}

	public BillServiceResponse applyCoupon(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public BillServiceResponse getAllSchemes(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public BillServiceResponse getAllVendors(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
