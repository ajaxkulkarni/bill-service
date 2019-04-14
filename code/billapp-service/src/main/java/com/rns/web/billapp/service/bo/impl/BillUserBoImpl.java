package com.rns.web.billapp.service.bo.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillUserBo;
import com.rns.web.billapp.service.bo.domain.BillAdminDashboard;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillCustomerGroup;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillOrder;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBBusinessInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerGroup;
import com.rns.web.billapp.service.dao.domain.BillDBDevices;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusinessInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
import com.rns.web.billapp.service.dao.domain.BillMyCriteria;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillSchemesDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillTransactionsDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillMessageBroadcaster;
import com.rns.web.billapp.service.util.BillNameSorter;
import com.rns.web.billapp.service.util.BillPayTmStatusCheck;
import com.rns.web.billapp.service.util.BillPaymentUtil;
import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.BillSMSUtil;
import com.rns.web.billapp.service.util.BillUserLogUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillUserBoImpl implements BillUserBo, BillConstants {

	private SessionFactory sessionFactory;
	private ThreadPoolTaskExecutor executor;

	public static Set<Integer> invoicesInProgress = new ConcurrentSkipListSet<Integer>();

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
		if (user == null || StringUtils.isBlank(user.getPhone())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser existingUser = dao.getEntityByKey(BillDBUser.class, USER_DB_ATTR_PHONE, user.getPhone(), false);
			if (user.getId() == null) {
				if (existingUser == null) {
					BillDBUser dbUser = new BillDBUser();
					BeanUtils.copyProperties(dbUser, user);
					dbUser.setCreatedDate(new Date());
					dbUser.setStatus(STATUS_PENDING);
					session.persist(dbUser);
					updateUserFiles(user, dbUser);
					BillBusinessConverter.updateBusinessDetails(user, dao, dbUser);
					BillPaymentCredentials instaResponse = BillPaymentUtil.createNewUser(user, null);
					BillBusinessConverter.setPaymentCredentials(dbUser, instaResponse);
					// New user ; send email and SMS if NOT distributor
					if (user.getCurrentBusiness() != null && StringUtils.equals(ACCESS_DISTRIBUTOR, user.getCurrentBusiness().getType())) {
						// Do not send email/SMS as this is mostly added by
						// admin
						dbUser.setStatus(STATUS_ACTIVE);
					} else {
						BillSMSUtil.sendSMS(user, null, MAIL_TYPE_REGISTRATION, null);
						executor.execute(new BillMailUtil(MAIL_TYPE_REGISTRATION, user));
						BillSMSUtil.sendSMS(user, null, MAIL_TYPE_REGISTRATION_ADMIN, null);
						executor.execute(new BillMailUtil(MAIL_TYPE_REGISTRATION_ADMIN, user));
					}

				} else {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_MOBILE_PRESENT);
				}
			} else {
				BeanUtilsBean notNullBean = new NullAwareBeanUtils();

				if (existingUser != null) {
					notNullBean.copyProperties(existingUser, user);
					updateUserFiles(user, existingUser);
					BillBusinessConverter.updateBusinessDetails(user, dao, existingUser);
					if (StringUtils.isBlank(existingUser.getInstaId())) {
						BillUser currentUser = new BillUser();
						notNullBean.copyProperties(currentUser, existingUser);
						BillPaymentCredentials instaResponse = BillPaymentUtil.createNewUser(currentUser, existingUser.getRefreshToken());
						BillBusinessConverter.setPaymentCredentials(existingUser, instaResponse);
					}
					session.persist(existingUser);
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
		if (!folderLocation.exists()) {
			folderLocation.mkdirs();
		}
		if (user.getPanFile() != null) {
			String panFilePath = folderPath + user.getPanFile().getFilePath();
			CommonUtils.writeToFile(user.getPanFile().getFileData(), panFilePath);
			dbUser.setPanFilePath(panFilePath);
		}
		if (user.getAadharFile() != null) {
			String aadharFilePath = folderPath + user.getAadharFile().getFilePath();
			CommonUtils.writeToFile(user.getAadharFile().getFileData(), aadharFilePath);
			dbUser.setAadharFilePath(aadharFilePath);
		}
	}

	public BillServiceResponse updateUserFinancialInfo(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser user = request.getUser();
		if (user == null || StringUtils.isBlank(user.getPhone()) || user.getFinancialDetails() == null) {
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
			if (existingUser != null) {
				BillDBUserFinancialDetails dbFinancial = null;
				if (user.getFinancialDetails().getId() != null) {
					dbFinancial = dao.getEntityByKey(BillDBUserFinancialDetails.class, ID_ATTR, user.getFinancialDetails().getId(), true);
				}
				if (dbFinancial == null) {
					dbFinancial = dao.getEntityByKey(BillDBUserFinancialDetails.class, "user.id", existingUser.getId(), true);
					if (dbFinancial == null) {
						dbFinancial = new BillDBUserFinancialDetails();
					}
					dbFinancial.setUser(existingUser);
					dbFinancial.setStatus(STATUS_ACTIVE);
					dbFinancial.setCreatedDate(new Date());
				}
				notNullBean.copyProperties(dbFinancial, user.getFinancialDetails());
				if (dbFinancial.getId() == null) {
					session.persist(dbFinancial);
				}
				if (StringUtils.isNotBlank(existingUser.getInstaId())) {
					BillFinancialDetails details = new BillFinancialDetails();
					notNullBean.copyProperties(details, dbFinancial);
					BillPaymentCredentials credentials = new BillPaymentCredentials();
					credentials.setInstaId(existingUser.getInstaId());
					credentials.setRefresh_token(existingUser.getRefreshToken());
					credentials.setAccess_token(existingUser.getAccessToken());
					BillBusinessConverter.setPaymentCredentials(existingUser, BillPaymentUtil.updateBankDetails(details, credentials, true));

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
		// BillItem item = request.getItem();
		if (request.getBusiness() == null || request.getBusiness().getId() == null || CollectionUtils.isEmpty(request.getItems())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			for (BillItem item : request.getItems()) {
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
		if (user == null || request.getUser().getCurrentSubscription() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		BillSubscription customer = user.getCurrentSubscription();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			String phone = CommonUtils.trimPhoneNumber(user.getPhone());
			BeanUtilsBean notNullBean = new NullAwareBeanUtils();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUserBusiness dbBusiness = null;
			BillDBSubscription dbSubscription = dao.getEntityByKey(BillDBSubscription.class, ID_ATTR, customer.getId(), false);
			if (dbSubscription == null) {
				dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(phone, request.getBusiness().getId());
				if (dbSubscription == null) {
					dbSubscription = new BillDBSubscription();
				} else if (StringUtils.equals("CUSTOMER", request.getRequestType())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_MOBILE_PRESENT);
					return response;
				}
				dbSubscription.setCreatedDate(new Date());
				dbSubscription.setStatus(STATUS_ACTIVE);
			}
			if(phone != null && phone.length() < 10) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_PHONE_NUMBER);
				return response;
			}
			if(phone != null && phone.length() > 12) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_PHONE_NUMBER);
				return response;
			}
			user.setPhone(phone);
			notNullBean.copyProperties(dbSubscription, user);
			if (customer.getServiceCharge() != null) {
				dbSubscription.setServiceCharge(customer.getServiceCharge());
			}
			if (customer.getArea() != null) {
				BillDBLocation location = new BillDBLocation();
				location.setId(customer.getArea().getId());
				dbSubscription.setLocation(location);
			}
			if (dbSubscription.getBusiness() == null && business != null) {
				dbBusiness = dao.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, business.getId(), true);
				if (dbBusiness == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
					return response;
				}
				business.setName(dbBusiness.getName());
				business = BillDataConverter.getBusiness(dbBusiness);
				dbBusiness.setId(business.getId());
				dbSubscription.setBusiness(dbBusiness);
			}
			if (dbSubscription.getId() == null) {
				session.persist(dbSubscription);
				// New customer
				user.setCurrentBusiness(business);
				executor.execute(new BillMailUtil(MAIL_TYPE_NEW_CUSTOMER, user));
				BillSMSUtil.sendSMS(user, null, MAIL_TYPE_NEW_CUSTOMER, null);
			}
			if(request.getCustomerGroup() != null) {
				if(request.getCustomerGroup().getId() == null || request.getCustomerGroup().getId() == 0 | request.getCustomerGroup().getId().intValue() == 0) {
					dbSubscription.setCustomerGroup(null);
					dbSubscription.setGroupSequence(null);
				} else if(dbSubscription.getCustomerGroup() == null || (dbSubscription.getCustomerGroup() != null && dbSubscription.getCustomerGroup().getId() != request.getCustomerGroup().getId())){
					BillDBCustomerGroup customerGroup = dao.getEntityByKey(BillDBCustomerGroup.class, ID_ATTR, request.getCustomerGroup().getId(), true);
					if(customerGroup != null) {
						dbSubscription.setCustomerGroup(customerGroup);
						Integer maxSequence = BillRuleEngine.getNextGroupNumber(dao, dbSubscription);
						dbSubscription.setGroupSequence(maxSequence);
					}
				}
			}
			tx.commit();
			if(user.getId() == null) {
				user.setId(dbSubscription.getId());
				response.setUser(user);
			}
			if(StringUtils.equals("CUSTOMER", request.getRequestType()) && dbBusiness != null) {
				//Notify vendor that customer is added
				BillUser vendor = new BillUser();
				new NullAwareBeanUtils().copyProperties(vendor, dbBusiness.getUser());
				vendor.setCurrentBusiness(business);
				BillMailUtil vendorMailer = new BillMailUtil(MAIL_TYPE_NEW_CUSTOMER_VENDOR, vendor);
				vendorMailer.setCustomerInfo(user);
				executor.execute(vendorMailer);
				BillSMSUtil smsUtil = new BillSMSUtil();
				smsUtil.setCustomer(user);
				smsUtil.sendSms(vendor, null, MAIL_TYPE_NEW_CUSTOMER_VENDOR, null);
			}
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
		if (request.getUser().getCurrentSubscription() != null) {
			currentSubscription = request.getUser().getCurrentSubscription();
		}
		if (item == null) {
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
			if (dbSubscribedItem == null) {
				if (item.getParentItem() != null) {
					dbSubscribedItem = new BillSubscriptionDAOImpl(session).getActiveItemSubscription(currentSubscription.getId(),
							item.getParentItem().getId());
				}
				if (dbSubscribedItem == null) {
					dbSubscribedItem = new BillDBItemSubscription();
				}
				dbSubscribedItem.setCreatedDate(new Date());
				dbSubscribedItem.setStatus(STATUS_ACTIVE);
			}
			notNullBean.copyProperties(dbSubscribedItem, item);
			if (dbSubscribedItem.getSubscription() == null && request.getUser() != null && currentSubscription != null && currentSubscription.getId() != null) {
				BillDBSubscription subscription = new BillDBSubscription();
				subscription.setId(currentSubscription.getId());
				dbSubscribedItem.setSubscription(subscription);
			}
			if (dbSubscribedItem.getBusinessItem() == null && item.getParentItem() != null && item.getParentItem().getId() != null) {
				BillDBItemBusiness businessItem = new BillDBItemBusiness();
				businessItem.setId(item.getParentItem().getId());
				dbSubscribedItem.setBusinessItem(businessItem);
			}
			if (dbSubscribedItem.getId() == null) {
				session.persist(dbSubscribedItem);
			}
			// Update change log
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
		if (item == null && CollectionUtils.isEmpty(request.getItems())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			if (item != null && item.getId() != null) {
				BillDBItemSubscription dbSubscribedItem = dao.getEntityByKey(BillDBItemSubscription.class, ID_ATTR, item.getId(), false);
				if (dbSubscribedItem != null) {
					// Update change log
					BillUserLogUtil.updateBillItemLog(item, session, dao, dbSubscribedItem);
					BillItem logItem = BillDataConverter.getItem(dbSubscribedItem);
					logItem.setChangeLog(item.getChangeLog());
					BillUser billUser = new BillUser();
					new NullAwareBeanUtils().copyProperties(billUser, dbSubscribedItem.getSubscription());
					BillBusiness business = new BillBusiness();
					business.setName(dbSubscribedItem.getSubscription().getBusiness().getName());
					billUser.setCurrentBusiness(business);
					sendLogUpdate(logItem, billUser, MAIL_TYPE_PAUSE_CUSTOMER);
				}
			} else if (item != null && item.getParentItem() != null && item.getParentItem().getId() != null) {
				pauseBusinessItem(item, session, dao);
			} else if (CollectionUtils.isNotEmpty(request.getItems())) {
				// Pause multiple business items for pause business service
				BillDBItemBusiness itemBusiness = null;
				for (BillItem businessItem : request.getItems()) {
					itemBusiness = pauseBusinessItem(businessItem, session, dao);
				}
				if (itemBusiness != null) {
					// Send log update to customers
					List<BillDBSubscription> subscriptions = new BillSubscriptionDAOImpl(session).getBusinessSubscriptions(itemBusiness.getBusiness().getId(), null);
					if (CollectionUtils.isNotEmpty(subscriptions)) {
						for (BillDBSubscription subscription : subscriptions) {
							BillUser billUser = new BillUser();
							new NullAwareBeanUtils().copyProperties(billUser, subscription);
							BillBusiness business = BillDataConverter.getBusiness(itemBusiness.getBusiness());
							billUser.setCurrentBusiness(business);
							sendLogUpdate(request.getItems().get(0), billUser, MAIL_TYPE_PAUSE_BUSINESS);
						}
					}
				}
			} else if (item != null && item.getParentItemId() != null) {
				// Update change log
				BillUserLogUtil.updateBillItemLog(item, session, dao, new BillDBItemSubscription());
			} else if (StringUtils.equalsIgnoreCase(request.getRequestType(), "DELETE") && item.getChangeLog().getId() != null) {
				BillDBUserLog log = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserLog.class, ID_ATTR, item.getChangeLog().getId(), false);
				if (log != null) {
					if (log.getFromDate().getTime() <= new Date().getTime()) {
						response.setResponse(ERROR_CODE_GENERIC, ERROR_OLD_HOLIDAY_DELETION);
					} else {
						session.delete(log);
					}
				}
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

	private BillDBItemBusiness pauseBusinessItem(BillItem item, Session session, BillGenericDaoImpl dao) {
		BillDBItemBusiness businessItem = dao.getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getParentItem().getId(), false);
		if (businessItem != null) {
			BillDBItemSubscription subscribed = new BillDBItemSubscription();
			subscribed.setBusinessItem(businessItem);
			// Update change log
			BillUserLogUtil.updateBillItemLog(item, session, dao, subscribed);
		}
		return businessItem;
	}

	private void sendLogUpdate(BillItem item, BillUser billUser, String mailType) {
		List<BillItem> items = new ArrayList<BillItem>();
		items.add(item);
		billUser.getCurrentBusiness().setItems(items);
		BillMailUtil billMailUtil = new BillMailUtil(mailType, billUser);
		executor.execute(billMailUtil);
		BillSMSUtil.sendSMS(billUser, null, mailType, null);
	}

	public BillServiceResponse updateCustomerInvoice(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillInvoice invoice = request.getInvoice();
		BillSubscription currentSubscription = request.getUser().getCurrentSubscription();
		if (invoice == null || request.getUser() == null || currentSubscription == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		if (invoice.getMonth() == null || invoice.getYear() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
			BillDBInvoice dbInvoice = dao.getInvoiceForMonth(currentSubscription.getId(), invoice.getMonth(), invoice.getYear());
			BillDBSubscription customerSubscription = new BillGenericDaoImpl(session).getEntityByKey(BillDBSubscription.class, ID_ATTR,
					currentSubscription.getId(), true);
			if (customerSubscription == null) {
				response.setResponse(ERROR_CODE_FATAL, ERROR_NO_USER);
				return response;
			}
			boolean invoicePaid = false;
			if (dbInvoice == null) {
				dbInvoice = new BillDBInvoice();
				dbInvoice.setStatus(INVOICE_STATUS_PENDING);
				dbInvoice.setCreatedDate(new Date());
			} else {
				//If invoice is already paid.. don't allow user to un pay it
				if(StringUtils.equals(INVOICE_STATUS_PAID, dbInvoice.getStatus()) && !StringUtils.equals(INVOICE_STATUS_DELETED, invoice.getStatus())) {
					response.setResponse(ERROR_CODE_FATAL, ERROR_INVOICE_PAID);
					return response;
				}
				
				if (!StringUtils.equals(invoice.getStatus(), dbInvoice.getStatus())) {
					if (StringUtils.equals(INVOICE_STATUS_PAID, invoice.getStatus())) {
						invoicePaid = true;
						BillBusinessConverter.updatePaymentStatusAsPaid(invoice, dbInvoice);
						BillBusinessConverter.updatePaymentTransactionLog(session, dbInvoice, invoice);
					} else {
						dbInvoice.setStatus(invoice.getStatus());
						BillBusinessConverter.updatePaymentTransactionLog(session, dbInvoice, invoice);
					}
				}
			}
			NullAwareBeanUtils nullAware = new NullAwareBeanUtils();
			nullAware.copyProperties(dbInvoice, invoice);
			if (dbInvoice.getSubscription() == null) {
				dbInvoice.setSubscription(customerSubscription);
			}
			if (dbInvoice.getId() == null) {
				session.persist(dbInvoice);
			}
			if(!invoicePaid && !StringUtils.equals(INVOICE_STATUS_DELETED, invoice.getStatus())) {
				BillBusinessConverter.setInvoiceItems(invoice, session, dbInvoice, true);
			}
			if (customerSubscription != null) {
				nullAware.copyProperties(invoice, dbInvoice);
				if (invoicePaid) {
					// updateTransactionLog(session, dbInvoice);
					invoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + invoice.getId());
					BillRuleEngine.sendEmails(invoice, dbInvoice, nullAware, executor, sessionFactory);
				}
				// Update payment URL
				BillBusinessConverter.updatePaymentURL(invoice, dbInvoice, customerSubscription, session);
			}
			if(StringUtils.isBlank(dbInvoice.getShortUrl())) {
				dbInvoice.setShortUrl(BillSMSUtil.shortenUrl(null, BillRuleEngine.preparePaymentUrl(dbInvoice.getId())));
			}
			tx.commit();
			
			//if(invoice.getId() == null) {
				BillInvoice currrInvoice = BillDataConverter.getInvoice(new NullAwareBeanUtils(), dbInvoice);
				BillRuleEngine.calculatePayable(currrInvoice, dbInvoice, session);
				BillUser customerDetails = BillDataConverter.getCustomerDetails(new NullAwareBeanUtils(), dbInvoice.getSubscription());
				if(dbInvoice.getSubscription() != null) {
					customerDetails.setCurrentBusiness(BillDataConverter.getBusinessBasic(dbInvoice.getSubscription().getBusiness()));
				}
				currrInvoice.setPaymentMessage(BillSMSUtil.generateResultMessage(customerDetails, currrInvoice, BillConstants.MAIL_TYPE_INVOICE, null));
				response.setInvoice(currrInvoice);
			//}
			
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
		if (requestUser == null || StringUtils.isBlank(requestUser.getPhone())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser dbUser = dao.getEntityByKey(BillDBUser.class, USER_DB_ATTR_PHONE, requestUser.getPhone(), false);
			if (dbUser == null || StringUtils.equals(STATUS_DELETED, dbUser.getStatus())) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_USER);
				return response;
			}
			if (StringUtils.equals(STATUS_PENDING, dbUser.getStatus())) {
				response.setResponse(ERROR_NOT_APPROVED, ERROR_USER_NOT_APPROVED);
				return response;
			}
			BillUser user = BillDataConverter.getUserProfile(response, session, dbUser);
			response.setUser(user);
			
			//Store the device ID if not stored
			if(StringUtils.isNotBlank(requestUser.getDeviceId())) {
				BillDBDevices devices = dao.getEntityByKey(BillDBDevices.class, "token", requestUser.getDeviceId(), true);
				if(devices == null) {
					Transaction tx = session.beginTransaction();
					devices = new BillDBDevices();
					devices.setToken(requestUser.getDeviceId());
					devices.setUser(dbUser);
					devices.setCreatedDate(new Date());
					devices.setStatus(STATUS_ACTIVE);
					session.persist(devices);
					tx.commit();
				}
			}

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
			List<BillDBLocation> locations = dao.getEntities(BillDBLocation.class, true, "name", "asc");
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
		if (request.getSector() == null || request.getSector().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBItemParent> items = dao.getEntitiesByKey(BillDBItemParent.class, "sector.id", request.getSector().getId(), true, "name", "asc");
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
		if (request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBItemBusiness> items = dao.getEntitiesByKey(BillDBItemBusiness.class, "business.id", request.getBusiness().getId(), true, null, null);
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
		if (request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillSubscriptionDAOImpl dao = new BillSubscriptionDAOImpl(session);
			Integer groupId = null;
			if(request.getCustomerGroup() != null) {
				groupId = request.getCustomerGroup().getId();
			}
			List<BillDBSubscription> customers = dao.getBusinessSubscriptions(request.getBusiness().getId(), groupId);
			response.setUsers(BillDataConverter.getCustomers(customers, false));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse loadDeliveries(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getBusiness().getId() == null || request.getRequestedDate() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillUser> users = new ArrayList<BillUser>();
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			Integer groupId = null;
			if(request.getCustomerGroup() != null && request.getCustomerGroup().getId() != null) {
				groupId = request.getCustomerGroup().getId();
			}
			List<BillDBOrders> orders = new BillVendorDaoImpl(session).getOrders(request.getRequestedDate(), request.getBusiness().getId(), groupId);
			System.out.println("Fetching done..");
			if (CollectionUtils.isNotEmpty(orders)) {
				for (BillDBOrders order : orders) {
					BillUser user = new BillUser();
					beanUtils.copyProperties(user, order.getSubscription());
					BillSubscription currentSubscription = new BillSubscription();
					currentSubscription.setItems(BillDataConverter.getOrderItems(order.getOrderItems()));
					currentSubscription.setId(order.getSubscription().getId());
					currentSubscription.setAmount(order.getAmount());
					currentSubscription.setStatus(order.getStatus());
					user.setCurrentSubscription(currentSubscription);
					users.add(user);
				}
			}
			/*if(groupId == null) {
				Collections.sort(users, new BillNameSorter());
			}*/
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
		if (request.getUser() == null || request.getUser().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillSubscriptionDAOImpl dao = new BillSubscriptionDAOImpl(session);
			BillDBSubscription customer = dao.getSubscriptionDetails(request.getUser().getId());
			if (customer == null) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_CUSTOMER_PROFILE_NOT_FOUND);
				return response;
			}
			BillUser customerDetails = BillDataConverter.getCustomerDetails(new NullAwareBeanUtils(), customer);
			// Additional info for profile
			BillSubscription currentSubscription = customerDetails.getCurrentSubscription();
			if (currentSubscription != null) {
				//Get all pending invoices
				BillInvoiceDaoImpl billInvoiceDaoImpl = new BillInvoiceDaoImpl(session);
				List<BillDBInvoice> pendingInvoices = billInvoiceDaoImpl.getAllInvoices(currentSubscription.getId(), INVOICE_STATUS_PENDING);
				List<BillDBInvoice> failedInvoices = billInvoiceDaoImpl.getAllInvoices(currentSubscription.getId(), INVOICE_STATUS_FAILED);
				if(CollectionUtils.isNotEmpty(failedInvoices)) {
					if(pendingInvoices == null) {
						pendingInvoices = new ArrayList<BillDBInvoice>();
					}
					pendingInvoices.addAll(failedInvoices);
				}
				if(CollectionUtils.isNotEmpty(pendingInvoices)) {
					currentSubscription.setBillsDue(pendingInvoices.size());
					response.setInvoices(BillDataConverter.getInvoices(pendingInvoices, session));
					BillDBInvoice latestPaid = billInvoiceDaoImpl.getLatestPaidInvoice(currentSubscription.getId());
					if (latestPaid != null) {
						currentSubscription.setLastBillPaid(latestPaid.getPaidDate());
					}
				} else {
					currentSubscription.setBillsDue(0);
				}
				if(customer.getCustomerGroup() != null) {
					//Get group
					BillDBCustomerGroup customerGroup = new BillGenericDaoImpl(session).getEntityByKey(BillDBCustomerGroup.class, ID_ATTR, customer.getCustomerGroup().getId(), true);
					if(customerGroup != null) {
						BillCustomerGroup group = new BillCustomerGroup();
						new NullAwareBeanUtils().copyProperties(group, customerGroup);
						currentSubscription.setGroup(group);
					}
				}
				
			}
			response.setUser(customerDetails);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getCustomerInvoices(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || request.getUser().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
			List<BillDBInvoice> invoices = dao.getAllInvoices(request.getUser().getId(), null);
			System.out.println("Done ..............");
			List<BillInvoice> userInvoices = BillDataConverter.getInvoices(invoices, session);
			response.setInvoices(userInvoices);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse sendCustomerInvoice(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillInvoice invoice = request.getInvoice();
		if (invoice.getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBInvoice dbInvoice = dao.getEntityByKey(BillDBInvoice.class, "id", invoice.getId(), false);
			if (StringUtils.equals(INVOICE_STATUS_DELETED, dbInvoice.getStatus())) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_INVOICE_NOT_FOUND);
				return response;
			}
			if (dbInvoice != null && dbInvoice.getAmount() != null /*&& dbInvoice.getMonth() != null && dbInvoice.getYear() != null*/) {
				BillDBUser vendor = null;
				BillUser customer = new BillUser();
				NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
				invoice = BillDataConverter.getInvoice(beanUtils, dbInvoice);
				BillPaymentCredentials credentials = new BillPaymentCredentials();
				if (dbInvoice.getSubscription() != null) {
					beanUtils.copyProperties(customer, dbInvoice.getSubscription());
					BillBusiness business = BillDataConverter.getBusiness(dbInvoice.getSubscription().getBusiness());
					customer.setCurrentBusiness(business);
					vendor = dbInvoice.getSubscription().getBusiness().getUser();
					BillDataConverter.setCredentials(vendor, credentials);
					BillRuleEngine.calculatePayable(invoice, dbInvoice, session);
				}
				if (!StringUtils.equalsIgnoreCase(REQUEST_TYPE_EMAIL, request.getRequestType())) {
					if(!StringUtils.equalsIgnoreCase("READONLY", request.getRequestType())) {
						Integer paymentAttempt = dbInvoice.getPaymentAttempt();
						if (paymentAttempt == null) {
							paymentAttempt = 0;
						}
						paymentAttempt++;
						dbInvoice.setPaymentAttempt(paymentAttempt);
						BillPaymentUtil.prepareHdfcRequest(invoice, customer);
						BillPaymentUtil.prepareCashFreeSignature(invoice, customer, paymentAttempt);
						BillPaymentUtil.prepareAtomRequest(invoice, vendor);
						BillPaymentUtil.preparePayTmRequest(invoice, customer, paymentAttempt);
						// Only if InstaMojo payment request is not already
						// generated
						if (StringUtils.isBlank(invoice.getPaymentUrl())) {
							BillBusinessConverter.updatePaymentURL(invoice, dbInvoice, vendor, customer, credentials);
						}
					} else {
						response.setResponse(BillSMSUtil.sendSMS(customer, invoice, MAIL_TYPE_INVOICE, null));
					}
				} else {
					invoice.setPaymentUrl(BillRuleEngine.preparePaymentUrl(invoice.getId()));
					if(StringUtils.isBlank(dbInvoice.getShortUrl())) {
						dbInvoice.setShortUrl(BillSMSUtil.shortenUrl(null, invoice.getPaymentUrl()));
					}
					if(dbInvoice.getNoOfReminders() == null) {
						dbInvoice.setNoOfReminders(0);
					}
					dbInvoice.setNoOfReminders(dbInvoice.getNoOfReminders() + 1);
					invoice.setShortUrl(dbInvoice.getShortUrl());
					BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_INVOICE);
					mailUtil.setUser(customer);
					mailUtil.setInvoice(invoice);
					executor.execute(mailUtil);
					response.setResponse(BillSMSUtil.sendSMS(customer, invoice, MAIL_TYPE_INVOICE, null));
				}
				response.setUser(customer);
				response.setInvoice(invoice);
			} else {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_INVOICE_NOT_FOUND);
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

	public BillServiceResponse updatePaymentCredentials(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || request.getUser().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillDBUser dbUser = new BillGenericDaoImpl(session).getEntityByKey(BillDBUser.class, "id", request.getUser().getId(), true);
			if (dbUser != null) {
				BillBusinessConverter.setPaymentCredentials(dbUser, BillPaymentUtil.getToken(null, dbUser.getEmail()));
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

	public BillServiceResponse completePayment(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		BillInvoice currentInvoice = request.getInvoice();
		try {
			session = this.sessionFactory.openSession();
			if (invoicesInProgress.contains(currentInvoice.getId())) {
				LoggingUtil.logMessage("Already working with this invoice .." + currentInvoice.getId() + " Invoices in progress = " + invoicesInProgress);
				return response;
			}
			invoicesInProgress.add(currentInvoice.getId()); // Locked
			LoggingUtil.logMessage("Locked .. " + currentInvoice.getId() + " -- " + invoicesInProgress);
			List<BillDBTransactions> existingTransctions = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBTransactions.class, "paymentId",
					currentInvoice.getPaymentId(), false, null, null);
			if(CollectionUtils.isNotEmpty(existingTransctions)) {
				for(BillDBTransactions existingTransaction: existingTransctions) {
					if (existingTransaction != null && (StringUtils.equals(INVOICE_STATUS_PAID, existingTransaction.getStatus()) || StringUtils.equals(INVOICE_SETTLEMENT_STATUS_SETTLED, existingTransaction.getStatus()) || StringUtils.equals(INVOICE_SETTLEMENT_STATUS_INITIATED, existingTransaction.getStatus()))) {
						//To avoid multiple hits from the server. In case of pending payment, multiple hits are allowed
						LoggingUtil.logMessage("Already transacted with this invoice .." + currentInvoice.getId() + " PID " + currentInvoice.getPaymentId() + " txn "
								+ existingTransaction.getId() + " status " + existingTransaction.getStatus() + " current status " + currentInvoice.getStatus());
						return response;
					}
				}
			}
			Transaction tx = session.beginTransaction();
			//This is causing issues on different transactions with same BANK TXNID or same request ID
			//BillDBInvoice invoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBInvoice.class, "paymentRequestId",
			//		currentInvoice.getPaymentRequestId(), false);
			//if (invoice == null) {
			BillDBInvoice invoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBInvoice.class, ID_ATTR, currentInvoice.getId(), false);
			if(invoice == null) {
				LoggingUtil.logMessage("Could not find any invoice for ID => " + currentInvoice.getId());
				return response;
			}
			invoice.setPaymentRequestId(currentInvoice.getPaymentRequestId());
			//}
			invoice.setStatus(currentInvoice.getStatus());
			updateInvoicePaymentStatus(currentInvoice, invoice);
			if (StringUtils.equalsIgnoreCase("Success", currentInvoice.getStatus()) || StringUtils.equalsIgnoreCase("Ok", currentInvoice.getStatus())) {
				invoice.setStatus(BillConstants.INVOICE_STATUS_PAID);
				// Update all pending invoices as outstanding bills is also paid
				// off
				List<BillDBInvoice> invoicesPaidOff = new ArrayList<BillDBInvoice>();
				List<BillDBInvoice> pendingInvoices = new BillInvoiceDaoImpl(session).getAllInvoices(invoice.getSubscription().getId(), INVOICE_STATUS_PENDING);
				List<BillDBInvoice> failedInvoices = new BillInvoiceDaoImpl(session).getAllInvoices(invoice.getSubscription().getId(), INVOICE_STATUS_FAILED);
				if (CollectionUtils.isNotEmpty(pendingInvoices)) {
					invoicesPaidOff.addAll(pendingInvoices);
				}
				if (CollectionUtils.isNotEmpty(failedInvoices)) {
					invoicesPaidOff.addAll(failedInvoices);
				}
				LoggingUtil.logMessage("Pending invoices = >" + pendingInvoices + " failed invoices =>" + failedInvoices);
				if (CollectionUtils.isNotEmpty(invoicesPaidOff)) {
					for (BillDBInvoice paidInvoice : invoicesPaidOff) {
						updateInvoicePaymentStatus(currentInvoice, paidInvoice);
					}
				}
				
				//Check for any reward scheme
				List<BillDBSchemes> rewardSchemes = new BillSchemesDaoImpl(session).getSchemes(SCHEME_TYPE_REWARD);
				if(CollectionUtils.isNotEmpty(rewardSchemes)) {
					BillDBSchemes schemes = rewardSchemes.get(0);
					if(validateScheme(schemes, invoice, session)) {
						BillDBCustomerCoupons coupon = BillBusinessConverter.getCustomerCoupon(schemes, invoice.getSubscription(), invoice);
						session.persist(coupon);
						BillRuleEngine.redeemCoupon(session, coupon, executor);
					}
				}
				if(invoice.getSubscription() != null && invoice.getSubscription().getBusiness() != null && invoice.getSubscription().getBusiness().getTransactionCharges() != null) {
					//Check for referrals
					List<BillDBCustomerCoupons> referrals = new BillSchemesDaoImpl(session).getReferrals(invoice.getSubscription().getBusiness().getId());
					if(CollectionUtils.isNotEmpty(referrals)) {
						for(BillDBCustomerCoupons coupons: referrals) {
							if(coupons.getScheme() != null && coupons.getScheme().getBusiness() != null && coupons.getScheme().getVendorCommission() != null) {
								BigDecimal commission = null;
								if(StringUtils.equals(COMMISSION_PAID_PERCENT, coupons.getScheme().getCommissionPaidType())) {
									 commission = invoice.getPaidAmount().multiply(coupons.getScheme().getVendorCommission()).divide(new BigDecimal(100), 4, RoundingMode.HALF_DOWN);
									 
								}
								//Transaction added for business which has given the referral code
								BillRuleEngine.addCouponTransaction(session, coupons, coupons.getScheme().getBusiness(), commission);
							}
						}
					}
				}
				
			} else {
				invoice.setStatus(INVOICE_STATUS_FAILED);
			}

			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			nullAwareBeanUtils.copyProperties(currentInvoice, invoice);
			//currentInvoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + (currentInvoice.getStatus() + "/"
			//		+ CommonUtils.encode(invoice.getSubscription().getBusiness().getName()) + "/" + invoice.getAmount() + "/" + invoice.getPaymentId()));
			
			currentInvoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + currentInvoice.getId());
			response.setInvoice(currentInvoice);

			BillBusinessConverter.updatePaymentTransactionLog(session, invoice, currentInvoice);
			BillRuleEngine.sendEmails(currentInvoice, invoice, nullAwareBeanUtils, executor, sessionFactory);
			
			tx.commit();

		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
			if(invoicesInProgress != null && currentInvoice != null && currentInvoice.getId() != null) {
				invoicesInProgress.remove(currentInvoice.getId()); // Un-Locked
				LoggingUtil.logMessage("Removed invoice .. " + currentInvoice.getId() + " .. " + invoicesInProgress);
			}
			if(BillPayTmStatusCheck.paytmPendingInvoices != null && currentInvoice != null && currentInvoice.getId() != null) {
				BillPayTmStatusCheck.paytmPendingInvoices.remove(currentInvoice.getId()); // Un-Locked
				LoggingUtil.logMessage("Removed paytm invoice .. " + currentInvoice.getId() + " .. " + BillPayTmStatusCheck.paytmPendingInvoices, LoggingUtil.paytmLogger);
			}
		}
		return response;
	}

	private boolean validateScheme(BillDBSchemes schemes, BillDBInvoice invoice, Session session) {
		if(schemes == null || schemes.getId() == null || invoice == null || invoice.getSubscription() == null || invoice.getSubscription().getId() == null) {
			return false;
		}
		// Check if the scheme is already accepted by the same customer
		BillDBCustomerCoupons existing = new BillSchemesDaoImpl(session).getAcceptedScheme(schemes.getId(), invoice.getSubscription().getId());
		if (existing != null) {
			return false;
		}
		// Check if any scheme is already accepted against this invoice
		if (invoice != null && invoice.getId() != null) {
			existing = new BillGenericDaoImpl(session).getEntityByKey(BillDBCustomerCoupons.class, "invoice.id", invoice.getId(), true);
			if (existing != null) {
				return false;
			}
		}
		return true;
	}

	private void updateInvoicePaymentStatus(BillInvoice currentInvoice, BillDBInvoice invoice) {
		invoice.setPaidDate(new Date());
		invoice.setPaymentId(currentInvoice.getPaymentId());
		invoice.setPaidAmount(currentInvoice.getAmount());
		invoice.setPaymentType(PAYMENT_ONLINE);
		invoice.setPaymentMedium(currentInvoice.getPaymentMedium());
		invoice.setPaymentMode(currentInvoice.getPaymentMode());
		if (StringUtils.equals(BillConstants.PAYMENT_MEDIUM_CASHFREE, invoice.getPaymentMedium())) {
			invoice.setSettlementStatus(INVOICE_STATUS_PENDING);
		}
		invoice.setStatus(INVOICE_STATUS_PAID);
		LoggingUtil.logMessage("Updating invoice .. " + invoice.getId() + " status " + invoice.getStatus());
	}
	
	public BillServiceResponse getDailySummary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillVendorDaoImpl dao = new BillVendorDaoImpl(session);
			Integer groupId = null;
			if(request.getCustomerGroup() != null) {
				groupId = request.getCustomerGroup().getId();
			}
			List<Object[]> result = dao.getItemOrderSummary(request.getRequestedDate(), request.getBusiness().getId(), groupId);

			List<BillItem> items = new ArrayList<BillItem>();
			BigDecimal totalPayable = BigDecimal.ZERO;
			BigDecimal totalProfit = BigDecimal.ZERO;
			List<BillUser> users = new ArrayList<BillUser>();
			if (CollectionUtils.isNotEmpty(result)) {
				for (Object[] row : result) {
					if (ArrayUtils.isEmpty(row)) {
						continue;
					}
					BigDecimal total = (BigDecimal) row[0];
					BillDBItemBusiness businessItem = (BillDBItemBusiness) row[1];
					BillDBOrders order = (BillDBOrders) row[2];
					BillItem item = BillDataConverter.getBusinessItem(new NullAwareBeanUtils(), businessItem);
					BigDecimal costPrice = (BigDecimal) row[3];
					BigDecimal sellingPrice = (BigDecimal) row[4];
					if (item != null) {
						item.setQuantity(CommonUtils.formatDecimal(total));
						item.setCostPrice(costPrice);
						item.setPrice(sellingPrice);
						if (total != null && BigDecimal.ZERO.compareTo(total) < 0) {
							if (costPrice != null) {
								item.setUnitCostPrice(costPrice.divide(total, RoundingMode.HALF_UP));
								totalPayable = totalPayable.add(costPrice);
							}
							if (sellingPrice != null) {
								item.setUnitSellingPrice(sellingPrice.divide(total, RoundingMode.HALF_UP));
								totalProfit = totalProfit.add(sellingPrice);
							}
						}

						if (StringUtils.equalsIgnoreCase(ACCESS_DISTRIBUTOR, request.getRequestType())) {
							List<BillDBItemBusiness> distributors = dao.getBusinessesByItemAccess(businessItem.getParent().getId(), ACCESS_DISTRIBUTOR,
									new ArrayList<BillDBLocation>(businessItem.getBusiness().getLocations()));
							if (CollectionUtils.isNotEmpty(distributors)) {
								for (BillDBItemBusiness business : distributors) {
									BillUser user = new BillUser();
									if (CollectionUtils.isNotEmpty(users)) {
										//Find if already added
										for (BillUser existing : users) {
											if (existing.getCurrentBusiness().getId() == business.getBusiness().getId()) {
												user = existing;
												break;
											}
										}
									}
									if (user.getCurrentBusiness() == null) { //Not added
										BillBusiness businessBasic = BillDataConverter.getBusinessBasic(business.getBusiness());
										List<BillItem> businessItems = new ArrayList<BillItem>();
										businessItems.add(item);
										businessBasic.setItems(businessItems);
										user.setCurrentBusiness(businessBasic);
										BillInvoice currentInvoice = new BillInvoice();
										currentInvoice.setAmount(item.getPrice());
										currentInvoice.setPayable(item.getCostPrice());
										user.setCurrentInvoice(currentInvoice);
										users.add(user);
									} else { //Already added.. just update
										if (user.getCurrentBusiness().getItems() == null) {
											user.getCurrentBusiness().setItems(new ArrayList<BillItem>());
										}
										user.getCurrentBusiness().getItems().add(item);
										if (user.getCurrentInvoice().getPayable() != null && item.getCostPrice() != null) {
											user.getCurrentInvoice().getPayable().add(item.getCostPrice());
										} else {
											user.getCurrentInvoice().setPayable(item.getCostPrice());
										}
										if (user.getCurrentInvoice().getAmount() != null && item.getPrice() != null) {
											user.getCurrentInvoice().getAmount().add(item.getPrice());
										} else {
											user.getCurrentInvoice().setAmount(item.getPrice());
										}
									}
								}
							}

						} else {
							items.add(item);
						}

						// items.add(item);
					}

				}
			}
			if(CollectionUtils.isNotEmpty(users)) {
				Collections.sort(users, new BillNameSorter());
			}
			if(CollectionUtils.isNotEmpty(items)) {
				Collections.sort(items, new BillNameSorter());
			}
			BillInvoice invoice = new BillInvoice();
			invoice.setPayable(totalPayable);
			invoice.setAmount(totalProfit);
			response.setInvoice(invoice);
			response.setItems(items);
			response.setUsers(users);
		} catch (

		Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getInvoiceSummary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillUser> users = prepareInvoiceSummary(request, session, INVOICE_STATUS_PAID);
			if (!StringUtils.equals(REQUEST_TYPE_EMAIL, request.getRequestType())) {
				response.setUsers(users);
			} else {
				//Send SMS/Email to all customers asynchronously..
				executor.execute(new BillMessageBroadcaster(users, MAIL_TYPE_INVOICE, BillConstants.REQUEST_TYPE_EMAIL));
				executor.execute(new BillMessageBroadcaster(users, MAIL_TYPE_INVOICE, "SMS"));
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	private List<BillUser> prepareInvoiceSummary(BillServiceRequest request, Session session, String status) throws IllegalAccessException, InvocationTargetException {
		Integer year = null, month = null;
		if(request.getInvoice() != null) {
			year = request.getInvoice().getYear();
			month = request.getInvoice().getMonth();
			year = getYear(year, month);
		}
		Integer groupId = null;
		if(request.getCustomerGroup() != null) {
			groupId = request.getCustomerGroup().getId();
		}
		List<Object[]> result = new BillInvoiceDaoImpl(session).getCustomerInvoiceSummary(request.getRequestedDate(), request.getBusiness().getId(),
				month, year, status, groupId);
		List<BillUser> users = new ArrayList<BillUser>();
		if (CollectionUtils.isNotEmpty(result)) {
			for (Object[] row : result) {
				if (ArrayUtils.isEmpty(row)) {
					continue;
				}
				BillDBSubscription subscription = (BillDBSubscription) row[1];
				BillUser customer = new BillUser();
				new NullAwareBeanUtils().copyProperties(customer, subscription);
				if (userNotPresent(request.getUsers(), customer)) {
					continue;
				}
				BigDecimal total = (BigDecimal) row[0];
				if (StringUtils.equals(STATUS_DELETED, subscription.getStatus())) {
					continue;
				}
				BillInvoice invoice = new BillInvoice();
				invoice.setAmount(total);
				if (row[2] != null) {
					invoice.setPendingBalance((BigDecimal) row[2]);
				}
				if (row[3] != null) {
					invoice.setServiceCharge((BigDecimal) row[3]);
				}
				if (row[4] != null) {
					invoice.setCreditBalance((BigDecimal) row[4]);
				}
				if (row[5] != null) {
					Long count = (Long) row[5];
					invoice.setNoOfReminders(count.intValue());
				}
				BillRuleEngine.calculatePayable(invoice, null, null);

				customer.setCurrentInvoice(invoice);
				if (StringUtils.equals(REQUEST_TYPE_EMAIL, request.getRequestType()) && total != null && total.compareTo(BigDecimal.ZERO) > 0) {
					BillDBInvoice lastUnpaid = new BillInvoiceDaoImpl(session).getLatestUnPaidInvoice(subscription.getId());
					if (lastUnpaid != null) {
						BillInvoice lastInvoice = BillDataConverter.getInvoice(new NullAwareBeanUtils(), lastUnpaid);
						BillRuleEngine.calculatePayable(lastInvoice, lastUnpaid, session);
						invoice.setPaymentUrl(BillRuleEngine.preparePaymentUrl(lastUnpaid.getId()));
						if(StringUtils.isBlank(lastUnpaid.getShortUrl())) {
							Transaction tx = session.beginTransaction();
							lastUnpaid.setShortUrl(BillSMSUtil.shortenUrl(null, invoice.getPaymentUrl()));
							tx.commit();
						}
						invoice.setShortUrl(lastUnpaid.getShortUrl());
						//BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_INVOICE);
						customer.setCurrentBusiness(BillDataConverter.getBusiness(subscription.getBusiness()));
						//mailUtil.setUser(customer);
						//mailUtil.setInvoice(lastInvoice);
						customer.setCurrentInvoice(lastInvoice);
						//executor.execute(mailUtil);
						//BillSMSUtil.sendSMS(customer, lastInvoice, MAIL_TYPE_INVOICE, null);
					}
				}
				users.add(customer);
			}
		}
		return users;
	}

	private boolean userNotPresent(List<BillUser> users, BillUser searchUser) {
		if (CollectionUtils.isEmpty(users)) {
			return false;
		}
		for (BillUser user : users) {
			if (searchUser.getId().intValue() == user.getId().intValue()) {
				return false;
			}
		}
		return true;
	}

	public BillServiceResponse getCustomerActivity(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || request.getUser().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillLogDAOImpl billLogDAOImpl = new BillLogDAOImpl(session);
			BillDBSubscription userSubscription = new BillSubscriptionDAOImpl(session).getSubscriptionDetails(request.getUser().getId());
			if (userSubscription != null) {
				Set<BillUserLog> logs = new HashSet<BillUserLog>();
				BillDBUserLog userLog = new BillDBUserLog();
				Date monthFirstDate = CommonUtils.getMonthFirstDate(request.getInvoice().getMonth(), request.getInvoice().getYear());
				userLog.setFromDate(monthFirstDate);
				Date monthLastDate = CommonUtils.getMonthLastDate(request.getInvoice().getMonth(), request.getInvoice().getYear());
				userLog.setToDate(monthLastDate);
				userLog.setBusiness(userSubscription.getBusiness());
				List<BillUserLog> businessItemLogs = BillUserLogUtil.getBillUserLogs(billLogDAOImpl.getLogsBetweenRange(userLog));
				if (CollectionUtils.isNotEmpty(businessItemLogs)) {
					logs.addAll(businessItemLogs);
				}
				userLog.setBusiness(null);
				userLog.setSubscription(userSubscription);
				List<BillUserLog> subscribedItemLogs = BillUserLogUtil.getBillUserLogs(billLogDAOImpl.getLogsBetweenRange(userLog));
				if (CollectionUtils.isNotEmpty(subscribedItemLogs)) {
					logs.addAll(subscribedItemLogs);
				}
				if (CollectionUtils.isNotEmpty(userSubscription.getSubscriptions())) {
					userLog.setSubscription(null);
					for (BillDBItemSubscription subItem : userSubscription.getSubscriptions()) {
						if (subItem.getBusinessItem().getParent() != null) {
							userLog.setParentItem(subItem.getBusinessItem().getParent());
							List<BillUserLog> parentItemLogs = BillUserLogUtil.getBillUserLogs(billLogDAOImpl.getLogsBetweenRange(userLog));
							if (CollectionUtils.isNotEmpty(parentItemLogs)) {
								logs.addAll(parentItemLogs);
							}
						}

					}
				}
				response.setLogs(BillDataConverter.formatLogs(logs));

				List<BillDBOrders> orders = new BillSubscriptionDAOImpl(session).getOrders(monthFirstDate, monthLastDate, userSubscription.getId());
				List<BillOrder> ordersList = new ArrayList<BillOrder>();
				if (CollectionUtils.isNotEmpty(orders)) {
					for (BillDBOrders dbOrder : orders) {
						BillOrder order = new BillOrder();
						new NullAwareBeanUtils().copyProperties(order, dbOrder);
						order.setOrderDateString(CommonUtils.convertDate(order.getOrderDate(), DATE_FORMAT_DISPLAY_NO_YEAR));
						order.setItems(BillDataConverter.getOrderItems(dbOrder.getOrderItems()));
						ordersList.add(order);

					}
					response.setOrders(ordersList);
				}
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getFile(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			if (StringUtils.equalsIgnoreCase("logo", request.getRequestType())) {
				BillDBUserBusiness dbBusiness = dao.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), true);
				if (dbBusiness != null && StringUtils.isNotBlank(dbBusiness.getLogoImg())) {
					BillFile image = new BillFile();
					image.setFileName(CommonUtils.getFileName(dbBusiness.getLogoImg()));
					image.setFileData(new FileInputStream(dbBusiness.getLogoImg()));
					response.setFile(image);
				}
			}

		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getTransactions(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Integer month = CommonUtils.getCalendarValue(new Date(), Calendar.MONTH);
			Integer year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
			if (request.getInvoice() != null && request.getInvoice().getMonth() != null && request.getInvoice().getYear() != null) {
				month = request.getInvoice().getMonth();
				year = request.getInvoice().getYear();
			}
			BillUserLog log = new BillUserLog();
			if(request.getItem() == null || request.getItem().getChangeLog() == null) {
				log.setFromDate(CommonUtils.getMonthFirstDate(month, year));
				log.setToDate(CommonUtils.getMonthLastDate(month, year));
			} else {
				log = request.getItem().getChangeLog();
			}
			Integer groupId = null;
			if(request.getCustomerGroup() != null) {
				groupId = request.getCustomerGroup().getId();
			}
			List<BillDBTransactions> transactions = new BillTransactionsDaoImpl(session).getTransactions(log, request.getBusiness().getId(), groupId);
			List<BillUser> users = BillDataConverter.getTransactions(transactions);
			response.setUsers(users);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getCustomerBillsSummary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getInvoice() == null || request.getInvoice().getMonth() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			
			List<BillDBItemBusiness> items = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBItemBusiness.class, "business.id", request.getBusiness().getId(), true, null, null);
			response.setItems(BillDataConverter.getBusinessItems(items));
			List<BillUser> users = new ArrayList<BillUser>();
			
			if(request.getItem() == null || request.getItem().getParentItemId() == null) {
				//Get data for full invoices
				users = prepareInvoiceSummary(request, session, null);
				//Collections.sort(users, new BillNameSorter());
				response.setUsers(users);
				return response;
			} 
			
			Integer year = null, month = null;
			if(request.getInvoice() != null) {
				year = request.getInvoice().getYear();
				month = request.getInvoice().getMonth();
				year = getYear(year, month);
			}
			Integer groupId = null;
			if(request.getCustomerGroup() != null) {
				groupId = request.getCustomerGroup().getId();
			}
			
			List<Object[]> rows = new BillVendorDaoImpl(session).getBillSummary(request.getBusiness().getId(), request.getItem().getParentItemId(), month, year, groupId);
			for(Object[] row: rows) {
				BillDBItemInvoice itemI = (BillDBItemInvoice) row[0];
				BillDBInvoice invoice = (BillDBInvoice) row[1];
				BillDBSubscription subscription = (BillDBSubscription) row[2];
				NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
				BillInvoice itemInvoice = BillDataConverter.getInvoice(beanUtils, invoice);
				List<BillItem> invoiceItems = new ArrayList<BillItem>();
				BillItem invoiceItem = BillDataConverter.getInvoiceItem(beanUtils, itemInvoice, itemI);
				BillUser customer = BillDataConverter.getCustomerDetails(beanUtils, subscription);
				if(customer != null) {
					if(customer.getCurrentSubscription() != null && CollectionUtils.isNotEmpty(customer.getCurrentSubscription().getItems())) {
						for(BillItem item: customer.getCurrentSubscription().getItems()) {
							if(item.getParentItemId() != null && invoiceItem.getParentItem() != null && invoiceItem.getParentItem().getId() == item.getParentItemId() && item.getPrice() != null) {
								invoiceItem.setSchemeStartDate(item.getSchemeStartDate());
								invoiceItem.setSchemeEndDate(item.getSchemeEndDate());
								invoiceItem.setPaymentRef(CommonUtils.getStringValue(item.getPaymentRef()));
							}
						}
					}
					invoiceItems.add(invoiceItem);
					itemInvoice.setAmount(itemI.getPrice());
					itemInvoice.setInvoiceItems(invoiceItems);
					customer.setCurrentInvoice(itemInvoice);
					users.add(customer);
					//CommonUtils.addIfNotPresent(customer, users);
				}
			}
			Collections.sort(users, new BillNameSorter());
			response.setUsers(users);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	private Integer getYear(Integer year, Integer month) {
		if(month != null && year == null) {
			year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
			Date date = CommonUtils.getDate(month, year);
			if(date != null && date.compareTo(new Date()) > 0) {
				year = year - 1;
			}
		}
		return year;
	}

	public BillServiceResponse getAllSectors() {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBSector> dbSectors = dao.getEntities(BillDBSector.class, true, "name", "asc");
			List<BillSector> sectors = BillDataConverter.getSectors(dbSectors);
			response.setSectors(sectors);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse updateInvoiceItems(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getInvoice() == null || request.getItem() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			if(request.getItem() != null) {
				if(request.getItem().getParentItemId() != null && request.getItem().getPrice() != null) {
					//Update all invoice items for this business item, useful for bulk bill correction
					Integer year = getYear(request.getInvoice().getYear(), request.getInvoice().getMonth());
					List<BillDBItemInvoice> invoiceItems = new BillInvoiceDaoImpl(session).getInvoiceItems(request.getInvoice().getMonth(), year, request.getItem().getParentItemId(), request.getItem().getPriceType());
					if(CollectionUtils.isNotEmpty(invoiceItems)) {
						for(BillDBItemInvoice invoiceItem: invoiceItems) {
							if(invoiceItem.getQuantity() == null) {
								continue;
							}
							BigDecimal oldVal = invoiceItem.getPrice();
							BigDecimal difference = request.getItem().getPrice().subtract(oldVal);
							invoiceItem.getInvoice().setAmount(invoiceItem.getInvoice().getAmount().add(difference));
							invoiceItem.setPrice(request.getItem().getPrice());
						}
					}
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

	public BillServiceResponse updateCustomerGroup(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getCustomerGroup() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillCustomerGroup group = request.getCustomerGroup();
			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
			if(group.getId() == null) {
				BillDBCustomerGroup dbGroup = new BillDBCustomerGroup();
				nullAwareBeanUtils.copyProperties(dbGroup, group);
				dbGroup.setCreatedDate(new Date());
				dbGroup.setStatus(STATUS_ACTIVE);
				if(request.getBusiness() != null) {
					BillDBUserBusiness userBusiness = billGenericDaoImpl.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), true);
					if(userBusiness != null) {
						dbGroup.setBusiness(userBusiness);
						session.persist(dbGroup);
					}
				}
			} else {
				BillDBCustomerGroup dbGroup = billGenericDaoImpl.getEntityByKey(BillDBCustomerGroup.class, ID_ATTR, group.getId(), true);
				if(dbGroup != null) {
					nullAwareBeanUtils.copyProperties(dbGroup, group);
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

	public BillServiceResponse updateGroupCustomers(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getCustomerGroup() == null || CollectionUtils.isEmpty(request.getUsers())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			
			for(BillUser customer: request.getUsers()) {
				if(customer.getCurrentSubscription() != null && customer.getCurrentSubscription().getGroup() != null) {
					BillDBSubscription subscription = new BillGenericDaoImpl(session).getEntityByKey(BillDBSubscription.class, ID_ATTR, customer.getId(), true);
					if(subscription != null) {
						subscription.setGroupSequence(customer.getCurrentSubscription().getGroup().getSequenceNumber());
					}
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

	public BillServiceResponse getAllCustomerGroups(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillDBCustomerGroup> customerGroups = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBCustomerGroup.class, "business.id", request.getBusiness().getId(), true, "groupName", "asc");
			List<BillCustomerGroup> groups = new ArrayList<BillCustomerGroup>();
			if(CollectionUtils.isNotEmpty(customerGroups)) {
				for(BillDBCustomerGroup customerGroup: customerGroups) {
					BillCustomerGroup group = new BillCustomerGroup();
					new NullAwareBeanUtils().copyProperties(group, customerGroup);
					Map<String, Object> restrictions = new HashMap<String, Object>();
					restrictions.put("customerGroup.id", group.getId());
					//No of users in the group
					Long count = (Long) new BillGenericDaoImpl(session).getSum(BillDBSubscription.class, ID_ATTR, restrictions, null, null, "count", null, null);
					if(count != null) {
						group.setNoOfCustomers(count.intValue());
					}
					groups.add(group);
				}
			}
			response.setGroups(groups);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getPaymentsReport(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getBusiness().getId() == null || request.getItem().getChangeLog() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		BillUserLog log = request.getItem().getChangeLog();
		
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillAdminDashboard dashboard = new BillAdminDashboard();
			Map<String, Object> restrictions = new HashMap<String, Object>();
			Date startDate = log.getFromDate();
			Date endDate = log.getToDate();
			BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
			
			List<BillMyCriteria> criteriaList = new ArrayList<BillMyCriteria>();
			
			Map<String, Object> keys = new HashMap<String, Object>();
			keys.put("business.id", request.getBusiness().getId());
			if(request.getCustomerGroup() != null) {
				keys.put("customerGroup.id", request.getCustomerGroup().getId());
			}
			criteriaList.add(new BillMyCriteria("subscription", keys));
			
			restrictions.put("status", BillConstants.INVOICE_STATUS_PAID);
			
			//Count of offline/online
			restrictions.put("paymentType", PAYMENT_OFFLINE);
			dashboard.setOfflineInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count", "paidDate", criteriaList));
			restrictions.put("paymentType", PAYMENT_ONLINE);
			dashboard.setOnlineInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count", "paidDate", criteriaList));
			restrictions.remove("paymentType");
			
			//restrictions.remove("status");
			//Amount of online/offline
			restrictions.put("status", INVOICE_STATUS_PAID);
			restrictions.put("paymentMode", PAYMENT_OFFLINE);
			dashboard.setOfflinePaid((BigDecimal) billGenericDaoImpl.getSum(BillDBTransactions.class, "amount", restrictions, startDate, endDate, "sum", "createdDate", criteriaList));
			restrictions.remove("paymentMode");
			restrictions.put("!paymentMode", PAYMENT_OFFLINE);
			dashboard.setOnlinePaid((BigDecimal) billGenericDaoImpl.getSum(BillDBTransactions.class, "amount", restrictions, startDate, endDate, "sum", "createdDate", criteriaList));
			restrictions.remove("!paymentMode");
			
			//Pending
			restrictions.put("status", BillConstants.INVOICE_STATUS_PENDING);
			dashboard.setPendingInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count", null, criteriaList));
			dashboard.setPendingAmount((BigDecimal) billGenericDaoImpl.getSum(BillDBInvoice.class, "amount", restrictions, startDate, endDate, "sum", null, criteriaList));
			//Settled
			restrictions.put("status", BillConstants.INVOICE_SETTLEMENT_STATUS_SETTLED);
			dashboard.setCompletedSettlements((BigDecimal) billGenericDaoImpl.getSum(BillDBTransactions.class, "amount", restrictions, startDate, endDate, "sum", "settlementDate", criteriaList));
			
			//Add completed settlements + pending = Total online payments
			if(dashboard.getCompletedSettlements() != null) {
				if(dashboard.getOnlinePaid() != null) {
					dashboard.setOnlinePaid(dashboard.getOnlinePaid().add(dashboard.getCompletedSettlements()));
				} else {
					dashboard.setOnlinePaid(dashboard.getCompletedSettlements());
				}
			}
			Map<String, Object> bInvoiceRestrictions = new HashMap<String, Object>();
			keys.put("fromBusiness.id", request.getBusiness().getId());
			keys.put("!status", INVOICE_STATUS_DELETED);
			BigDecimal payableAmount = (BigDecimal) billGenericDaoImpl.getSum(BillDBBusinessInvoice.class, "amount", bInvoiceRestrictions, startDate, endDate, "sum", "invoiceDate", null);
			BigDecimal soldAmount = (BigDecimal) billGenericDaoImpl.getSum(BillDBBusinessInvoice.class, "soldAmount", bInvoiceRestrictions, startDate, endDate, "sum", "invoiceDate", null);
			if(payableAmount != null && soldAmount != null) {
				dashboard.setTotalProfit(soldAmount.subtract(payableAmount));
			}
			response.setDashboard(dashboard);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse updateBusinessInvoice(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillInvoice currentInvoice = request.getInvoice();
			BillDBBusinessInvoice businessInvoice = null;
			
			if(currentInvoice.getId() == null) {
				if(request.getBusiness() == null || request.getUser() == null || request.getInvoice() == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
					return response;
				}
				BillDBUserBusiness fromBusiness = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getUser().getCurrentBusiness().getId(), true);
				BillDBUserBusiness toBusiness = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), true);
				//New Invoice
				BillDBBusinessInvoice existingInvoice = new BillInvoiceDaoImpl(session).getInvoiceByDate(currentInvoice.getInvoiceDate(), fromBusiness.getId(), toBusiness.getId());
				if(existingInvoice != null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INVOICE_EXISTS);
					return response;
				}
				businessInvoice = new BillDBBusinessInvoice();
				businessInvoice.setCreatedDate(new Date());
				businessInvoice.setStatus(INVOICE_STATUS_PENDING);
				businessInvoice.setFromBusiness(fromBusiness);
				businessInvoice.setToBusiness(toBusiness);
				session.persist(businessInvoice);
				updateBusinessInvoice(session, currentInvoice, businessInvoice);
			} else {
				businessInvoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBBusinessInvoice.class, ID_ATTR, currentInvoice.getId(), false);
				if(businessInvoice != null) {
					if(StringUtils.equals(BillConstants.INVOICE_STATUS_PAID, currentInvoice.getStatus())) {
						//Cash or UPI paid
						businessInvoice.setPaidDate(new Date());
						//TODO add to transactions
						//TODO notify users
					}
					updateBusinessInvoice(session, currentInvoice, businessInvoice);
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

	private void updateBusinessInvoice(Session session, BillInvoice currentInvoice, BillDBBusinessInvoice businessInvoice)
			throws IllegalAccessException, InvocationTargetException {
		new NullAwareBeanUtils().copyProperties(businessInvoice, currentInvoice);
		if(CollectionUtils.isNotEmpty(currentInvoice.getInvoiceItems())) {
			for(BillItem item: currentInvoice.getInvoiceItems()) {
				BillDBItemBusinessInvoice businessInvoiceItem = null;
				BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
				if(item.getId() == null) {
					businessInvoiceItem = new BillDBItemBusinessInvoice();
					businessInvoiceItem.setStatus(STATUS_ACTIVE);
					businessInvoiceItem.setCreatedDate(new Date());
					BillDBItemBusiness fromBusinessItem = billGenericDaoImpl.getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getParentItemId(), true);
					businessInvoiceItem.setFromBusinessItem(fromBusinessItem);
					if(item.getParentItem() != null) {
						BillDBItemBusiness toBusinessItem = billGenericDaoImpl.getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getParentItem().getId(), true) ;
						businessInvoiceItem.setToBusinessItem(toBusinessItem);
					}
					businessInvoiceItem.setInvoice(businessInvoice);
				} else {
					businessInvoiceItem = billGenericDaoImpl.getEntityByKey(BillDBItemBusinessInvoice.class, ID_ATTR, item.getId(), true);
				}
				if(businessInvoiceItem != null) {
					businessInvoiceItem.setQuantity(item.getQuantity());
					businessInvoiceItem.setPrice(item.getPrice());
					businessInvoiceItem.setUnitSellingPrice(item.getUnitSellingPrice());
					if(businessInvoiceItem.getId() == null) {
						session.persist(businessInvoiceItem);
					}
				}
			}
		}
	}

	public BillServiceResponse getBusinessInvoicesForBusiness(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || request.getUser().getCurrentBusiness() == null || request.getBusiness() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
			BillUserLog log = null;
			if(request.getInvoice() != null && request.getInvoice().getMonth() != null && request.getInvoice().getYear() != null) {
				log = new BillUserLog();
				log.setFromDate(CommonUtils.getMonthFirstDate(request.getInvoice().getMonth(), request.getInvoice().getYear()));
				log.setToDate(CommonUtils.getMonthLastDate(request.getInvoice().getMonth(), request.getInvoice().getYear()));
			} else if(request.getItem() != null) {
				log = request.getItem().getChangeLog();
			}
			List<BillDBBusinessInvoice> invoices = dao.getAllPurchaseInvoices(request.getUser().getCurrentBusiness().getId(), null, log, request.getBusiness().getId());
			//Calculate profit
			BigDecimal totalProfit = BigDecimal.ZERO;
			BigDecimal totalPending = BigDecimal.ZERO;
			List<BillInvoice> businessInvoices = new ArrayList<BillInvoice>();
			if(CollectionUtils.isNotEmpty(invoices)) {
				NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
				for(BillDBBusinessInvoice invoice: invoices) {
					BillInvoice businessInvoice = new BillInvoice();
					nullAwareBeanUtils.copyProperties(businessInvoice, invoice);
					businessInvoice.setPayable(invoice.getAmount());
					if(businessInvoice.getSoldAmount() != null && businessInvoice.getPayable() != null) {
						totalProfit = totalProfit.add(businessInvoice.getSoldAmount().subtract(businessInvoice.getPayable()));
					}
					if(StringUtils.equals(INVOICE_STATUS_PENDING, businessInvoice.getStatus()) && businessInvoice.getPayable() != null) {
						totalPending = totalPending.add(businessInvoice.getPayable());
					}
					if(CollectionUtils.isNotEmpty(invoice.getItems())) {
						List<BillItem> invoiceItems = new ArrayList<BillItem>();
						for(BillDBItemBusinessInvoice item: invoice.getItems()) {
							BillItem invoiceItem = new BillItem();
							if(item.getFromBusinessItem() != null && item.getFromBusinessItem().getParent() != null) {
								nullAwareBeanUtils.copyProperties(invoiceItem, item.getFromBusinessItem().getParent());
								invoiceItem.setParentItemId(item.getFromBusinessItem().getId());
							}
							nullAwareBeanUtils.copyProperties(invoiceItem, item);
							invoiceItems.add(invoiceItem);
						}
						businessInvoice.setInvoiceItems(invoiceItems);
					}
					businessInvoices.add(businessInvoice);
				}
			}
			BillInvoice result = new BillInvoice();
			result.setPayable(totalPending);
			result.setSoldAmount(totalProfit);
			response.setInvoice(result);
			response.setInvoices(businessInvoices);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getBusinessesByType(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUserBusiness userBusiness = dao.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), true);
			if(CollectionUtils.isEmpty(userBusiness.getLocations())) {
				response.setResponse(ERROR_CODE_GENERIC, "Please setup locations in your profile!");
				return response;
			}
			String businessType = ACCESS_DISTRIBUTOR;
			if(StringUtils.equals(ACCESS_DISTRIBUTOR, userBusiness.getType())) {
				businessType = "!" + ACCESS_DISTRIBUTOR;
			}
			List<BillDBItemBusiness> items = dao.getEntitiesByKey(BillDBItemBusiness.class, "business.id", request.getBusiness().getId(), true, null, null);
			List<BillDBUserBusiness> businessesByType = new BillVendorDaoImpl(session).getBusinessesByType(businessType, new ArrayList<BillDBLocation>(userBusiness.getLocations()),  items);
			List<BillUser> businesses = new ArrayList<BillUser>();
			if(CollectionUtils.isEmpty(businessesByType)) {
				response.setResponse(ERROR_CODE_GENERIC, "Please contact our team to enable this feature for your location. Once enabled you will be able to track and pay your purchase online.");
				return response;
			}
			if(CollectionUtils.isNotEmpty(businessesByType)) {
				for(BillDBUserBusiness business: businessesByType) {
					BillBusiness currentBusiness = BillDataConverter.getBusiness(business);
					BillUser user = currentBusiness.getOwner();
					Integer toBusinessId = business.getId();
					Integer fromBusinessId = request.getBusiness().getId();
					//If request is from the distributor
					if(StringUtils.equals(ACCESS_DISTRIBUTOR, userBusiness.getType())) {
						//Get total pending for this business
						fromBusinessId = business.getId();
						toBusinessId = request.getBusiness().getId();
					} else {
						//Load financial for UPI transfer
						BillDataConverter.setUserFinancials(response, session, business.getUser(), user, new NullAwareBeanUtils());
					}
					
					BigDecimal sum = (BigDecimal) new BillInvoiceDaoImpl(session).getTotalPendingForBusiness(toBusinessId, fromBusinessId, null);
					if(user != null) {
						if(CollectionUtils.isNotEmpty(businesses)) {
							boolean found = false;
							for(BillUser existing: businesses) {
								if(existing.getId().intValue() == user.getId().intValue()) {
									found = true;
									break;
								}
							}
							if(found) {
								continue;
							}
						}
						currentBusiness.setOwner(null);
						List<BillItem> businessItems = BillDataConverter.getBusinessItems(new ArrayList<BillDBItemBusiness>(business.getBusinessItems()));
						currentBusiness.setItems(businessItems);
						BillInvoice currentInvoice = new BillInvoice();
						currentInvoice.setPayable(sum);
						user.setCurrentInvoice(currentInvoice);
						user.setCurrentBusiness(currentBusiness);
						businesses.add(user);
					}
				}
			}
			response.setUsers(businesses);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getBusinessItemsByDate(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getUser() == null || request.getUser().getCurrentBusiness() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillDBItemBusiness> items = new BillVendorDaoImpl(session).getBusinessItems(request.getBusiness().getId());
			List<BillDBItemBusiness> distributorItems = new BillVendorDaoImpl(session).getBusinessItems(request.getUser().getCurrentBusiness().getId());
			if(CollectionUtils.isEmpty(items) || CollectionUtils.isEmpty(distributorItems)) {
				return response;
			}
			List<BillItem> businessItems = BillDataConverter.getBusinessItems(distributorItems);
			List<BillItem> responseItems = new ArrayList<BillItem>();
			BillInvoice returnInvoice = new BillInvoice();
			if(request.getRequestedDate() != null) {
				Calendar dayBefore = Calendar.getInstance();
				dayBefore.setTime(request.getRequestedDate());
				dayBefore.add(Calendar.DATE, -1);
				//Find return price
				BillDBBusinessInvoice existingInvoice = new BillInvoiceDaoImpl(session).getInvoiceByDate(dayBefore.getTime(), request.getBusiness().getId(), request.getUser().getCurrentBusiness().getId());
				if(existingInvoice != null) {
					//Create return JSON
					if(CollectionUtils.isNotEmpty(existingInvoice.getItems())) {
						List<BillItem> returnItems = new ArrayList<BillItem>();
						for(BillDBItemBusinessInvoice bItem: existingInvoice.getItems()) {
							if(StringUtils.equals(STATUS_DELETED, bItem.getStatus())) {
								continue;
							}
							BillItem returnItem = new BillItem();
							returnItem.setCostPrice(bItem.getPrice());
							returnItem.setParentItemId(bItem.getFromBusinessItem().getId());
							returnItem.setUnitSellingPrice(bItem.getUnitSellingPrice());
							BillItem parentItem = new BillItem();
							parentItem.setId(bItem.getToBusinessItem().getId());
							returnItem.setParentItem(parentItem);
							returnItems.add(returnItem);
						}
						returnInvoice.setInvoiceItems(returnItems);
					}
				}
			}
			
			List<BillItem> returnItems = new ArrayList<BillItem>();
			if (CollectionUtils.isNotEmpty(businessItems)) {
				for (BillItem distributorItem : businessItems) {
					BillItem parentItem = distributorItem.getParentItem();
					// Find parent that matches the business owner
					if (CollectionUtils.isNotEmpty(items)) {
						boolean found = false;
						for (BillDBItemBusiness businessItem : items) {
							if (businessItem.getParent() != null && businessItem.getParent().getId().intValue() == parentItem.getId().intValue()) {
								distributorItem.setParentItemId(businessItem.getId());
								found = true;
								break;
							}
						}
						if(!found) {
							continue;
						}
					}
					
					if (parentItem != null) {
						Calendar cal = Calendar.getInstance();
						//Set the date
						if(request.getRequestedDate() != null) {
							cal.setTime(request.getRequestedDate());
						}
						BigDecimal costPrice = getCostPrice(parentItem, cal, "CP");
						distributorItem.setUnitSellingPrice(getCostPrice(parentItem, cal, "SP"));
						distributorItem.setCostPrice(costPrice);
						//Calculate the return price also
						if(CollectionUtils.isEmpty(returnInvoice.getInvoiceItems())) {
							cal.add(Calendar.DATE, -1);
							BigDecimal returnPrice = getCostPrice(parentItem, cal, "CP");
							BillItem returnItem = new BillItem();
							returnItem.setParentItemId(distributorItem.getParentItemId());
							returnItem.setParentItem(parentItem);
							returnItem.setCostPrice(returnPrice);
							returnItem.setUnitSellingPrice(getCostPrice(parentItem, cal, "SP"));
							returnItems.add(returnItem);
						}
					}
					if(distributorItem.getCostPrice() != null) {
						responseItems.add(distributorItem);
					}
				}
				if(CollectionUtils.isEmpty(returnInvoice.getInvoiceItems())) {
					returnInvoice.setInvoiceItems(returnItems);
				}
			}
			response.setInvoice(returnInvoice);
			response.setItems(responseItems);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	private BigDecimal getCostPrice(BillItem parentItem, Calendar cal, String type) {
		BigDecimal value = null;
		String weeklyPricing = parentItem.getWeeklyCostPrice();
		BigDecimal price = parentItem.getCostPrice();
		if(StringUtils.equals("SP", type)) {
			weeklyPricing = parentItem.getWeeklyPricing();
			price = parentItem.getPrice();
		}
		if (StringUtils.equals(FREQ_DAILY, parentItem.getFrequency()) && StringUtils.isNotBlank(parentItem.getWeekDays())
				/*&& StringUtils.isNotBlank(parentItem.getWeeklyPricing())*/) {
			// Calculate cost price
			value = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_WEEK), parentItem.getWeekDays(),
							weeklyPricing, price);
			
		} else if ((StringUtils.equals(FREQ_WEEKLY, parentItem.getFrequency()) || StringUtils.equals(FREQ_MONTHLY, parentItem.getFrequency()))
				&& StringUtils.isNotBlank(parentItem.getMonthDays()) /*&& StringUtils.isNotBlank(parentItem.getWeeklyPricing())*/) {
			// Calculate cost price
			value = BillRuleEngine.calculatePricing(cal.get(Calendar.DAY_OF_MONTH), parentItem.getMonthDays(),
							weeklyPricing, price);
		}
		return value;
	}

}
