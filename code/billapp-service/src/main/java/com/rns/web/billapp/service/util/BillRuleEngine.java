package com.rns.web.billapp.service.util;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;

public class BillRuleEngine {

	
	public static boolean isDelivery(List<BillUserLog> logs, BillDBItemSubscription subscription) {
		if(CollectionUtils.isEmpty(logs)) {
			return true;
		}
		if(subscription == null) {
			return false;
		}
		//int noOrder = 0;	
		for(BillUserLog log: logs) {
			if(subscription.getBusinessItem() != null && subscription.getBusinessItem().getParent() != null && log.getParentItemId() == subscription.getBusinessItem().getParent().getId()) {
				//Parent Item holiday
				return isOrder(log);
			} else if (log.getSubscriptionId() == null && subscription.getBusinessItem() != null && subscription.getBusinessItem().getId() == log.getBusinessItemId()) {
				//Business Item holiday
				return isOrder(log);
			} else if (log.getSubscriptionId() != null && subscription.getSubscription() != null && log.getSubscriptionId().intValue() == subscription.getSubscription().getId().intValue()
					&& subscription.getBusinessItem() != null && subscription.getBusinessItem().getId() == log.getBusinessItemId()) {
				//Customer holiday
				return isOrder(log);
			}
		}
		/*if(noOrder == currentSubscription.getItems().size()) {
			return false;
		}*/
		return true;
	}


	private static boolean isOrder(BillUserLog log) {
		if(log.getQuantityChange() != null && BigDecimal.ZERO.equals(log.getQuantityChange())) {
			return false;
		}
		return true;
	}
	

	private static void setQuantity(BillUserLog log, BillItem item) {
		if(item.getQuantity() == null || !item.getQuantity().equals(BigDecimal.ZERO)) {
			item.setQuantity(log.getQuantityChange());
			/*if(item.getQuantity() != null && item.getQuantity().equals(BigDecimal.ZERO)) {
				noOrder++;
			}*/
		}
		//return noOrder;
	}


	public static void calculatePayable(BillInvoice invoice, BillDBInvoice dbInvoice, Session session) {
		if(invoice.getAmount() == null) {
			return;
		}
		MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
		invoice.setPayable(invoice.getAmount());
		if(invoice.getPendingBalance() != null) {
			invoice.setPayable(invoice.getPayable().add(invoice.getPendingBalance()));
		}
		if(invoice.getCreditBalance() != null) {
			invoice.setPayable(invoice.getPayable().subtract(invoice.getCreditBalance()));
		}
		if(invoice.getServiceCharge() != null) {
			invoice.setPayable(invoice.getPayable().add(invoice.getServiceCharge()));
		}
		BigDecimal internetHandlingFees = invoice.getPayable().multiply(new BigDecimal(BillConstants.PAYMENT_CHARGE_PERCENT), mc).add(new BigDecimal(BillConstants.PAYMENT_CHARGE_FIXED), mc);
		//Add GST 18%
		internetHandlingFees = internetHandlingFees.add(internetHandlingFees.multiply(new BigDecimal(0.18), mc), mc);
		internetHandlingFees = BigDecimal.ZERO; //TODO: Change later
		invoice.setInternetFees(internetHandlingFees);
		invoice.setPayable(invoice.getPayable().add(invoice.getInternetFees()));
		
		//TODO Later
		if(dbInvoice != null && dbInvoice.getSubscription() != null && dbInvoice.getMonth() != null && dbInvoice.getYear() != null) {
			BigDecimal outstanding = BigDecimal.ZERO;
			//Outstanding amount is the total amount of the bills excluding this bill month / year
			List<Object[]> result = new BillInvoiceDaoImpl(session).getCustomerOutstanding(dbInvoice.getMonth(), dbInvoice.getYear(), dbInvoice.getSubscription().getId());
			if(CollectionUtils.isNotEmpty(result)) {
				for(Object[] row: result) {
					outstanding = outstanding.add(CommonUtils.getAmount(row[0]));
					outstanding = outstanding.add(CommonUtils.getAmount(row[1]));
					outstanding = outstanding.add(CommonUtils.getAmount(row[2]));
				 	outstanding = outstanding.subtract(CommonUtils.getAmount(row[3]));
				}
				invoice.setOutstandingBalance(outstanding);
				invoice.setPayable(invoice.getPayable().add(outstanding));
			}
		}
	}

