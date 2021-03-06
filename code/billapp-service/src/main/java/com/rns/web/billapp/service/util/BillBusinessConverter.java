package com.rns.web.billapp.service.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;

public class BillBusinessConverter {
	
	public static void updateBusinessDetails(BillUser user, BillGenericDaoImpl dao, BillDBUser existingUser) throws IllegalAccessException, InvocationTargetException, IOException {
		BillBusiness currentBusiness = user.getCurrentBusiness();
		if(currentBusiness != null) {
			BillDBUserBusiness billDBUserBusiness = new BillDBUserBusiness();
			if(currentBusiness.getId() != null) {
				billDBUserBusiness = dao.getEntityByKey(BillDBUserBusiness.class, "id", currentBusiness.getId(), true);
			}
			if(billDBUserBusiness == null) {
				return;
			}
			updateBillDbBusiness(user, existingUser, billDBUserBusiness);
			Session session = dao.getSession();
			if(billDBUserBusiness.getId() == null) {
				billDBUserBusiness.setStatus(BillConstants.STATUS_ACTIVE);
				billDBUserBusiness.setCreatedDate(new Date());
				if(billDBUserBusiness.getSector() != null) {
					session.persist(billDBUserBusiness);
					currentBusiness.setId(billDBUserBusiness.getId()); //For reference while adding distributor via admin
				}
			}
			if(currentBusiness.getLogo() != null) {
				String folderLoc = BillConstants.ROOT_FOLDER_LOCATION + billDBUserBusiness.getId();
				File folder = new File(folderLoc);
				if(!folder.exists()) {
					folder.mkdirs();
				}
				String logoLoc = folderLoc + "/logo_" + currentBusiness.getLogo().getFilePath();
				CommonUtils.writeToFile(currentBusiness.getLogo().getFileData(), logoLoc);
				billDBUserBusiness.setLogoImg(logoLoc);
			}
			//Update business locations
			updateBusinessLocations(currentBusiness, billDBUserBusiness, session);
		}
	}

