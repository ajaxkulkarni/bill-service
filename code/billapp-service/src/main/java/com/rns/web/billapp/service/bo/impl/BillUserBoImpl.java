package com.rns.web.billapp.service.bo.impl;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
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
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillInvoice;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
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
					BillBusinessConverter.updateBusinessDetails(user, dao, dbUser);
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
					dbFinancial = dao.getEntityByKey(BillDBUserFinancialDetails.class, "user.id", existingUser.getId(), true);
					if(dbFinancial == null) {
						dbFinancial = new BillDBUserFinancialDetails();
					}
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
		//BillItem item = request.getItem();
		if(request.getBusiness() == null || request.getBusiness().getId() == null || CollectionUtils.isEmpty(request.getItems())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			for(BillItem item: request.getItems()) {
				BillBusinessConverter.updateBusinessItem(item, session, request.getBusiness());
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

	public BillServiceResponse updateCustomerItemTemporary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillItem item = request.getItem();
		if(item == null || (item.getPrice() == null && item.getQuantity() == null) || item.getChangeLog() == null || item.getChangeLog().getFromDate() == null || item.getChangeLog().getToDate() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			if(item.getId() != null) {
				BillDBItemSubscription dbSubscribedItem = dao.getEntityByKey(BillDBItemSubscription.class, ID_ATTR, item.getId(), false);
				if(dbSubscribedItem != null) {
					//Update change log
					BillUserLogUtil.updateBillItemLog(item, session, dao, dbSubscribedItem);
				}
			} else if (item.getParentItem() != null && item.getParentItem().getId() != null) {
				BillDBItemBusiness businessItem = dao.getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getParentItem().getId(), false);
				if(businessItem != null) {
					BillDBItemSubscription subscribed = new BillDBItemSubscription();
					subscribed.setBusinessItem(businessItem);
					//Update change log
					BillUserLogUtil.updateBillItemLog(item, session, dao, subscribed);
				}
			} else if (item.getParentItemId() != null) {
				//Update change log
				BillUserLogUtil.updateBillItemLog(item, session, dao, new BillDBItemSubscription());
			} else {
				response.setResponse(ERROR_CODE_FATAL, ERROR_INVALID_ITEM);
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

	public BillServiceResponse updateCustomerInvoice(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillInvoice invoice = request.getInvoice();
		BillSubscription currentSubscription = request.getUser().getCurrentSubscription();
		if(invoice == null || request.getUser() == null || currentSubscription == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		if(invoice.getMonth() == null || invoice.getYear() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
			BillDBInvoice dbInvoice = dao.getInvoiceForMonth(currentSubscription.getId(), invoice.getMonth(), invoice.getYear());
			if(dbInvoice == null) {
				dbInvoice = new BillDBInvoice();
				invoice.setStatus(INVOICE_STATUS_PENDING);
			} 
			NullAwareBeanUtils nullAware = new NullAwareBeanUtils();
			nullAware.copyProperties(dbInvoice, invoice);
			if(dbInvoice.getSubscription() == null) {
				dbInvoice.setSubscription(new BillDBSubscription(currentSubscription.getId()));
			}
			if(dbInvoice.getId() == null) {
				session.persist(dbInvoice);
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

	public BillServiceResponse loadProfile(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser requestUser = request.getUser();
		if(requestUser == null || StringUtils.isBlank(requestUser.getPhone())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser dbUser = dao.getEntityByKey(BillDBUser.class, USER_DB_ATTR_PHONE, requestUser.getPhone(), false);
			if(dbUser == null || StringUtils.equals(STATUS_DELETED, dbUser.getStatus())) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_USER);
				return response;
			}
			if(StringUtils.equals(STATUS_PENDING, dbUser.getStatus())) {
				response.setResponse(ERROR_NOT_APPROVED, ERROR_USER_NOT_APPROVED);
				return response;
			}
			BillUser user = BillDataConverter.getUserProfile(response, session, dbUser);
			response.setUser(user);
			
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
			System.out.println("Closed");
		}
		return response;
	}

	public BillServiceResponse getAllAreas() {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBLocation> locations = dao.getEntities(BillDBLocation.class, true);
			response.setLocations(BillDataConverter.getLocations(locations));
			
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getSectorItems(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getSector() == null || request.getSector().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBItemParent> items = dao.getEntitiesByKey(BillDBItemParent.class, "sector.id", request.getSector().getId(),  true);
			response.setItems(BillDataConverter.getItems(items));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getBusinessItems(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBItemBusiness> items = dao.getEntitiesByKey(BillDBItemBusiness.class, "business.id", request.getBusiness().getId(),  true);
			response.setItems(BillDataConverter.getBusinessItems(items));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}
	
	public BillServiceResponse getAllBusinessCustomers(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillSubscriptionDAOImpl dao = new BillSubscriptionDAOImpl(session);
			List<BillDBSubscription> customers = dao.getBusinessSubscriptions(request.getBusiness().getId());
			response.setUsers(BillDataConverter.getCustomers(customers));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse loadDeliveries(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			
			List<BillDBItemBusiness> items = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBItemBusiness.class, "business.id", request.getBusiness().getId(),  true);
			if(CollectionUtils.isEmpty(items)) {
				return response;
			}
			
			//change log for the business
			BillLogDAOImpl dao = new BillLogDAOImpl(session);
			
			List<BillDBUserLog> logs = new ArrayList<BillDBUserLog>();
			if(CollectionUtils.isNotEmpty(items)) {
				for(BillDBItemBusiness businessItem: items) {
					
					BillDBUserLog userLog = new BillDBUserLog();
					BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
					dbBusiness.setId(request.getBusiness().getId());
					userLog.setBusiness(dbBusiness);
					userLog.setFromDate(request.getRequestedDate());
					userLog.setBusinessItem(businessItem);
					userLog.setParentItem(businessItem.getParent());
					
					BillDBUserLog businessLog = dao.getLatestBusinessItemQuantityLog(userLog);
					if(businessLog != null) {
						logs.add(businessLog);
					}
					
					BillDBUserLog subscriptionLog = dao.getLatestSubscribedItemQuantityLog(userLog);
					
					if(subscriptionLog != null) {
						logs.add(subscriptionLog);
					}
					
					if(userLog.getParentItem() != null) {
						BillDBUserLog parentItemLog = dao.getLatestParentItemsQuantityLog(userLog);
						if(parentItemLog != null) {
							logs.add(parentItemLog);
						}
					}
					
				}
			}
			
			List<BillUser> users = new ArrayList<BillUser>();
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			List<BillDBSubscription> orders = new BillVendorDaoImpl(session).getDeliveries(request.getBusiness().getId());
			System.out.println("Fetching done..");
			if(CollectionUtils.isNotEmpty(orders)) {
				for(BillDBSubscription subscription: orders) {
					BillUser user = new BillUser();
					beanUtils.copyProperties(user, subscription);
					BillSubscription currentSubscription = new BillSubscription();
					currentSubscription.setItems(BillDataConverter.getSubscribedItems(new ArrayList<BillDBItemSubscription>(subscription.getSubscriptions())));
					user.setCurrentSubscription(currentSubscription); 
					/*if(BillRuleEngine.isDelivery(logs, currentSubscription)) {
						users.add(user);
					}*/
				}
			}
			response.setUsers(users);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}
	
	public BillServiceResponse getCustomerProfile(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getUser() == null || request.getUser().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillSubscriptionDAOImpl dao = new BillSubscriptionDAOImpl(session);
			BillDBSubscription customer = dao.getSubscriptionDetails(request.getUser().getId());
			response.setUser(BillDataConverter.getCustomerDetails(new NullAwareBeanUtils(), customer));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

}
