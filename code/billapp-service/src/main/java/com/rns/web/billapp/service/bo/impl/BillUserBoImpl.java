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
import java.util.HashSet;
import java.util.List;
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
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillOrder;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
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
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.domain.BillDBUserLog;
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
import com.rns.web.billapp.service.util.BillNameSorter;
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

	private static Set<Integer> invoicesInProgress = new ConcurrentSkipListSet<Integer>();

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
		if (user == null || user.getCurrentSubscription() == null) {
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
			if (dbSubscription == null) {
				dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(user.getPhone(), request.getBusiness().getId());
				if (dbSubscription == null) {
					dbSubscription = new BillDBSubscription();
				}
				dbSubscription.setCreatedDate(new Date());
				dbSubscription.setStatus(STATUS_ACTIVE);
			}
			notNullBean.copyProperties(dbSubscription, user);
			if (user.getCurrentSubscription().getServiceCharge() != null) {
				dbSubscription.setServiceCharge(user.getCurrentSubscription().getServiceCharge());
			}
			if (user.getCurrentSubscription().getArea() != null) {
				BillDBLocation location = new BillDBLocation();
				location.setId(user.getCurrentSubscription().getArea().getId());
				dbSubscription.setLocation(location);
			}
			if (dbSubscription.getBusiness() == null && business != null) {
				BillDBUserBusiness dbBusiness = dao.getEntityByKey(BillDBUserBusiness.class, ID_ATTR, business.getId(), true);
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
					List<BillDBSubscription> subscriptions = new BillSubscriptionDAOImpl(session).getBusinessSubscriptions(itemBusiness.getBusiness().getId());
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
				if (!StringUtils.equals(invoice.getStatus(), dbInvoice.getStatus())) {
					if (StringUtils.equals(INVOICE_STATUS_PAID, invoice.getStatus())) {
						invoicePaid = true;
						BillRuleEngine.calculatePayable(invoice, null, null);
						dbInvoice.setPaidAmount(invoice.getPayable());
						dbInvoice.setPaidDate(new Date());
						dbInvoice.setPaymentType(BillConstants.PAYMENT_OFFLINE);
						dbInvoice.setStatus(invoice.getStatus());
						dbInvoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_CASH);
						dbInvoice.setPaymentMode(BillConstants.PAYMENT_OFFLINE);
						dbInvoice.setSettlementStatus(INVOICE_STATUS_PAID);
						dbInvoice.setSettlementDate(new Date());
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
			BillBusinessConverter.setInvoiceItems(invoice, session, dbInvoice);
			if (customerSubscription != null) {
				nullAware.copyProperties(invoice, dbInvoice);
				if (invoicePaid) {
					// updateTransactionLog(session, dbInvoice);
					sendEmails(invoice, dbInvoice, nullAware);
				}
				// Update payment URL
				BillBusinessConverter.updatePaymentURL(invoice, dbInvoice, customerSubscription, session);
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
		if (request.getBusiness() == null || request.getBusiness().getId() == null || request.getRequestedDate() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillUser> users = new ArrayList<BillUser>();
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			List<BillDBOrders> orders = new BillVendorDaoImpl(session).getOrders(request.getRequestedDate(), request.getBusiness().getId());
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
			Collections.sort(users, new BillNameSorter());
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
				BillInvoiceDaoImpl billInvoiceDaoImpl = new BillInvoiceDaoImpl(session);
				currentSubscription.setBillsDue(billInvoiceDaoImpl.getInvoiceCountByStatus(currentSubscription.getId(), INVOICE_STATUS_PENDING));
				BillDBInvoice latestPaid = billInvoiceDaoImpl.getLatestPaidInvoice(currentSubscription.getId());
				if (latestPaid != null) {
					currentSubscription.setLastBillPaid(latestPaid.getPaidDate());
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
			if (dbInvoice != null && dbInvoice.getAmount() != null && dbInvoice.getMonth() != null && dbInvoice.getYear() != null) {
				BillDBUser vendor = null;
				BillUser customer = new BillUser();
				NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
				invoice = BillDataConverter.getInvoice(beanUtils, dbInvoice);
				BillPaymentCredentials credentials = new BillPaymentCredentials();
				if (dbInvoice.getSubscription() != null) {
					beanUtils.copyProperties(customer, dbInvoice.getSubscription());
					BillBusiness business = BillDataConverter.getBusinessBasic(dbInvoice.getSubscription().getBusiness());
					customer.setCurrentBusiness(business);
					vendor = dbInvoice.getSubscription().getBusiness().getUser();
					BillDataConverter.setCredentials(vendor, credentials);
					BillRuleEngine.calculatePayable(invoice, dbInvoice, session);
				}
				if (!StringUtils.equalsIgnoreCase(REQUEST_TYPE_EMAIL, request.getRequestType())) {
					Integer paymentAttempt = dbInvoice.getPaymentAttempt();
					if (paymentAttempt == null) {
						paymentAttempt = 0;
					}
					paymentAttempt++;
					dbInvoice.setPaymentAttempt(paymentAttempt);
					BillPaymentUtil.prepareHdfcRequest(invoice, customer);
					BillPaymentUtil.prepareCashFreeSignature(invoice, customer, paymentAttempt);
					BillPaymentUtil.prepareAtomRequest(invoice, vendor);
					// Only if InstaMojo payment request is not already
					// generated
					if (StringUtils.isBlank(invoice.getPaymentUrl())) {
						BillBusinessConverter.updatePaymentURL(invoice, dbInvoice, vendor, customer, credentials);
					}
				} else {
					invoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_LINK) + invoice.getId());
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
			Transaction tx = session.beginTransaction();
			if (invoicesInProgress.contains(currentInvoice.getId())) {
				LoggingUtil.logMessage("Already working with this invoice .." + currentInvoice.getId() + " Invoices in progress = " + invoicesInProgress);
				return response;
			}
			invoicesInProgress.add(currentInvoice.getId()); // Locked
			LoggingUtil.logMessage("Locked .. " + currentInvoice.getId() + " -- " + invoicesInProgress);
			BillDBTransactions existingTransction = new BillGenericDaoImpl(session).getEntityByKey(BillDBTransactions.class, "paymentId",
					currentInvoice.getPaymentId(), false);
			if (existingTransction != null) {
				LoggingUtil.logMessage("Already transacted with this invoice .." + currentInvoice.getId() + " PID " + currentInvoice.getPaymentId() + " txn "
						+ existingTransction.getId());
				return response;
			}

			BillDBInvoice invoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBInvoice.class, "paymentRequestId",
					currentInvoice.getPaymentRequestId(), false);
			if (invoice == null) {
				invoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBInvoice.class, ID_ATTR, currentInvoice.getId(), false);
				invoice.setPaymentRequestId(currentInvoice.getPaymentRequestId());
			}
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
				
				List<BillDBSchemes> rewardSchemes = new BillSchemesDaoImpl(session).getSchemes(SCHEME_TYPE_REWARD);
				if(CollectionUtils.isNotEmpty(rewardSchemes)) {
					BillDBSchemes schemes = rewardSchemes.get(0);
					if(validateScheme(schemes, invoice, session)) {
						BillDBCustomerCoupons coupon = BillBusinessConverter.getCustomerCoupon(schemes, invoice.getSubscription(), invoice);
						session.persist(coupon);
						BillRuleEngine.redeemCoupon(session, coupon, executor);
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
			sendEmails(currentInvoice, invoice, nullAwareBeanUtils);
			
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
		LoggingUtil.logMessage("Updating invoice .. " + invoice.getId());
	}

	private void sendEmails(BillInvoice currentInvoice, BillDBInvoice invoice, NullAwareBeanUtils nullAwareBeanUtils)
			throws IllegalAccessException, InvocationTargetException {
		BillUser customer = new BillUser();
		nullAwareBeanUtils.copyProperties(customer, invoice.getSubscription());
		BillUser vendor = new BillUser();
		nullAwareBeanUtils.copyProperties(vendor, invoice.getSubscription().getBusiness().getUser());
		vendor.setName(customer.getName());
		BillBusiness business = new BillBusiness();
		nullAwareBeanUtils.copyProperties(business, invoice.getSubscription().getBusiness());
		customer.setCurrentBusiness(business);
		BillMailUtil customerMail = new BillMailUtil(MAIL_TYPE_PAYMENT_RESULT);
		customerMail.setUser(customer);
		currentInvoice.setPayable(invoice.getPaidAmount());
		customerMail.setInvoice(currentInvoice);
		BillMailUtil vendorMail = new BillMailUtil(MAIL_TYPE_PAYMENT_RESULT_VENDOR);
		vendorMail.setUser(vendor);
		vendorMail.setInvoice(currentInvoice);
		if (StringUtils.isNotBlank(customer.getEmail())) {
			executor.execute(customerMail);
		}
		if (StringUtils.isNotBlank(vendor.getEmail())) {
			executor.execute(vendorMail);
		}
		BillSMSUtil.sendSMS(customer, currentInvoice, MAIL_TYPE_PAYMENT_RESULT, null);
		BillSMSUtil.sendSMS(vendor, currentInvoice, MAIL_TYPE_PAYMENT_RESULT_VENDOR, null);
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
			List<Object[]> result = dao.getItemOrderSummary(request.getRequestedDate(), request.getBusiness().getId());

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
			if(month != null && year == null) {
				year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
				Date date = CommonUtils.getDate(month, year);
				if(date != null && date.compareTo(new Date()) > 0) {
					year = year - 1;
				}
			}
		}
		List<Object[]> result = new BillInvoiceDaoImpl(session).getCustomerInvoiceSummary(request.getRequestedDate(), request.getBusiness().getId(),
				month, year, status);
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
				BillRuleEngine.calculatePayable(invoice, null, null);

				customer.setCurrentInvoice(invoice);
				users.add(customer);
				if (StringUtils.equals(REQUEST_TYPE_EMAIL, request.getRequestType()) && total != null && total.compareTo(BigDecimal.ZERO) > 0) {
					BillDBInvoice lastUnpaid = new BillInvoiceDaoImpl(session).getLatestUnPaidInvoice(subscription.getId());
					if (lastUnpaid != null) {
						BillInvoice lastInvoice = BillDataConverter.getInvoice(new NullAwareBeanUtils(), lastUnpaid);
						BillRuleEngine.calculatePayable(lastInvoice, lastUnpaid, session);
						invoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_LINK) + lastUnpaid.getId());
						BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_INVOICE);
						customer.setCurrentBusiness(BillDataConverter.getBusiness(subscription.getBusiness()));
						mailUtil.setUser(customer);
						mailUtil.setInvoice(lastInvoice);
						executor.execute(mailUtil);
						BillSMSUtil.sendSMS(customer, lastInvoice, MAIL_TYPE_INVOICE, null);
					}
				}
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
			log.setFromDate(CommonUtils.getMonthFirstDate(month, year));
			log.setToDate(CommonUtils.getMonthLastDate(month, year));
			List<BillDBTransactions> transactions = new BillTransactionsDaoImpl(session).getTransactions(log, request.getBusiness().getId());
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
				Collections.sort(users, new BillNameSorter());
				response.setUsers(users);
				return response;
			} 
			
			Integer year = null, month = null;
			if(request.getInvoice() != null) {
				year = request.getInvoice().getYear();
				month = request.getInvoice().getMonth();
				if(month != null && year == null) {
					year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
					Date date = CommonUtils.getDate(month, year);
					if(date != null && date.compareTo(new Date()) > 0) {
						year = year - 1;
					}
				}
			}
			
			List<Object[]> rows = new BillVendorDaoImpl(session).getBillSummary(request.getBusiness().getId(), request.getItem().getParentItemId(), month, year);
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

}