	public static boolean showBillDetails(BillUser user) {
		if(user == null) {
			return true;
		}
		if(StringUtils.equals(BillConstants.NO, user.getShowBillDetails())) {
			return false;
		} else if (user.getShowBillDetails() == null && user.getCurrentBusiness() != null && StringUtils.equals(BillConstants.NO, user.getCurrentBusiness().getShowBillDetails())) {
			return false;
		}
		return true;
	}
	
	
	public static void redeemCoupon(Session session, BillDBCustomerCoupons existing, ThreadPoolTaskExecutor executor) throws IllegalAccessException, InvocationTargetException {
		existing.setStatus("R");
		existing.setRedeemDate(new Date());
		// If vendor commission, then add the commission as a
		// transaction
		addCouponTransaction(session, existing, null, null);
		if (existing.getScheme() != null && existing.getScheme().getBusiness() != null && existing.getSubscription() != null) {
			sendCouponMails(existing.getScheme(), existing.getSubscription(), existing, BillConstants.MAIL_TYPE_COUPON_REDEEMED, BillConstants.MAIL_TYPE_COUPON_REDEEMED_BUSINESS, BillConstants.MAIL_TYPE_COUPON_REDEEMED_ADMIN, executor, null);
		}
	}


	public static void addCouponTransaction(Session session, BillDBCustomerCoupons existing, BillDBUserBusiness businessToPay, BigDecimal commission) {
		if (existing.getScheme() != null && existing.getScheme().getVendorCommission() != null) {
			BillDBTransactions transactions = new BillDBTransactions();
			if(commission != null) {
				transactions.setAmount(commission);
			} else {
				transactions.setAmount(existing.getScheme().getVendorCommission());
			}
			transactions.setCreatedDate(new Date());
			transactions.setStatus(BillConstants.INVOICE_STATUS_PAID);
			transactions.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_CASHFREE);
			transactions.setPaymentMode(BillConstants.PAYMENT_MODE_REWARD);
			transactions.setReferenceNo(existing.getCouponCode());
			transactions.setSubscription(existing.getSubscription());
			if(businessToPay != null) {
				transactions.setBusiness(businessToPay);
			} else if (existing.getSubscription() != null) {
				transactions.setBusiness(existing.getSubscription().getBusiness());
			}
			transactions.setTransactionDate(CommonUtils.convertDate(new Date()));
			transactions.setComments(existing.getScheme().getSchemeName());
			session.persist(transactions);
		}
	}

	
	public static void sendCouponMails(BillDBSchemes schemes, BillDBSubscription subscription, BillDBCustomerCoupons coupons, String mailTypeCustomer, String mailTypeCouponBusiness, String mailTypeCouponAdmin, ThreadPoolTaskExecutor executor, BillDBUser acceptedUser)
			throws IllegalAccessException, InvocationTargetException {
		BillUser customer = null;
		BillUser vendor = null;
		BillUser schemeBusinessVendor = null;
		BillBusiness schemeBusiness = null;
		BillScheme pickedScheme = null;
		NullAwareBeanUtils beanUtils = new NullAwareBeanUtils();
		if (subscription != null && subscription.getBusiness() != null && subscription.getBusiness().getUser() != null) {
			customer = BillDataConverter.getCustomerDetails(beanUtils, subscription);
			vendor = new BillUser();
			beanUtils.copyProperties(vendor, subscription.getBusiness().getUser());
		} else if (acceptedUser != null) {
			//In case of offer picked up by vendor
			customer = new BillUser();
			beanUtils.copyProperties(customer, acceptedUser);
		}
		if (schemes != null && schemes.getBusiness() != null && schemes.getBusiness().getUser() != null) {
			schemeBusinessVendor = new BillUser();
			beanUtils.copyProperties(schemeBusinessVendor, schemes.getBusiness().getUser());
			pickedScheme = BillDataConverter.getScheme(schemes, coupons, beanUtils);
			schemeBusiness = BillDataConverter.getBusiness(schemes.getBusiness());
		}

		if(pickedScheme != null && schemeBusiness != null) {
			if(customer != null && StringUtils.isNotBlank(mailTypeCustomer)) {
				// Send offer details to customer
				customer.setCurrentBusiness(schemeBusiness);
				// String mailTypeCustomer = MAIL_TYPE_COUPON_ACCEPTED;
				if (!StringUtils.equals(BillConstants.SCHEME_TYPE_REWARD, pickedScheme.getSchemeType())) {
					// Send email to customer
					BillMailUtil customerMail = new BillMailUtil(mailTypeCustomer, customer);
					customerMail.setSelectedScheme(pickedScheme);
					executor.execute(customerMail);
					// Send SMS to customer
					BillSMSUtil.sendSMS(customer, null, mailTypeCustomer, pickedScheme);
				}
			}
			if(schemeBusinessVendor != null && StringUtils.isNotBlank(mailTypeCouponBusiness)) {
				// Notify scheme business
				schemeBusinessVendor.setCurrentBusiness(schemeBusiness);
				// String mailTypeCouponBusiness = MAIL_TYPE_COUPON_ACCEPTED_BUSINESS;
				BillMailUtil schemeBusinessMail = new BillMailUtil(mailTypeCouponBusiness, schemeBusinessVendor);
				schemeBusinessMail.setCustomerInfo(customer);
				schemeBusinessMail.setSelectedScheme(pickedScheme);
				schemeBusinessMail.setCopyAdmins(true);
				executor.execute(schemeBusinessMail);
				BillSMSUtil smsUtil = new BillSMSUtil();
				smsUtil.setCustomer(customer);
				smsUtil.sendSms(schemeBusinessVendor, null, mailTypeCouponBusiness, pickedScheme);
			}
			if(vendor != null && StringUtils.isNotBlank(mailTypeCouponAdmin)) {
				// Notify vendor
				vendor.setName(customer.getName());
				vendor.setCurrentBusiness(schemeBusiness);
				// String mailTypeCouponAdmin = MAIL_TYPE_COUPON_ACCEPTED_ADMIN;
				if (schemes.getVendorCommission() != null) {
					BillMailUtil vendorMail = new BillMailUtil(mailTypeCouponAdmin, vendor);
					vendorMail.setSelectedScheme(pickedScheme);
					vendorMail.setCopyAdmins(true);
					executor.execute(vendorMail);
					BillSMSUtil vendorSms = new BillSMSUtil();
					vendorSms.setCustomer(customer);
					vendorSms.sendSms(vendor, null, mailTypeCouponAdmin, pickedScheme);
				}
			}
		}
		
	}
	
	public static void sendEmails(BillInvoice currentInvoice, BillDBInvoice invoice, NullAwareBeanUtils nullAwareBeanUtils, ThreadPoolTaskExecutor executor)
			throws IllegalAccessException, InvocationTargetException {
		BillUser customer = new BillUser();
		nullAwareBeanUtils.copyProperties(customer, invoice.getSubscription());
		BillUser vendor = new BillUser();
		nullAwareBeanUtils.copyProperties(vendor, invoice.getSubscription().getBusiness().getUser());
		vendor.setName(customer.getName());
		BillBusiness business = new BillBusiness();
		nullAwareBeanUtils.copyProperties(business, invoice.getSubscription().getBusiness());
		customer.setCurrentBusiness(business);
		BillMailUtil customerMail = new BillMailUtil(BillConstants.MAIL_TYPE_PAYMENT_RESULT);
		customerMail.setUser(customer);
		currentInvoice.setPayable(invoice.getPaidAmount());
		customerMail.setInvoice(currentInvoice);
		BillMailUtil vendorMail = new BillMailUtil(BillConstants.MAIL_TYPE_PAYMENT_RESULT_VENDOR);
		vendorMail.setUser(vendor);
		vendorMail.setInvoice(currentInvoice);
		if (StringUtils.isNotBlank(customer.getEmail())) {
			executor.execute(customerMail);
		}
		if (StringUtils.isNotBlank(vendor.getEmail())) {
			executor.execute(vendorMail);
		}
		BillSMSUtil.sendSMS(customer, currentInvoice, BillConstants.MAIL_TYPE_PAYMENT_RESULT, null);
		BillSMSUtil.sendSMS(vendor, currentInvoice, BillConstants.MAIL_TYPE_PAYMENT_RESULT_VENDOR, null);
	}

	public static BigDecimal calculateTransactionCharges(BigDecimal amount, BigDecimal txCharges) {
		if(amount == null || txCharges == null) {
			return null;
		}
		MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
		BigDecimal deduction = amount.multiply(txCharges.divide(new BigDecimal(100), mc), mc);
		//Add 18% GST
		deduction = deduction.add(deduction.multiply(new BigDecimal(0.18), mc));
		return deduction;
	}
	
	public static BigDecimal calculateSettlementAmount(BigDecimal amount, BigDecimal txCharges) {
		if(txCharges == null || amount == null) {
			return amount;
		}
		return amount.subtract(txCharges);
	}


	public static String preparePaymentUrl(Integer id) {
		return BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_LINK) + id;
	}


	public static BigDecimal getBillTotal(BillInvoice invoice) {
		if(invoice == null || invoice.getPayable() == null) {
			return null;
		}
		if (invoice.getOutstandingBalance() != null) {
			return invoice.getPayable().subtract(invoice.getOutstandingBalance());
		}
		return invoice.getPayable();
	}

	
}
