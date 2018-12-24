package com.rns.web.billapp.service.bo.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillBusinessBo;
import com.rns.web.billapp.service.bo.domain.BillAdminDashboard;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.BillSMSUtil;
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
				dbScheme = billGenericDaoImpl.getEntityByKey(BillDBSchemes.class, "schemeCode", scheme.getSchemeCode(), true);
				if(dbScheme != null) {
					response.setResponse(ERROR_CODE_GENERIC, "Scheme already exists with the same code! Please use a different code!");
					return response;
				}
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

	public BillServiceResponse updateCustomerBill(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getInvoice() == null || request.getInvoice().getAmount() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		boolean invoicePaid = false;
		BillDBInvoice dbInvoice = null;
		BillDBSubscription subscription = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillInvoice invoice = request.getInvoice();
			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			BillDBUserBusiness business = null;
			if(invoice.getId() == null) {
				BillUser customer = request.getUser();
				if(StringUtils.isBlank(customer.getPhone())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_NO_CUSTOMER);
					return response;
				}
				String phone = CommonUtils.trimPhoneNumber(customer.getPhone());
				if(phone != null && phone.length() < 10) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_PHONE_NUMBER);
					return response;
				}
				if(phone != null && phone.length() > 12) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_PHONE_NUMBER);
					return response;
				}
				customer.setPhone(phone);
				business = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserBusiness.class, ID_ATTR, request.getBusiness().getId(), false);
				if(business == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCESS_DENIED);
					return response;
				}
				subscription = new BillVendorDaoImpl(session).getCustomerByPhone(request.getBusiness().getId(), phone);
				if(subscription == null) {
					subscription = new BillDBSubscription();
					subscription.setCreatedDate(new Date());
					subscription.setStatus(STATUS_ACTIVE);
					subscription.setBusiness(business);
					nullAwareBeanUtils.copyProperties(subscription, customer);
					session.persist(subscription);
				} else {
					nullAwareBeanUtils.copyProperties(subscription, customer);
				}
				
				if(invoice.getMonth() != null && invoice.getYear() != null) {
					BillDBInvoice existing = new BillInvoiceDaoImpl(session).getActiveInvoiceForMonth(subscription.getId(), invoice.getMonth(), invoice.getYear());
					if(existing != null) {
						response.setResponse(ERROR_CODE_GENERIC, "Invoice already exists for selected month and year!");
						return response;
					}
				}
				
				dbInvoice = new BillDBInvoice();
				nullAwareBeanUtils.copyProperties(dbInvoice, invoice);
				dbInvoice.setSubscription(subscription);
				dbInvoice.setCreatedDate(new Date());
				if(invoice.getInvoiceDate() == null) {
					dbInvoice.setInvoiceDate(new Date());
				}
				if(StringUtils.isBlank(dbInvoice.getStatus())) {
					dbInvoice.setStatus(INVOICE_STATUS_PENDING);
				}
				session.persist(dbInvoice);
				
				if (StringUtils.equals(INVOICE_STATUS_PAID, dbInvoice.getStatus())) {
					BillBusinessConverter.updatePaymentStatusAsPaid(invoice, dbInvoice);
					BillBusinessConverter.updatePaymentTransactionLog(session, dbInvoice, invoice);
					invoicePaid = true;
				}
				
				boolean hasSubscribedItems = false;
				if(invoice.getMonth() != null && invoice.getYear() != null) {
					hasSubscribedItems = true;
					//Invoice is for RECURRING type.. so add customer subscriptions also
					updateSubscribedItems(session, subscription, invoice);
				} else {
					updateInvoiceItems(session, invoice, business);
				}
						
				BillBusinessConverter.setInvoiceItems(invoice, session, dbInvoice, hasSubscribedItems);
			} else {
				dbInvoice = new BillGenericDaoImpl(session).getEntityByKey(BillDBInvoice.class, ID_ATTR, invoice.getId(), false);
				if(dbInvoice == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INVOICE_NOT_FOUND);
					return response;
				}
				if (!StringUtils.equals(invoice.getStatus(), dbInvoice.getStatus())) {
					if (StringUtils.equals(INVOICE_STATUS_PAID, invoice.getStatus())) {
						BillBusinessConverter.updatePaymentStatusAsPaid(invoice, dbInvoice);
						BillBusinessConverter.updatePaymentTransactionLog(session, dbInvoice, invoice);
						invoicePaid = true;
					} else {
						dbInvoice.setStatus(invoice.getStatus());
						BillBusinessConverter.updatePaymentTransactionLog(session, dbInvoice, invoice);
					}
				}
				nullAwareBeanUtils.copyProperties(dbInvoice, invoice);
				
				boolean hasSubscribedItems = false;
				if(invoice.getMonth() != null && invoice.getYear() != null) {
					hasSubscribedItems = true;
					//Invoice is for RECURRING type.. so add customer subscriptions also
					updateSubscribedItems(session, dbInvoice.getSubscription(), invoice);
				} else {
					updateInvoiceItems(session, invoice, dbInvoice.getSubscription().getBusiness());
				}
				BillBusinessConverter.setInvoiceItems(invoice, session, dbInvoice, hasSubscribedItems);
			}
			if(dbInvoice != null && StringUtils.isBlank(dbInvoice.getShortUrl())) {
				dbInvoice.setShortUrl(BillSMSUtil.shortenUrl(null, BillRuleEngine.preparePaymentUrl(dbInvoice.getId())));
			}
			tx.commit();
			if(invoicePaid && dbInvoice != null) {
				BillInvoice currInvoice = new BillInvoice();
				nullAwareBeanUtils.copyProperties(currInvoice, invoice);
				if(currInvoice.getId() == null) {
					currInvoice.setId(dbInvoice.getId());
				}
				currInvoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + currInvoice.getId());
				BillRuleEngine.sendEmails(currInvoice, dbInvoice, nullAwareBeanUtils, executor);
			}
			if(dbInvoice != null) {
				if(invoice.getId() == null) {
					dbInvoice = new BillInvoiceDaoImpl(session).getBusinessInvoice(dbInvoice.getId());
					session.refresh(dbInvoice); //To refresh state of invoice and read the invoice items
				}
				BillInvoice currrInvoice = BillDataConverter.getInvoice(nullAwareBeanUtils, dbInvoice);
				BillRuleEngine.calculatePayable(currrInvoice, dbInvoice, session);
				BillUser customerDetails = BillDataConverter.getCustomerDetails(nullAwareBeanUtils, dbInvoice.getSubscription());
				customerDetails.setCurrentBusiness(BillDataConverter.getBusinessBasic(business));
				currrInvoice.setPaymentMessage(BillSMSUtil.generateResultMessage(customerDetails, currrInvoice, BillConstants.MAIL_TYPE_INVOICE, null));
				response.setInvoice(currrInvoice);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	private void updateSubscribedItems(Session session, BillDBSubscription subscription, BillInvoice invoice) {
		if(CollectionUtils.isEmpty(invoice.getInvoiceItems())) {
			return;
		}
		for(BillItem item: invoice.getInvoiceItems()) {
			//Check if this customer already has the subscription
			if (item.getParentItem() != null && subscription.getId() != null) {
				BillDBItemSubscription dbSubscribedItem = null;
				if(item.getParentItemId() != null && invoice.getMonth() != null) {
					//for Recurring
					dbSubscribedItem = new BillGenericDaoImpl(session).getEntityByKey(BillDBItemSubscription.class, ID_ATTR, item.getParentItemId(), true);
				} else {
					//for generic
					dbSubscribedItem = new BillSubscriptionDAOImpl(session).getActiveItemSubscription(subscription.getId(), item.getParentItem().getId());
				}
				
				if (dbSubscribedItem == null) {
					dbSubscribedItem = new BillDBItemSubscription();
					dbSubscribedItem.setCreatedDate(new Date());
					dbSubscribedItem.setStatus(STATUS_ACTIVE);
					dbSubscribedItem.setQuantity(new BigDecimal(1));
					dbSubscribedItem.setSubscription(subscription);
					BillDBItemBusiness itemBusiness = new BillGenericDaoImpl(session).getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getParentItem().getId(), true);
					if(itemBusiness != null) {
						dbSubscribedItem.setBusinessItem(itemBusiness);
						session.persist(dbSubscribedItem);
					}
				}
				item.setId(dbSubscribedItem.getId());
			}
		}
	}

	private void updateInvoiceItems(Session session, BillInvoice invoice, BillDBUserBusiness business) {
		if(CollectionUtils.isNotEmpty(invoice.getInvoiceItems())) {
			for(BillItem item: invoice.getInvoiceItems()) {
				if(item.getId() == null) {
					//New invoice item
					if(item.getParentItem() == null && item.getPrice() != null && item.getQuantity() != null && StringUtils.isNotBlank(item.getName())) {
						//New item to be added to business items
						BillDBItemBusiness businessItem = new BillDBItemBusiness();
						businessItem.setName(item.getName());
						businessItem.setPrice(CommonUtils.formatDecimal(item.getPrice().divide(item.getQuantity(), 2, RoundingMode.HALF_UP)));
						businessItem.setBusiness(business);
						businessItem.setCreatedDate(new Date());
						businessItem.setStatus(STATUS_ACTIVE);
						session.persist(businessItem);
						BillItem parentItem = new BillItem();
						parentItem.setId(businessItem.getId());
						item.setParentItem(parentItem);
					}
				}
			}
		}
	}

	public BillServiceResponse getAllInvoices(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
			BillUserLog userLog = null;
			if(request.getItem() != null && request.getItem().getChangeLog() != null) {
				userLog = request.getItem().getChangeLog();
				userLog.setFromDate(CommonUtils.setZero(userLog.getFromDate()));
				userLog.setToDate(CommonUtils.setZero(userLog.getToDate()));
			}
			List<BillDBInvoice> invoices = dao.getAllBusinessInvoices(request.getBusiness().getId(), null, userLog);
			List<BillUser> userInvoices = BillDataConverter.getBusinessInvoices(invoices, session);
			response.setUsers(userInvoices);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getBusinessSummary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Date startDate = null, endDate = null;
			if(request.getItem() != null && request.getItem().getChangeLog() != null) {
				startDate = CommonUtils.setZero(request.getItem().getChangeLog().getFromDate());
				endDate = CommonUtils.setZero(request.getItem().getChangeLog().getToDate());
			}
			
			BillAdminDashboard dashboard = new BillAdminDashboard();
			Map<String, Object> restrictions = new HashMap<String, Object>();
			restrictions.put("business", request.getBusiness().getId());
			restrictions.put("status", BillConstants.INVOICE_STATUS_PAID);
			BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
			dashboard.setTotalPaid((BigDecimal) billGenericDaoImpl.getSum(BillDBTransactions.class, "amount", restrictions, startDate, endDate, "sum"));
			dashboard.setTotalInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count"));
			dashboard.setTotalCustomers((Long) billGenericDaoImpl.getSum(BillDBSubscription.class, "id", restrictions, startDate, endDate, "count"));
			restrictions.put("status", BillConstants.INVOICE_SETTLEMENT_STATUS_SETTLED);
			
			response.setDashboard(dashboard);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

}
