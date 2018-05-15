package com.rns.web.billapp.service.bo.impl;


import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillUserBo;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillUserLogUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillUserBoImpl implements BillUserBo, BillConstants {
	
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

	public BillServiceResponse updateUserInfo(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser user = request.getUser();
		if(user == null || StringUtils.isBlank(user.getPhone())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser existingUser = dao.getEntityByKey(BillDBUser.class, USER_DB_ATTR_PHONE, user.getPhone(), false);
			if(user.getId() == null) {
				if(existingUser == null) {
					BillDBUser dbUser = new BillDBUser();
					BeanUtils.copyProperties(dbUser, user);
					dbUser.setCreatedDate(new Date());
					dbUser.setStatus(STATUS_PENDING);
					session.persist(dbUser);
					updateUserFiles(user, dbUser);
					BillBusinessConverter.updateBusinessDetails(user, dao, existingUser);
				} else {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_MOBILE_PRESENT);
				}
			} else {
				BeanUtilsBean notNullBean = new NullAwareBeanUtils();
				
				if(existingUser != null) {
					notNullBean.copyProperties(existingUser, user);
					updateUserFiles(user, existingUser);
					BillBusinessConverter.updateBusinessDetails(user, dao, existingUser);
				} else {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_USER);
				}
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

	private void updateUserFiles(BillUser user, BillDBUser dbUser) throws IOException {
		String folderPath = ROOT_FOLDER_LOCATION + dbUser.getId() + "/";
		File folderLocation = new File(folderPath);
		if(!folderLocation.exists()) {
			folderLocation.mkdirs();
		}
		if(user.getPanFile() != null) {
			String panFilePath = folderPath + user.getPanFile().getFilePath();
			CommonUtils.writeToFile(user.getPanFile().getFileData(), panFilePath);
			dbUser.setPanFilePath(panFilePath);
		}
		if(user.getAadharFile() != null) {
			String aadharFilePath = folderPath + user.getAadharFile().getFilePath();
			CommonUtils.writeToFile(user.getAadharFile().getFileData(), aadharFilePath);
			dbUser.setAadharFilePath(aadharFilePath);
		}
	}
	
	public BillServiceResponse updateUserFinancialInfo(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser user = request.getUser();
		if(user == null || StringUtils.isBlank(user.getPhone()) || user.getFinancialDetails() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BeanUtilsBean notNullBean = new NullAwareBeanUtils();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser existingUser = dao.getEntityByKey(BillDBUser.class, USER_DB_ATTR_PHONE, user.getPhone(), true);
			if(existingUser != null) {
				BillDBUserFinancialDetails dbFinancial = null;
				if(user.getFinancialDetails().getId() != null) {
					dbFinancial = dao.getEntityByKey(BillDBUserFinancialDetails.class, ID_ATTR, user.getFinancialDetails().getId(), true);
				}
				if(dbFinancial == null) {
					dbFinancial = new BillDBUserFinancialDetails();
					dbFinancial.setUser(existingUser);
					dbFinancial.setStatus(STATUS_ACTIVE);
					dbFinancial.setCreatedDate(new Date());
				}
				notNullBean.copyProperties(dbFinancial, user.getFinancialDetails());
				if(dbFinancial.getId() == null) {
					session.persist(dbFinancial);
				}
			} else {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_USER);
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

	public BillServiceResponse updateBusinessItem(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillItem item = request.getItem();
		if(request.getBusiness() == null || request.getBusiness().getId() == null || item == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillBusinessConverter.updateBusinessItem(item, session, request.getBusiness());
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse updateCustomerInfo(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser user = request.getUser();
		BillBusiness business = request.getBusiness();
		if(user == null || user.getCurrentSubscription() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BeanUtilsBean notNullBean = new NullAwareBeanUtils();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBSubscription dbSubscription = dao.getEntityByKey(BillDBSubscription.class, ID_ATTR, user.getCurrentSubscription().getId(), false);
			if(dbSubscription == null) {
				dbSubscription = dao.getEntityByKey(BillDBSubscription.class, USER_DB_ATTR_PHONE, user.getPhone(), false);
				if(dbSubscription == null) {
					dbSubscription = new BillDBSubscription();
				}
				dbSubscription.setCreatedDate(new Date());
				dbSubscription.setStatus(STATUS_ACTIVE);
			}
			notNullBean.copyProperties(dbSubscription, user);
			if(user.getCurrentSubscription().getServiceCharge() != null) {
				dbSubscription.setServiceCharge(user.getCurrentSubscription().getServiceCharge());
			}
			if(user.getCurrentSubscription().getArea() != null) {
				BillDBLocation location = new BillDBLocation();
				location.setId(user.getCurrentSubscription().getArea().getId());
				dbSubscription.setLocation(location);
			}
			if(dbSubscription.getBusiness() == null && business != null) {
				BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
				dbBusiness.setId(business.getId());
				dbSubscription.setBusiness(dbBusiness);
			}
			if(dbSubscription.getId() == null) {
				session.persist(dbSubscription);
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

	public BillServiceResponse updateCustomerItem(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillItem item = request.getItem();
		BillSubscription currentSubscription = new BillSubscription();
		if(request.getUser().getCurrentSubscription() != null) {
			currentSubscription = request.getUser().getCurrentSubscription();
		}
		if(item == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BeanUtilsBean notNullBean = new NullAwareBeanUtils();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBItemSubscription dbSubscribedItem = dao.getEntityByKey(BillDBItemSubscription.class, ID_ATTR, item.getId(), false);
			if(dbSubscribedItem == null) {
				if(item.getParentItem() != null) {
					dbSubscribedItem = new BillSubscriptionDAOImpl(session).getActiveItemSubscription(currentSubscription.getId(), item.getParentItem().getId());
				}
				if(dbSubscribedItem == null) {
					dbSubscribedItem = new BillDBItemSubscription();
				}
				dbSubscribedItem.setCreatedDate(new Date());
				dbSubscribedItem.setStatus(STATUS_ACTIVE);
			}
			notNullBean.copyProperties(dbSubscribedItem, item);
			if(dbSubscribedItem.getSubscription() == null && request.getUser() != null && currentSubscription != null && currentSubscription.getId() != null) {
				BillDBSubscription subscription = new BillDBSubscription();
				subscription.setId(currentSubscription.getId());
				dbSubscribedItem.setSubscription(subscription);
			}
			if(dbSubscribedItem.getBusinessItem() == null && item.getParentItem() != null && item.getParentItem().getId() != null) {
				BillDBItemBusiness businessItem = new BillDBItemBusiness();
				businessItem.setId(item.getParentItem().getId());
				dbSubscribedItem.setBusinessItem(businessItem);
			}
			if(dbSubscribedItem.getId() == null) {
				session.persist(dbSubscribedItem);
			}
			//Update change log
			BillUserLogUtil.updateBillItemLog(item, session, dao, dbSubscribedItem);
			
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}



}
