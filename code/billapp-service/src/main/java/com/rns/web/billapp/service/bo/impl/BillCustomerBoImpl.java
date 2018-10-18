package com.rns.web.billapp.service.bo.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillCustomerBo;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSchemesDaoImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillSMSUtil;
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
		if (request.getScheme() == null || request.getScheme().getId() == null || request.getUser() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			// Check if the scheme is already accepted by the same customer
			BillDBCustomerCoupons existing = new BillSchemesDaoImpl(session).getAcceptedScheme(request.getScheme().getId(), request.getUser().getId());
			if (existing != null) {
				if (StringUtils.equals(STATUS_PENDING, existing.getStatus())) {
					response.setResponse(ERROR_NOT_APPROVED, ERROR_INCOMPLETE_SCHEME);
					return response;
				} else {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME);
					return response;
				}
			}
			// Check if any scheme is already accepted against this invoice
			if (request.getInvoice() != null) {
				existing = dao.getEntityByKey(BillDBCustomerCoupons.class, "invoice.id", request.getInvoice().getId(), true);
				if (existing != null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME);
					return response;
				}
			}
			// Get DB entities
			BillDBSchemes schemes = dao.getEntityByKey(BillDBSchemes.class, ID_ATTR, request.getScheme().getId(), true);
			if (schemes == null) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_FOUND);
				return response;
			}
			// Scheme should still be available at this time
			if (schemes.getValidFrom() != null && schemes.getValidFrom().compareTo(new Date()) > 0) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_STARTED);
				return response;
			}
			if (schemes.getValidTill() != null && schemes.getValidTill().compareTo(new Date()) < 0) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_EXPIRED);
				return response;
			}

			BillDBSubscription subscription = null; // customer
			if (request.getUser().getId() != null) {
				subscription = dao.getEntityByKey(BillDBSubscription.class, ID_ATTR, request.getUser().getId(), true);
				if (subscription == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_CUSTOMER_PROFILE_NOT_FOUND);
					return response;
				}
			}
			// Invoice needs to be paid to accept scheme, especially if it's a
			// post invoice scheme
			BillDBInvoice invoice = null;
			if (request.getInvoice() != null) {
				invoice = dao.getEntityByKey(BillDBInvoice.class, ID_ATTR, request.getInvoice().getId(), false);
				if (invoice == null || !StringUtils.equalsIgnoreCase(INVOICE_STATUS_PAID, invoice.getStatus())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_UNPAID_INVOICE);
					return response;
				}
			} else if (StringUtils.equalsIgnoreCase(SCHEME_TYPE_INVOICE, schemes.getSchemeType())) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_UNPAID_INVOICE);
				return response;
			}

			BillDBCustomerCoupons coupons = new BillDBCustomerCoupons();
			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			// nullAwareBeanUtils.copyProperties(coupons, request.getScheme());

			coupons.setAcceptedDate(new Date());
			coupons.setStatus(STATUS_ACTIVE);
			if (StringUtils.equalsIgnoreCase(SCHEME_TYPE_LINK, schemes.getSchemeType())) {
				coupons.setStatus(STATUS_PENDING);
			}
			coupons.setSubscription(subscription);
			coupons.setScheme(schemes);
			coupons.setInvoice(invoice);
			if (subscription != null) {
				coupons.setBusiness(subscription.getBusiness());
			}
			// Decide validity
			if (coupons.getScheme() != null && coupons.getScheme().getBusiness() != null && coupons.getScheme().getBusiness().getSector() != null
					&& StringUtils.equalsIgnoreCase("Newspaper", coupons.getScheme().getBusiness().getSector().getName())) {
				// If newspaper scheme
				int month = CommonUtils.getCalendarValue(new Date(), Calendar.MONTH);
				Integer year = CommonUtils.getCalendarValue(new Date(), Calendar.YEAR);
				Date date = CommonUtils.getMonthFirstDate(month + 1, year);
				if (CommonUtils.noOfDays(date, new Date()) > NS_SCHEME_DAYS_LIMIT) {
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
			if (StringUtils.isBlank(schemes.getPaymentType())) {
				// Payment is not required for this scheme, so send mails to the
				// customer/vendor and business
				sendCouponMails(schemes, subscription, coupons, MAIL_TYPE_COUPON_ACCEPTED, MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, MAIL_TYPE_COUPON_ACCEPTED_ADMIN);
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

	private void sendCouponMails(BillDBSchemes schemes, BillDBSubscription subscription, BillDBCustomerCoupons coupons, String mailTypeCustomer, String mailTypeCouponBusiness, String mailTypeCouponAdmin)
			throws IllegalAccessException, InvocationTargetException {
		if (subscription != null && subscription.getBusiness() != null && subscription.getBusiness().getUser() != null) {
			NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
			BillUser customer = BillDataConverter.getCustomerDetails(beanUtils, subscription);
			BillBusiness schemeBusiness = BillDataConverter.getBusiness(schemes.getBusiness());
			BillUser vendor = new BillUser();
			beanUtils.copyProperties(vendor, subscription.getBusiness().getUser());
			BillUser schemeBusinessVendor = new BillUser();
			beanUtils.copyProperties(schemeBusinessVendor, schemes.getBusiness().getUser());
			BillScheme pickedScheme = BillDataConverter.getScheme(schemes, coupons, beanUtils);
			// Send offer details to customer
			customer.setCurrentBusiness(schemeBusiness);
			//String mailTypeCustomer = MAIL_TYPE_COUPON_ACCEPTED;
			BillMailUtil customerMail = new BillMailUtil(mailTypeCustomer, customer);
			customerMail.setSelectedScheme(pickedScheme);
			executor.execute(customerMail);
			// Send SMS to customer
			BillSMSUtil.sendSMS(customer, null, mailTypeCustomer, pickedScheme);
			// Notify scheme business
			schemeBusinessVendor.setCurrentBusiness(schemeBusiness);
			//String mailTypeCouponBusiness = MAIL_TYPE_COUPON_ACCEPTED_BUSINESS;
			BillMailUtil schemeBusinessMail = new BillMailUtil(mailTypeCouponBusiness, schemeBusinessVendor);
			schemeBusinessMail.setCustomerInfo(customer);
			schemeBusinessMail.setSelectedScheme(pickedScheme);
			schemeBusinessMail.setCopyAdmins(true);
			executor.execute(schemeBusinessMail);
			BillSMSUtil smsUtil = new BillSMSUtil();
			smsUtil.setCustomer(customer);
			smsUtil.sendSms(schemeBusinessVendor, null, mailTypeCouponBusiness, pickedScheme);
			// Notify vendor
			vendor.setName(customer.getName());
			vendor.setCurrentBusiness(schemeBusiness);
			//String mailTypeCouponAdmin = MAIL_TYPE_COUPON_ACCEPTED_ADMIN;
			if(schemes.getVendorCommission() != null) {
				BillMailUtil vendorMail = new BillMailUtil(mailTypeCouponAdmin, vendor);
				vendorMail.setSelectedScheme(pickedScheme);
				vendorMail.setCopyAdmins(true);
				executor.execute(vendorMail);
			}
		}
	}

	public BillServiceResponse payScheme(BillServiceRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	public BillServiceResponse getAllSchemes(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			if (request.getInvoice() != null && request.getInvoice().getId() != null) {
				BillDBInvoice dbInvoice = dao.getEntityByKey(BillDBInvoice.class, ID_ATTR, request.getInvoice().getId(), false);
				BillInvoice currentInvoice = BillDataConverter.getInvoice(new NullAwareBeanUtils(), dbInvoice);
				response.setInvoice(currentInvoice);
				BillUser vendor = new BillUser();
				new NullAwareBeanUtils().copyProperties(vendor, dbInvoice.getSubscription());
				vendor.setCurrentBusiness(BillDataConverter.getBusiness(dbInvoice.getSubscription().getBusiness()));
				response.setUser(vendor);
				if (dbInvoice == null || !StringUtils.equalsIgnoreCase(INVOICE_STATUS_PAID, dbInvoice.getStatus())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_UNPAID_INVOICE);
					return response;
				}
				if (StringUtils.equalsIgnoreCase(PAYMENT_OFFLINE, dbInvoice.getPaymentType()) || StringUtils.equalsIgnoreCase(PAYMENT_MEDIUM_CASH, dbInvoice.getPaymentMedium())) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_CASH_INVOICE);
					return response;
				}
				// Request for invoice schemes
				BillDBCustomerCoupons existing = dao.getEntityByKey(BillDBCustomerCoupons.class, "invoice.id", request.getInvoice().getId(), true);
				if (existing != null) {
					BillScheme scheme = BillDataConverter.getScheme(existing.getScheme(), existing, new NullAwareBeanUtils());
					scheme.setSchemeBusiness(BillDataConverter.getBusiness(existing.getScheme().getBusiness()));
					response.setScheme(scheme);
					response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME_INVOICE);
					return response;
				}
				List<BillDBSchemes> offers = new BillSchemesDaoImpl(session).getSchemes(SCHEME_TYPE_INVOICE);
				if (CollectionUtils.isNotEmpty(offers)) {
					List<BillScheme> schemes = new ArrayList<BillScheme>();
					NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
					for (BillDBSchemes offer : offers) {
						BillScheme scheme = new BillScheme();
						nullAwareBeanUtils.copyProperties(scheme, offer);
						scheme.setSchemeBusiness(BillDataConverter.getBusinessBasic(offer.getBusiness()));
						schemes.add(scheme);
					}
					response.setSchemes(schemes);
				}
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse redeemScheme(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || StringUtils.isBlank(request.getUser().getPhone()) || request.getScheme() == null
				|| StringUtils.isBlank(request.getScheme().getCouponCode())) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS_COUPON_VERIFICATION);
			return response;
		}

		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillDBCustomerCoupons existing = new BillGenericDaoImpl(session).getEntityByKey(BillDBCustomerCoupons.class, "couponCode",
					request.getScheme().getCouponCode(), false);
			if (existing == null) {
				response.setResponse(ERROR_CODE_GENERIC, "Coupon code does not exist!");
				return response;
			}
			if (existing.getSubscription() == null || !CommonUtils.comparePhoneNumbers(existing.getSubscription().getPhone(), request.getUser().getPhone())) {
				response.setResponse(ERROR_CODE_GENERIC, "Customer phone number does not match!");
				return response;
			}
			NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
			if (StringUtils.equalsIgnoreCase("Verify", request.getRequestType())) {
				// Verify coupon
				BillScheme scheme = BillDataConverter.getScheme(existing.getScheme(), existing, nullAwareBeanUtils);
				scheme.setSchemeBusiness(BillDataConverter.getBusinessBasic(existing.getScheme().getBusiness()));
				if (new Date().compareTo(scheme.getValidTill()) > 0) {
					scheme.setStatus("Expired");
				}
				response.setScheme(scheme);
				response.setUser(BillDataConverter.getCustomerDetails(nullAwareBeanUtils, existing.getSubscription()));
			} else if (StringUtils.equalsIgnoreCase("Redeem", request.getRequestType())) {
				// Redeem coupon
				existing.setStatus("R");
				existing.setRedeemDate(new Date());
				// If vendor commission, then add the commission as a
				// transaction
				if (existing.getScheme() != null && existing.getScheme().getVendorCommission() != null) {
					BillDBTransactions transactions = new BillDBTransactions();
					transactions.setAmount(existing.getScheme().getVendorCommission());
					transactions.setCreatedDate(new Date());
					transactions.setStatus(INVOICE_STATUS_PAID);
					transactions.setMedium(PAYMENT_MEDIUM_CASHFREE);
					transactions.setMode(PAYMENT_MODE_REWARD);
					transactions.setReferenceNo(existing.getCouponCode());
					transactions.setSubscription(existing.getSubscription());
					if (existing.getSubscription() != null) {
						transactions.setBusiness(existing.getSubscription().getBusiness());
					}
					transactions.setTransactionDate(CommonUtils.convertDate(new Date()));
					transactions.setComments(existing.getScheme().getSchemeName());
					session.persist(transactions);
				}
				if (existing.getScheme() != null && existing.getScheme().getBusiness() != null && existing.getSubscription() != null) {
					sendCouponMails(existing.getScheme(), existing.getSubscription(), existing, MAIL_TYPE_COUPON_REDEEMED, MAIL_TYPE_COUPON_REDEEMED_BUSINESS, MAIL_TYPE_COUPON_REDEEMED_ADMIN);
				}
			}
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

}