	private static void updateBusinessLocations(BillBusiness currentBusiness, BillDBUserBusiness billDBUserBusiness, Session session)
			throws IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isNotEmpty(currentBusiness.getBusinessLocations())) {
			billDBUserBusiness.getLocations().removeAll(billDBUserBusiness.getLocations());
			Set<BillDBLocation> locations = new HashSet<BillDBLocation>();
			NullAwareBeanUtils notNullBean = new NullAwareBeanUtils();
			for(BillLocation loc: currentBusiness.getBusinessLocations()) {
				BillDBLocation dbLoc = new BillDBLocation();
				notNullBean.copyProperties(dbLoc, loc);
				locations.add(dbLoc);
			}
			billDBUserBusiness.setLocations(locations);
		}
	}

	private static void updateBillDbBusiness(BillUser user, BillDBUser dbUser, BillDBUserBusiness billDBUserBusiness) throws IllegalAccessException, InvocationTargetException {
		NullAwareBeanUtils notNullBean = new NullAwareBeanUtils();
		BillBusiness currentBusiness = user.getCurrentBusiness();
		notNullBean.copyProperties(billDBUserBusiness, currentBusiness);
		billDBUserBusiness.setUser(dbUser);
		if(billDBUserBusiness.getSector() == null && currentBusiness.getBusinessSector() != null && currentBusiness.getBusinessSector().getId() != null) {
			BillDBSector dbSector = new BillDBSector();
			dbSector.setId(currentBusiness.getBusinessSector().getId());
			billDBUserBusiness.setSector(dbSector);
		}
		
	}
	
	public static void updateBusinessItem(BillItem item, Session session, BillBusiness business) throws IllegalAccessException, InvocationTargetException, IOException {
		BeanUtilsBean notNullBean = new NullAwareBeanUtils();
		BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
		BillDBItemBusiness dbItem = dao.getEntityByKey(BillDBItemBusiness.class, BillConstants.ID_ATTR, item.getId(), true);
		if(dbItem == null) {
			dbItem = new BillDBItemBusiness();
		} 
		saveBusinessItem(item, session, business, notNullBean, dbItem);
		if(item.getImage() != null) {
			updateItemImage(item, dbItem);
		}
	}

	public static void saveBusinessItem(BillItem item, Session session, BillBusiness business, BeanUtilsBean notNullBean, BillDBItemBusiness dbItem)
			throws IllegalAccessException, InvocationTargetException {
		notNullBean.copyProperties(dbItem, item);
		//Set days if only frequency is set
		if(StringUtils.isNotBlank(dbItem.getFrequency()) ) {
			if(StringUtils.equals(dbItem.getFrequency(), BillConstants.FREQ_MONTHLY) && StringUtils.isBlank(dbItem.getMonthDays())) {
				dbItem.setMonthDays("1");
			}
		}
		if(item.getParentItem() != null) {
			BillDBItemParent parent = new BillDBItemParent();
			parent.setId(item.getParentItem().getId());
			dbItem.setParent(parent);
		}
		if(dbItem.getBusiness() == null) {
			BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
			dbBusiness.setId(business.getId());
			dbItem.setBusiness(dbBusiness);
		}
		if(dbItem.getId() == null) {
			dbItem.setStatus(BillConstants.STATUS_ACTIVE);
			dbItem.setCreatedDate(new Date());
			session.persist(dbItem);
		}
	}
	
	public static void setInvoiceItems(BillInvoice invoice, Session session, BillDBInvoice dbInvoice, boolean hasSubscribedItem) {
		if(CollectionUtils.isNotEmpty(invoice.getInvoiceItems())) {
			for(BillItem item: invoice.getInvoiceItems()) {
				BillDBItemInvoice invoiceItem = null;
				if(dbInvoice.getId() != null && hasSubscribedItem) {
					invoiceItem = new BillInvoiceDaoImpl(session).getInvoiceItem(dbInvoice.getId(), item.getId());
				} else {
					invoiceItem = new BillGenericDaoImpl(session).getEntityByKey(BillDBItemInvoice.class, BillConstants.ID_ATTR, item.getId(), true);
				}
				if(invoiceItem == null) {
					invoiceItem = new BillDBItemInvoice();
					invoiceItem.setCreatedDate(new Date());
					invoiceItem.setStatus(BillConstants.STATUS_ACTIVE);
					if(item.getId() != null) {
						invoiceItem.setSubscribedItem(new BillDBItemSubscription(item.getId()));
					}
					if(item.getParentItem() != null) {
						BillDBItemBusiness itemBusiness = new BillGenericDaoImpl(session).getEntityByKey(BillDBItemBusiness.class, BillConstants.ID_ATTR, item.getParentItem().getId(), false);
						invoiceItem.setBusinessItem(itemBusiness);
					}
					invoiceItem.setInvoice(dbInvoice);
				}
				invoiceItem.setPrice(item.getPrice());
				invoiceItem.setQuantity(item.getQuantity());
				if(/*!hasSubscribedItem &&*/ StringUtils.isNotBlank(item.getStatus())) {
					invoiceItem.setStatus(item.getStatus()); //Delete invoice item
				}
				if(invoiceItem.getId() == null) {
					session.persist(invoiceItem);
				}
			}
		}
	}
	
	public static void setPaymentCredentials(BillDBUser dbUser, BillPaymentCredentials instaResponse) {
		if(instaResponse != null) {
			if(StringUtils.isNotBlank(instaResponse.getInstaId())) {
				dbUser.setInstaId(instaResponse.getInstaId());
			}
			dbUser.setRefreshToken(instaResponse.getRefresh_token());
			dbUser.setAccessToken(instaResponse.getAccess_token());
		}
	}

	public static BillDBTransactions getTransaction(BillDBInvoice dbInvoice, BillInvoice invoice) {
		if(dbInvoice == null || dbInvoice.getId() == null) {
			return null;
		}
		BillDBTransactions transactions = new BillDBTransactions();
		transactions.setInvoice(dbInvoice);
		transactions.setAmount(dbInvoice.getPaidAmount());
		transactions.setCreatedDate(new Date());
		transactions.setStatus(dbInvoice.getStatus());
		transactions.setPaymentMedium(dbInvoice.getPaymentMedium());
		transactions.setPaymentMode(dbInvoice.getPaymentMode());
		transactions.setReferenceNo(dbInvoice.getPaymentRequestId());
		transactions.setPaymentId(dbInvoice.getPaymentId());
		transactions.setSubscription(dbInvoice.getSubscription());
		if(dbInvoice.getSubscription() != null) {
			transactions.setBusiness(dbInvoice.getSubscription().getBusiness());
			if(dbInvoice.getSubscription().getBusiness() != null) {
				transactions.setTransactionCharges(BillRuleEngine.calculateTransactionCharges(transactions.getAmount(), dbInvoice.getSubscription().getBusiness().getTransactionCharges()));
			}
		}
		if(invoice != null) {
			transactions.setResponse(invoice.getPaymentResponse());
			transactions.setTransactionDate(invoice.getTxTime());
			transactions.setComments(invoice.getComments());
		}
		return transactions;
	}
	
	public static void updatePaymentURL(BillInvoice invoice, BillDBInvoice dbInvoice, BillDBSubscription customerSubscription, Session session)
			throws IllegalAccessException, InvocationTargetException, JsonParseException, JsonMappingException, IOException {
		BillRuleEngine.calculatePayable(invoice, dbInvoice, session);
		LoggingUtil.logMessage("Updating payment URL - " + customerSubscription.getBusiness());
		BillDBUser vendor = customerSubscription.getBusiness().getUser();
		BillPaymentCredentials credentials = new BillPaymentCredentials();
		BillDataConverter.setCredentials(vendor, credentials);
		BillUser customer = new BillUser();
		new NullAwareBeanUtils().copyProperties(customer, customerSubscription);
		updatePaymentURL(invoice, dbInvoice, vendor, customer, credentials);
	}
	
	public static void updatePaymentURL(BillInvoice invoice, BillDBInvoice dbInvoice, BillDBUser vendor, BillUser customer, BillPaymentCredentials credentials)
			throws JsonParseException, JsonMappingException, IOException {
		credentials = BillPaymentUtil.createPaymentRequest(customer, credentials, invoice, true);
		if(credentials != null) {
			invoice.setPaymentUrl(credentials.getLongUrl());
			dbInvoice.setPaymentUrl(credentials.getLongUrl());
			dbInvoice.setPaymentRequestId(credentials.getPaymentRequestId());
			BillBusinessConverter.setPaymentCredentials(vendor, credentials);
		}
	}
	
	public static void updatePaymentTransactionLog(Session session, BillDBInvoice dbInvoice, BillInvoice invoice) {
		BillDBTransactions transaction = BillBusinessConverter.getTransaction(dbInvoice, invoice);
		if(transaction != null) {
			session.persist(transaction);
		}
	}


	public static BillDBCustomerCoupons getCustomerCoupon(BillDBSchemes schemes, BillDBSubscription subscription, BillDBInvoice invoice) {
		BillDBCustomerCoupons coupons = new BillDBCustomerCoupons();
		NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
		// nullAwareBeanUtils.copyProperties(coupons, request.getScheme());
		coupons.setAcceptedDate(new Date());
		coupons.setStatus(BillConstants.STATUS_ACTIVE);
		if (StringUtils.equalsIgnoreCase(BillConstants.SCHEME_TYPE_LINK, schemes.getSchemeType())) {
			coupons.setStatus(BillConstants.STATUS_PENDING);
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
			if (CommonUtils.noOfDays(date, new Date()) > BillConstants.NS_SCHEME_DAYS_LIMIT) {
				month++;
			} else {
				month = month + 2;
			}
			coupons.setValidFrom(CommonUtils.getMonthFirstDate(month, year));
			coupons.setValidTill(CommonUtils.addToDate(coupons.getValidFrom(), Calendar.MONTH, schemes.getDuration()));
		} else {
			coupons.setValidFrom(new Date());
			if(schemes.getDuration() != null) {
				coupons.setValidTill(CommonUtils.addToDate(coupons.getValidFrom(), Calendar.MONTH, schemes.getDuration()));
			} else {
				coupons.setValidTill(new Date());
			}
		}
		return coupons;
	}
	
	public static BillDBCustomerCoupons getBusinessCoupon(BillDBSchemes schemes, BillDBUserBusiness fromBusiness, BillDBUserBusiness toBusiness) {
		BillDBCustomerCoupons coupons = new BillDBCustomerCoupons();
		// nullAwareBeanUtils.copyProperties(coupons, request.getScheme());
		coupons.setAcceptedDate(new Date());
		coupons.setStatus(BillConstants.STATUS_ACTIVE);
		if (StringUtils.equalsIgnoreCase(BillConstants.SCHEME_TYPE_LINK, schemes.getSchemeType())) {
			coupons.setStatus(BillConstants.STATUS_PENDING);
		}
		coupons.setBusiness(fromBusiness);
		coupons.setAcceptedBy(toBusiness);
		coupons.setScheme(schemes);
		// Decide validity
		coupons.setValidFrom(new Date());
		if(schemes.getDuration() != null) {
			coupons.setValidTill(CommonUtils.addToDate(coupons.getValidFrom(), Calendar.MONTH, schemes.getDuration()));
		} else {
			coupons.setValidTill(new Date());
		}
		return coupons;
	}
	
	public static void updatePaymentStatusAsPaid(BillInvoice invoice, BillDBInvoice dbInvoice) {
		BillRuleEngine.calculatePayable(invoice, null, null);
		dbInvoice.setPaidAmount(invoice.getPayable());
		if(invoice.getPaidDate() == null) {
			dbInvoice.setPaidDate(new Date());
		}
		dbInvoice.setPaymentType(BillConstants.PAYMENT_OFFLINE);
		dbInvoice.setStatus(invoice.getStatus());
		dbInvoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_CASH);
		dbInvoice.setPaymentMode(BillConstants.PAYMENT_OFFLINE);
		dbInvoice.setSettlementStatus(BillConstants.INVOICE_STATUS_PAID);
		dbInvoice.setSettlementDate(new Date());
	}

	
	public static void updateItemImage(BillItem item, BillDBItemParent dbItem) throws IOException {
		if(item.getImage() == null) {
			return;
		}
		String folderPath = BillConstants.ROOT_FOLDER_LOCATION + "Items/" + dbItem.getId() + "/";
		String imgPath = saveItemImage(item, folderPath);
		dbItem.setImagePath(imgPath);

	}
	
	public static void updateItemImage(BillItem item, BillDBItemBusiness dbItem) throws IOException {
		if(dbItem.getBusiness() == null) {
			return;
		}
		String folderPath = BillConstants.ROOT_FOLDER_LOCATION + "Items/Business-" + dbItem.getBusiness().getId() + "/" + dbItem.getId() + "/";
		String imgPath = saveItemImage(item, folderPath);
		dbItem.setImagePath(imgPath);

	}

	private static String saveItemImage(BillItem item, String folderPath) throws IOException {
		if (item.getImage() != null) {
			File folderLocation = new File(folderPath);
			if (!folderLocation.exists()) {
				folderLocation.mkdirs();
			}
			String imgPath = null;
			imgPath = folderPath + item.getImage().getFilePath();
			CommonUtils.writeToFile(item.getImage().getFileData(), imgPath);
			return imgPath;
		}
		return null;
	}


}
