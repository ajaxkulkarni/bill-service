package com.rns.web.billapp.service.bo.impl;

import java.util.ArrayList;
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
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerProfile;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSchemesDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;
import com.sun.mail.iap.Response;

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
				List<BillDBCustomerCoupons> usedCoupons = dao.getEntitiesByKey(BillDBCustomerCoupons.class, "invoice.id", request.getInvoice().getId(), true, null, null);
				if (couponNotUsable(usedCoupons)) {
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

			BillDBCustomerCoupons coupons = BillBusinessConverter.getCustomerCoupon(schemes, subscription, invoice);
			session.persist(coupons);
			coupons.setCouponCode(CommonUtils.generateCouponCode(schemes, coupons));
			if (StringUtils.isBlank(schemes.getPaymentType())) {
				// Payment is not required for this scheme, so send mails to the
				// customer/vendor and business
				BillRuleEngine.sendCouponMails(schemes, subscription, coupons, MAIL_TYPE_COUPON_ACCEPTED, MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, MAIL_TYPE_COUPON_ACCEPTED_ADMIN, executor, null);
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
				List<BillDBCustomerCoupons> usedCoupons = dao.getEntitiesByKey(BillDBCustomerCoupons.class, "invoice.id", request.getInvoice().getId(), false, null, null);
				if (couponNotUsable(usedCoupons)) {
					for(BillDBCustomerCoupons existing: usedCoupons) {
						if(couponNotReUsable(existing)) {
							BillScheme scheme = BillDataConverter.getScheme(existing.getScheme(), existing, new NullAwareBeanUtils());
							scheme.setSchemeBusiness(BillDataConverter.getBusiness(existing.getScheme().getBusiness()));
							response.setScheme(scheme);
							response.setResponse(ERROR_CODE_GENERIC, ERROR_ACCEPTED_SCHEME_INVOICE);
							return response;
						}
					}
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
					LoggingUtil.logMessage("Loading schemes for .. " + request.getInvoice().getId());
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

	private boolean couponNotUsable(List<BillDBCustomerCoupons> existing) {
		if(CollectionUtils.isEmpty(existing)) {
			return false;
		}
		if(existing.size() > 1) {
			return true;
		}
		BillDBCustomerCoupons coupons = existing.get(0);
		return couponNotReUsable(coupons);
	}

	private boolean couponNotReUsable(BillDBCustomerCoupons coupons) {
		return coupons != null && coupons.getScheme() != null && !StringUtils.equals(SCHEME_TYPE_REWARD, coupons.getScheme().getSchemeType());
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
				BillRuleEngine.redeemCoupon(session, existing, executor);
			}
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse loadCustomerProfile(BillServiceRequest request) {
		BillServiceResponse billServiceResponse = new BillServiceResponse();
		if(request.getUser() == null || StringUtils.isBlank(request.getUser().getPhone())) {
			billServiceResponse.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return billServiceResponse;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBCustomerProfile> profiles = getCustomerByPhone(request, dao);
			if(CollectionUtils.isEmpty(profiles)) {
				billServiceResponse.setResponse(ERROR_NOT_FOUND, ERROR_CUSTOMER_PROFILE_NOT_FOUND);
				return billServiceResponse;
			} else {
				BillUser user = BillDataConverter.getCustomerProfile(profiles.get(0));
				billServiceResponse.setUser(user);
				return billServiceResponse;
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return billServiceResponse;
	}

	private List<BillDBSubscription> getSubscriptionByPhone(BillServiceRequest request, BillGenericDaoImpl dao) {
		List<BillDBSubscription> profiles = dao.getEntitiesByKey(BillDBSubscription.class, "phone", request.getUser().getPhone(), true, null, null);
		if(CollectionUtils.isEmpty(profiles)) {
			profiles = dao.getEntitiesByKey(BillDBSubscription.class, "phone", StringUtils.removeStart(request.getUser().getPhone(), "+91"), true, null, null);
		}
		return profiles;
	}
	
	private List<BillDBCustomerProfile> getCustomerByPhone(BillServiceRequest request, BillGenericDaoImpl dao) {
		List<BillDBCustomerProfile> profiles = dao.getEntitiesByKey(BillDBCustomerProfile.class, "phone", request.getUser().getPhone(), true, null, null);
		if(CollectionUtils.isEmpty(profiles)) {
			profiles = dao.getEntitiesByKey(BillDBCustomerProfile.class, "phone", StringUtils.removeStart(request.getUser().getPhone(), "+91"), true, null, null);
		}
		return profiles;
	}

	public BillServiceResponse loadCustomerBusinesses(BillServiceRequest request) {
		BillServiceResponse billServiceResponse = new BillServiceResponse();
		if(request.getUser() == null || StringUtils.isBlank(request.getUser().getPhone())) {
			billServiceResponse.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return billServiceResponse;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			//List<BillDBSubscription> profiles = dao.getEntitiesByKey(BillDBSubscription.class, "phone", request.getUser().getPhone(), true, null, null);
			List<BillDBSubscription> profiles = getSubscriptionByPhone(request, dao);
			if(CollectionUtils.isEmpty(profiles)) {
				billServiceResponse.setResponse(ERROR_NOT_FOUND, ERROR_CUSTOMER_PROFILE_NOT_FOUND);
				return billServiceResponse;
			} else {
				List<BillBusiness> businesses = new ArrayList<BillBusiness>();
				for(BillDBSubscription sub: profiles) {
					BillBusiness business = BillDataConverter.getBusiness(sub.getBusiness());
					businesses.add(business);
				}
				billServiceResponse.setBusinesses(businesses);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return billServiceResponse;
	}

	public BillServiceResponse loadCustomerDashboard(BillServiceRequest request) {
		Session session = null;
		BillServiceResponse billServiceResponse = new BillServiceResponse();
		String phone = request.getUser().getPhone();
		if(request.getUser() == null || StringUtils.isBlank(phone)) {
			billServiceResponse.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return billServiceResponse;
		}
		try {
			session = this.sessionFactory.openSession();
			BillUser customerDetails = new BillUser();
			// Additional info for profile
			BillSubscription currentSubscription = new BillSubscription();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			Long noOfBusinesses = dao.getCountByKey(BillDBSubscription.class, "phone", phone, true);
			if(noOfBusinesses == null || noOfBusinesses == 0) {
				phone = StringUtils.remove(phone, "+91");
				noOfBusinesses = dao.getCountByKey(BillDBSubscription.class, "phone", phone, true);
			}
			if(noOfBusinesses != null && noOfBusinesses > 0) {
				billServiceResponse.setUsers(new BillInvoiceDaoImpl(session).getCustomerPendingInvoices(phone));
				currentSubscription.setBusinessesConnected(noOfBusinesses);
				customerDetails.setCurrentSubscription(currentSubscription);
				billServiceResponse.setUser(customerDetails);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return billServiceResponse;
	}

	public BillServiceResponse registerCustomer(BillServiceRequest request) {
		BillServiceResponse billServiceResponse = new BillServiceResponse();
		if(request.getUser() == null || StringUtils.isBlank(request.getUser().getPhone())) {
			billServiceResponse.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return billServiceResponse;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			List<BillDBCustomerProfile> profiles = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBCustomerProfile.class, "phone", request.getUser().getPhone(), true, null, null);
			if(CollectionUtils.isEmpty(profiles)) {
				BillDBCustomerProfile profile = new BillDBCustomerProfile();
				profile.setEmail(request.getUser().getEmail());
				profile.setCreatedDate(new Date());
				profile.setLastLoggedIn(new Date());
				profile.setName(request.getUser().getName());
				profile.setPhone(request.getUser().getPhone());
				profile.setStatus(STATUS_ACTIVE);
				session.persist(profile);
			} else {
				BillDBCustomerProfile profile = profiles.get(0);
				profile.setEmail(request.getUser().getEmail());
				profile.setName(request.getUser().getName());
				profile.setPhone(request.getUser().getPhone());
			}
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return billServiceResponse;
	}

	public BillServiceResponse loadInvoice(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getUser() == null || StringUtils.isBlank(request.getUser().getPhone()) || request.getBusiness() == null || request.getBusiness().getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			//Get subscription ID from phone number first
			BillDBSubscription dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(request.getUser().getPhone(), request.getBusiness().getId());
			if(dbSubscription == null) {
				dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(StringUtils.removeStart(request.getUser().getPhone(), "+91"), request.getBusiness().getId());
			}
			if(dbSubscription != null) {
				BillInvoiceDaoImpl dao = new BillInvoiceDaoImpl(session);
				BillDBInvoice invoice = dao.getLatestUnPaidInvoice(dbSubscription.getId());
				BillUserBoImpl.prepareInvoiceForPayment(request, response, session, invoice, executor);
			} else {
				response.setResponse(ERROR_NOT_FOUND, ERROR_INVOICE_NOT_FOUND);
				return response;
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

}
