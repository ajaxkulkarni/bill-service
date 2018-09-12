package com.rns.web.billapp.service.bo.impl;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillCustomerBo;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillCustomerBoImpl implements BillCustomerBo, BillConstants {
	
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

	public BillServiceResponse getScheme(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public BillServiceResponse updateScheme(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request.getScheme() == null || request.getScheme().getId() == null || request.getUser() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			//Check if the scheme is already accepted by the same customer
			BillDBCustomerCoupons existing = dao.getEntityByKey(BillDBCustomerCoupons.class, "scheme.id", request.getScheme().getId(), false);
			if(existing != null) {
				if(StringUtils.equals(STATUS_PENDING, existing.getStatus())) {
					response.setResponse(ERROR_NOT_APPROVED, ERROR_INCOMPLETE_SCHEME);
					return response;
				} else {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME);
					return response;
				}
			}
			//Check if any scheme is already accepted against this invoice
			if(request.getInvoice() != null) {
				existing = dao.getEntityByKey(BillDBCustomerCoupons.class, "invoice.id", request.getInvoice().getId(), true);
				if(existing != null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME);
					return response;
				}
			}
			//Get DB entities
			BillDBSchemes schemes = dao.getEntityByKey(BillDBSchemes.class, ID_ATTR, request.getScheme().getId(), true);
			if(schemes == null) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_FOUND);
				return response;
			}
			//Scheme should still be available at this time
			if(schemes.getValidFrom() != null && schemes.getValidFrom().compareTo(new Date()) > 0) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_STARTED);
				return response;
			}
			if(schemes.getValidTill() != null && schemes.getValidTill().compareTo(new Date()) < 0) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_EXPIRED);
				return response;
			}
			
			BillDBSubscription subscription = null; //customer
			if(request.getUser().getId() != null) {
				subscription = dao.getEntityByKey(BillDBSubscription.class, ID_ATTR, request.getUser().getId(), true);
				if(subscription == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_CUSTOMER_PROFILE_NOT_FOUND);
					return response;
				}
			} 
			//Invoice needs to be paid to accept scheme, especially if it's a post invoice scheme
			BillDBInvoice invoice = null;
			if(request.getInvoice() != null) {
				invoice = dao.getEntityByKey(BillDBInvoice.class, ID_ATTR, request.getInvoice().getId(), false);
				if(invoice == null || !StringUtils.equalsIgnoreCase(INVOICE_STATUS_PAID, invoice.getStatus())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_UNPAID_INVOICE);
					return response;
				}
			} else if (StringUtils.equalsIgnoreCase(SCHEME_TYPE_INVOICE, schemes.getSchemeType())) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_UNPAID_INVOICE);
				return response;
			}
			
			BillDBCustomerCoupons coupons = new BillDBCustomerCoupons();
			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			//nullAwareBeanUtils.copyProperties(coupons, request.getScheme());
			
			coupons.setAcceptedDate(new Date());
			coupons.setStatus(STATUS_ACTIVE);
			if(!StringUtils.equals("NA", coupons.getPaymentType())) {
				coupons.setStatus(STATUS_PENDING);
			}
			coupons.setSubscription(subscription);
			coupons.setScheme(schemes);
			coupons.setInvoice(invoice);
			if(subscription != null) {
				coupons.setBusiness(subscription.getBusiness());
			}
			//Decide validity
			if(coupons.getBusiness() != null && coupons.getBusiness().getSector() != null && StringUtils.equalsIgnoreCase("Newspaper", coupons.getBusiness().getSector().getName())) {
				//If newspaper scheme
				int month = CommonUtils.getCalendarValue(new Date(), Calendar.MONTH);
				Integer year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
				Date date = CommonUtils.getMonthFirstDate(month + 1, year);
				if(CommonUtils.noOfDays(date, new Date()) > NS_SCHEME_DAYS_LIMIT) {
					month++;
				} else {
					month = month + 2;
				}
				coupons.setValidFrom(CommonUtils.getMonthFirstDate(month, year));
				coupons.setValidTill(CommonUtils.addToDate(coupons.getValidFrom(), Calendar.MONTH, schemes.getDuration()));
			} else {
				coupons.setValidFrom(new Date());
				coupons.setValidTill(CommonUtils.addToDate(coupons.getValidFrom(), Calendar.MONTH, schemes.getDuration()));
			}
			session.persist(coupons);
			coupons.setCouponCode(CommonUtils.generateCouponCode(schemes, coupons));
			if(StringUtils.isBlank(schemes.getPaymentType())) {
				//Payment is not required for this scheme, so send mails to the customer/vendor and business
				if(subscription != null && subscription.getBusiness() != null && subscription.getBusiness().getUser() != null) {
					BillUser customer = BillDataConverter.getCustomerDetails(nullAwareBeanUtils, subscription);
					BillBusiness currentBusiness = BillDataConverter.getBusiness(subscription.getBusiness());
					BillBusiness schemeBusiness = BillDataConverter.getBusiness(schemes.getBusiness());
					BillUser vendor = new BillUser();
					nullAwareBeanUtils.copyProperties(vendor, subscription.getBusiness().getUser());
					BillScheme pickedScheme = new BillScheme();
					nullAwareBeanUtils.copyProperties(pickedScheme, schemes);
					pickedScheme.setCouponCode(coupons.getCouponCode());
					
				}
			}
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return null;
	}

	public BillServiceResponse payScheme(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public BillServiceResponse getAllSchemes(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}