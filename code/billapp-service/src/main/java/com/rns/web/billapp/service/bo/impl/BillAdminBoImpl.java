package com.rns.web.billapp.service.bo.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillAdminBo;
import com.rns.web.billapp.service.bo.domain.BillAdminDashboard;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillBusinessSummary;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillNotification;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBOrders;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBTransactions;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBUserFinancialDetails;
import com.rns.web.billapp.service.dao.impl.BillAdminDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillOrderDaoImpl;
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
import com.rns.web.billapp.service.util.BillExcelUtil;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillMessageBroadcaster;
import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.rns.web.billapp.service.util.BillRuleEngine;
import com.rns.web.billapp.service.util.BillSMSUtil;
import com.rns.web.billapp.service.util.BillUserLogUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.rns.web.billapp.service.util.NullAwareBeanUtils;

public class BillAdminBoImpl implements BillAdminBo, BillConstants {

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

	public BillServiceResponse updateItem(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillItem item = request.getItem();
		if (item == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBItemParent dbItem = dao.getEntityByKey(BillDBItemParent.class, ID_ATTR, item.getId(), true);
			if (dbItem == null) {
				dbItem = new BillDBItemParent();
				dbItem.setCreatedDate(new Date());
				dbItem.setStatus(STATUS_ACTIVE);
			}
			NullAwareBeanUtils nullBean = new NullAwareBeanUtils();
			nullBean.copyProperties(dbItem, item);
			if (dbItem.getId() == null) {
				session.persist(dbItem);
			}
			if (dbItem.getSector() == null) {
				BillDBSector sector = new BillDBSector();
				sector.setId(item.getItemSector().getId());
				dbItem.setSector(sector);
			}
			BillBusinessConverter.updateItemImage(item, dbItem);
			BillUserLogUtil.updateBillItemParentLog(dbItem, item, session);
			tx.commit();
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}


	public InputStream getImage(BillItem item, String type) {
		if (item == null) {
			return null;
		}
		Session session = null;
		InputStream is = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			if(StringUtils.equals("BusinessItem", type)) {
				BillDBItemBusiness dbItem = dao.getEntityByKey(BillDBItemBusiness.class, ID_ATTR, item.getId(), true);
				if (dbItem != null && StringUtils.isNotBlank(dbItem.getImagePath())) {
					is = new FileInputStream(dbItem.getImagePath());
					BillFile image = new BillFile();
					image.setFileName(CommonUtils.getFileName(dbItem.getImagePath()));
					item.setImage(image);
				}
			} else {
				BillDBItemParent dbItem = dao.getEntityByKey(BillDBItemParent.class, ID_ATTR, item.getId(), true);
				if (dbItem != null && StringUtils.isNotBlank(dbItem.getImagePath())) {
					is = new FileInputStream(dbItem.getImagePath());
					BillFile image = new BillFile();
					image.setFileName(CommonUtils.getFileName(dbItem.getImagePath()));
					item.setImage(image);
				}
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} finally {
			CommonUtils.closeSession(session);
		}
		return is;
	}

	public BillServiceResponse getAllparentItems(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			List<BillDBItemParent> items = dao.getEntities(BillDBItemParent.class, true, "name", "asc");
			response.setItems(BillDataConverter.getItems(items));
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse updateUserStatus(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		BillUser user = request.getUser();
		if (user == null || user.getId() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBUser existingUser = dao.getEntityByKey(BillDBUser.class, ID_ATTR, user.getId(), false);
			if (existingUser != null) {
				NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
				nullAwareBeanUtils.copyProperties(existingUser, user);
				if (StringUtils.equals(STATUS_ACTIVE, user.getStatus())) {
					BillUser approvedUser = new BillUser();
					nullAwareBeanUtils.copyProperties(approvedUser, existingUser);
					// User activated notification
					executor.execute(new BillMailUtil(MAIL_TYPE_APPROVAL, approvedUser));
					BillSMSUtil.sendSMS(approvedUser, null, MAIL_TYPE_APPROVAL, null);
					
					//Setup transaction charges
					List<BillDBUserBusiness> businesses = null;
					if(request.getBusiness() != null && request.getBusiness().getTransactionCharges() != null) {
						businesses = new BillVendorDaoImpl(session).getUserBusinesses(existingUser.getId());
						if(CollectionUtils.isNotEmpty(businesses)) {
							businesses.get(0).setTransactionCharges(request.getBusiness().getTransactionCharges());
						}
					}
					
					if(request.getScheme() != null) {
						//Logic to add referral scheme after approval
						BillDBSchemes scheme = dao.getEntityByKey(BillDBSchemes.class, "schemeCode", request.getScheme().getSchemeCode(), true);
						if(scheme != null) {
							BillDBCustomerCoupons coupons = BillBusinessConverter.getCustomerCoupon(scheme, null, null);
							if(coupons != null) {
								if(businesses == null) {
									businesses = new BillVendorDaoImpl(session).getUserBusinesses(existingUser.getId());
								}
								if(CollectionUtils.isNotEmpty(businesses)) {
									coupons.setAcceptedBy(businesses.get(0));
								}
								session.persist(coupons);
								BillRuleEngine.sendCouponMails(scheme, null, coupons, null, MAIL_TYPE_COUPON_ACCEPTED_BUSINESS, MAIL_TYPE_COUPON_ACCEPTED_ADMIN, executor, existingUser);
							}
						} else {
							response.setWarningText(ERROR_SCHEME_NOT_FOUND);
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

	public BillServiceResponse uploadVendorData(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getBusiness() == null || request.getFile() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillDBUserBusiness billDBUserBusiness = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserBusiness.class, ID_ATTR,
					request.getBusiness().getId(), true);
			if (billDBUserBusiness != null) {
				BillBusiness business = BillDataConverter.getBusiness(billDBUserBusiness);
				if(StringUtils.equals("EXTERNAL", request.getRequestType())) {
					String groupName = null;
					if(request.getCustomerGroup() != null) {
						groupName = request.getCustomerGroup().getGroupName();
					}
					BillExcelUtil.uploadCustomersFromExternal(request.getFile().getFileData(), business, session, executor, groupName, request.getInvoice());
				} else {
					BillExcelUtil.uploadCustomers(request.getFile().getFileData(), business, session, executor, request.getInvoice());
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

	public BillServiceResponse generateBills(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if (request.getInvoice() == null || request.getInvoice().getMonth() == null || request.getInvoice().getYear() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			Integer month = request.getInvoice().getMonth();
			Integer year = request.getInvoice().getYear();
			Date fromDate = CommonUtils.getMonthFirstDate(month, year);
			Date toDate = CommonUtils.getMonthLastDate(month, year);
			List<Object[]> result = new BillInvoiceDaoImpl(session).getCustomerOrderSummary(fromDate, toDate);
			System.out.println(result.size());
			List<Object[]> orderItemsResult = new BillInvoiceDaoImpl(session).getCustomerOrderItemSummary(fromDate, toDate);
			
			Map<Integer, BillUser> vendors = new HashMap<Integer, BillUser>();
			
			Integer businessId = null;
			
			if(request.getBusiness() != null) {
				businessId = request.getBusiness().getId();
			}
			
			Map<Integer, BillItem> priceMap = null;
			if(request.getFile() != null) {
				priceMap = BillExcelUtil.createPriceMap(request.getFile().getFileData());
				LoggingUtil.logMessage("Price map created =>" + priceMap);
			} else {
				response.setResponse(ERROR_CODE_GENERIC, "Price reference file not uploaded!");
				return response;
			}
			
			LoggingUtil.logMessage(" ### Invoice generation started for " + fromDate + " to " + toDate + " business " + businessId);
			
			if (CollectionUtils.isNotEmpty(result)) {
				for (Object[] row : result) {
					if (ArrayUtils.isEmpty(row)) {
						continue;
					}
					BigDecimal total = (BigDecimal) row[0];
					BillDBSubscription subscription = (BillDBSubscription) row[1];
					if (StringUtils.equals(STATUS_DELETED, subscription.getStatus())) {
						continue;
					}
					if (businessId != null && businessId.intValue() != subscription.getBusiness().getId().intValue()) {
						continue;
					}
					if(request.getUser() != null && request.getUser().getId() != null && request.getUser().getId().intValue() != subscription.getId().intValue()) {
						continue;
					}
					BillDBInvoice dbInvoice = new BillInvoiceDaoImpl(session).getInvoiceForMonth(subscription.getId(), month, year);
					if (dbInvoice == null) {
						dbInvoice = new BillDBInvoice();
						dbInvoice.setStatus(INVOICE_STATUS_PENDING);
						dbInvoice.setCreatedDate(new Date());
						dbInvoice.setMonth(month);
						dbInvoice.setYear(year);
						dbInvoice.setCreatedDate(new Date());
						dbInvoice.setSubscription(subscription);
					} else if (!StringUtils.equalsIgnoreCase(REQUEST_TYPE_OVERWRITE, request.getRequestType())) {
						LoggingUtil.logMessage("Invoice already exists => " + dbInvoice.getId() +  " for " + subscription.getId() + " total " + total);
						continue;
					}
					
					dbInvoice.setAmount(total);
					dbInvoice.setServiceCharge(subscription.getServiceCharge());
					
					if(subscription.getCreatedDate().compareTo(CommonUtils.getMonthFirstDate(month, year)) > 0) {
						if(priceMap == null) {
							LoggingUtil.logMessage("Price map not found .. ");
							continue;
						}
						//Customer is registered after month's first date.. so calculate invoice by excel sheet
						LoggingUtil.logMessage("Calculating by excel for subscription " + subscription + " of business " + subscription.getBusiness().getId());
						BillExcelUtil.createInvoiceFromReference(priceMap, session, subscription, orderItemsResult, dbInvoice, month);
					} else if (CollectionUtils.isNotEmpty(orderItemsResult)) {
						for (Object[] subRow : orderItemsResult) {
							if (ArrayUtils.isEmpty(subRow)) {
								continue;
							}
							BigDecimal totalPrice = (BigDecimal) subRow[0];
							BigDecimal totalQuantity = (BigDecimal) subRow[1];
							BillDBOrderItems orderItems = (BillDBOrderItems) subRow[2];
							BillDBSubscription orderItemSub = (BillDBSubscription) subRow[3];
							if (orderItemSub.getId().intValue() == subscription.getId().intValue()) {
								if (CollectionUtils.isNotEmpty(dbInvoice.getItems())) {
									for (BillDBItemInvoice invoiceItem : dbInvoice.getItems()) {
										if (invoiceItem.getBusinessItem().getId().intValue() == orderItems.getBusinessItem().getId().intValue()) {
											invoiceItem.setPrice(totalPrice);
											invoiceItem.setQuantity(totalQuantity);
										}
									}
								} else {
									BillDBItemInvoice itemInvoice = new BillDBItemInvoice();
									itemInvoice.setBusinessItem(orderItems.getBusinessItem());
									itemInvoice.setSubscribedItem(orderItems.getSubscribedItem());
									itemInvoice.setInvoice(dbInvoice);
									itemInvoice.setCreatedDate(new Date());
									itemInvoice.setStatus(STATUS_ACTIVE);
									itemInvoice.setPrice(totalPrice);
									itemInvoice.setQuantity(totalQuantity);
									session.persist(itemInvoice);
								}
							}
						}
					}
					if (dbInvoice.getId() == null) {
						session.persist(dbInvoice);
					}
					String userName = "";
					if(dbInvoice.getSubscription() != null) {
						userName = dbInvoice.getSubscription().getName();
					}
					LoggingUtil.logMessage("Generated invoice  "  + dbInvoice.getId() + " for user .. " + userName);
					//User map for sending mails later
					/*if(dbInvoice.getSubscription() != null && dbInvoice.getSubscription().getBusiness() != null && dbInvoice.getSubscription().getBusiness().getUser() != null) {
						BillUser user = vendors.get(dbInvoice.getSubscription().getBusiness().getUser().getId());
						if(user == null) {
							user = new BillUser();
							NullAwareBeanUtils nullBeans = new NullAwareBeanUtils();
							nullBeans.copyProperties(user, dbInvoice.getSubscription().getBusiness().getUser());
							user.setCurrentBusiness(BillDataConverter.getBusiness(dbInvoice.getSubscription().getBusiness()));
							BillInvoice currentInvoice = new BillInvoice();
							currentInvoice.setPayable(dbInvoice.getAmount());
							currentInvoice.setAmount(BigDecimal.ONE);
							currentInvoice.setYear(request.getInvoice().getYear());
							currentInvoice.setMonth(request.getInvoice().getMonth());
							user.setCurrentInvoice(currentInvoice);
							vendors.put(dbInvoice.getSubscription().getBusiness().getUser().getId(), user);
						} else {
							user.getCurrentInvoice().setAmount(user.getCurrentInvoice().getPayable().add(dbInvoice.getAmount()));
							user.getCurrentInvoice().setPayable(user.getCurrentInvoice().getAmount().add(BigDecimal.ONE));
						}
					}*/
				}
			}

			tx.commit();
			//Notify vendors for invoices generated
			if(CollectionUtils.isNotEmpty(vendors.entrySet()) && !StringUtils.equals(request.getRequestType(), REQUEST_TYPE_OVERWRITE)) {
				for(BillUser key: vendors.values()) {
					BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_INVOICE_GENERATION, key);
					mailUtil.setInvoice(key.getCurrentInvoice());
					executor.execute(mailUtil);
					BillSMSUtil.sendSMS(key, key.getCurrentInvoice(), MAIL_TYPE_INVOICE_GENERATION, null);
				}
			}
			LoggingUtil.logMessage(" ### Invoice generation ended ## ");
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse login(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		if(request == null || request.getUser() == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		try {
			String username = BillPropertyUtil.getProperty(BillPropertyUtil.ADMIN_USERNAME);
			String password = BillPropertyUtil.getProperty(BillPropertyUtil.ADMIN_PASSWORD);
			if(!StringUtils.equalsIgnoreCase(request.getUser().getEmail(), username) || !StringUtils.equals(request.getUser().getPassword(), password)) {
				response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_CREDENTIALS);
			} else {
				response.setResponse(BillPropertyUtil.getProperty(BillPropertyUtil.ADMIN_TOKEN));
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_GENERIC, ERROR_IN_PROCESSING);
		} finally {
			//CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getSummary(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Date startDate = null, endDate = null;
			if (request.getInvoice() != null && request.getInvoice().getMonth() != null && request.getInvoice().getYear() != null) {
				startDate = CommonUtils.getMonthFirstDate(request.getInvoice().getMonth(), request.getInvoice().getYear());
				endDate = CommonUtils.getMonthLastDate(request.getInvoice().getMonth(), request.getInvoice().getYear());
			}
			BillAdminDashboard dashboard = new BillAdminDashboard();
			Map<String, Object> restrictions = new HashMap<String, Object>();
			restrictions.put("status", BillConstants.INVOICE_STATUS_PAID);
			BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
			dashboard.setPaidInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count", null, null));
			dashboard.setTotalPaid((BigDecimal) billGenericDaoImpl.getSum(BillDBInvoice.class, "amount", restrictions, startDate, endDate, "sum", null, null));
			dashboard.setTotalGenerated((BigDecimal) billGenericDaoImpl.getSum(BillDBInvoice.class, "amount", null, startDate, endDate, "sum", null, null));
			dashboard.setTotalInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", null, startDate, endDate, "count", null, null));
			dashboard.setTotalCustomers((Long) billGenericDaoImpl.getSum(BillDBSubscription.class, "id", null, startDate, endDate, "count", null, null));
			dashboard.setTotalBusinesses((Long) billGenericDaoImpl.getSum(BillDBUserBusiness.class, "id", null, startDate, endDate, "count", null, null));
			restrictions.put("status", BillConstants.STATUS_PENDING);
			dashboard.setPendingApprovals((Long) billGenericDaoImpl.getSum(BillDBUser.class, "id", restrictions, startDate, endDate, "count", null, null));
			
			
			List<Object[]> resultSet = new BillAdminDaoImpl(session).getVendorProgressSummary(session);
			if(CollectionUtils.isNotEmpty(resultSet)) {
				List<BillBusinessSummary> summaryList = new ArrayList<BillBusinessSummary>();
				for(Object[] row: resultSet) {
					BillBusinessSummary summary = new BillBusinessSummary();
					BillUser user = new BillUser();
					if(ArrayUtils.isNotEmpty(row)) {
						user.setId((Integer) row[0]);
						user.setName((String) row[1]);
						user.setPhone((String) row[2]);
						BillBusiness business = new BillBusiness();
						business.setName((String) row[3]);
						business.setType((String) row[9]);
						if(StringUtils.equals(ACCESS_DISTRIBUTOR, business.getType())) {
							continue;
						}
						user.setCurrentBusiness(business);
						summary.setCustomerCount((BigInteger) row[4]);
						BillInvoice currentInvoice = new BillInvoice();
						currentInvoice.setPaidDate((Date) row[5]);
						if(row[8] != null) {
							currentInvoice.setNoOfReminders(((BigDecimal) row[8]).intValue());
						}
						user.setCurrentInvoice(currentInvoice);
						user.setCreatedDate((Date) row[7]);
						summary.setOnlinePaidCount((BigInteger) row[6]);
						summary.setUser(user);
						summaryList.add(summary);
					}
				}
				dashboard.setSummary(summaryList);
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

	public BillServiceResponse getAllVendors(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillDBUserBusiness> businesses = new BillVendorDaoImpl(session).getAllBusinesses();
			if(CollectionUtils.isEmpty(businesses)) {
				return response;
			}
			List<BillBusiness> businessList = new ArrayList<BillBusiness>();
			for(BillDBUserBusiness business: businesses) {
				BillBusiness userBusiness = BillDataConverter.getBusiness(business);
				businessList.add(userBusiness);
			}
			response.setBusinesses(businessList);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getSettlements(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillInvoiceDaoImpl billInvoiceDaoImpl = new BillInvoiceDaoImpl(session);
			//List<Object[]> resultSet = billInvoiceDaoImpl.getInvoiceSettlements(request.getRequestType());
			String status = "";
			if(StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT_INITIATE, request.getRequestType()) || StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT_PENDING, request.getRequestType())) {
				status = INVOICE_STATUS_PAID;
			} else if (StringUtils.equalsIgnoreCase(ACTION_SETTLEMENTS_DATA_EXPORT, request.getRequestType()) || StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT_INITIATED, request.getRequestType())) {
				status = INVOICE_SETTLEMENT_STATUS_INITIATED;
			} else if (StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT, request.getRequestType())) {
				status = INVOICE_SETTLEMENT_STATUS_INITIATED;
			} else if (StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT_COMPLETED, request.getRequestType())) {
				status = INVOICE_SETTLEMENT_STATUS_SETTLED;
			}
			Integer businessId = null;
			if(request.getBusiness() != null) {
				businessId = request.getBusiness().getId();
			}
			List<BillDBTransactions> resultSet = billInvoiceDaoImpl.getInvoiceSettlements(status, businessId);
			if(CollectionUtils.isNotEmpty(resultSet)) {
				List<BillBusiness> businesses = new ArrayList<BillBusiness>();
				Map<Integer, List<BillUser>> paidInvoiceMap = new HashMap<Integer, List<BillUser>>();
				for(BillDBTransactions txn: resultSet) {
					BigDecimal amount = /*CommonUtils.getAmount(row[0])*/ txn.getAmount();
					BillDBUserBusiness dbBusiness = txn.getBusiness();
					if(dbBusiness != null) {
						if(StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT_INITIATE, request.getRequestType())) {
							//Initiate the settlement for the transaction
							txn.setStatus(INVOICE_SETTLEMENT_STATUS_INITIATED);
							continue;
						} else if (StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT, request.getRequestType())) {
							txn.setStatus(INVOICE_SETTLEMENT_STATUS_SETTLED);
							txn.setSettlementDate(new Date());
							txn.setSettlementRef(request.getInvoice().getPaymentId());
							//Update all the invoices with settlement details
							List<BillDBInvoice> paidInvoices = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBInvoice.class, "paymentId", txn.getPaymentId(), false, null, null);
							if(paidInvoiceMap.get(dbBusiness.getId()) == null) {
								paidInvoiceMap.put(dbBusiness.getId(), new ArrayList<BillUser>());
							}
							if(CollectionUtils.isNotEmpty(paidInvoices)) {
								for(BillDBInvoice paidInvoice: paidInvoices) {
									paidInvoice.setSettlementDate(new Date());
									paidInvoice.setSettlementStatus(INVOICE_SETTLEMENT_STATUS_SETTLED);
									paidInvoice.setSettlementRef(request.getInvoice().getPaymentId());
									BillUser customer = BillDataConverter.getCustomerDetails(new NullAwareBeanUtils(), paidInvoice.getSubscription());
									//To keep track of business wise payments settled and send mail
									BillInvoice currentInvoice = BillDataConverter.getInvoice(new NullAwareBeanUtils(), paidInvoice);
									BillRuleEngine.calculatePayable(currentInvoice, null, null);
									customer.setCurrentInvoice(currentInvoice);
									paidInvoiceMap.get(dbBusiness.getId()).add(customer);
								}
							} else if (StringUtils.equals(PAYMENT_MODE_REWARD, txn.getPaymentMode())) {
								//Reward
								BillInvoice reward = new BillInvoice();
								reward.setAmount(txn.getAmount());
								reward.setPayable(txn.getAmount());
								reward.setPaymentMode(PAYMENT_MODE_REWARD);
								reward.setComments(txn.getComments());
								BillUser customer = new BillUser();
								if(txn.getSubscription() != null) {
									customer = BillDataConverter.getCustomerDetails(new NullAwareBeanUtils(), txn.getSubscription());
								}
								customer.setCurrentInvoice(reward);
								paidInvoiceMap.get(dbBusiness.getId()).add(customer);
							}
						}
						//Check for existing business. If present add the total to the same
						boolean found = false;
						for(BillBusiness existing: businesses) {
							if(existing.getId().intValue() == dbBusiness.getId().intValue()) {
								BillInvoice currentInvoice = existing.getOwner().getCurrentInvoice();
								currentInvoice.setAmount(currentInvoice.getAmount().add(BillRuleEngine.calculateSettlementAmount(txn.getAmount(), txn.getTransactionCharges())));
								if(txn.getTransactionCharges() != null) {
									if(currentInvoice.getTransactionCharges() == null) {
										currentInvoice.setTransactionCharges(BigDecimal.ZERO);
									}
									currentInvoice.setTransactionCharges(currentInvoice.getTransactionCharges().add(txn.getTransactionCharges()));
								}
								found = true;
								break;
							}
						}
						if(found) {
							continue;
						}
						BillInvoice invoice = new BillInvoice();
						invoice.setAmount(BillRuleEngine.calculateSettlementAmount(amount, txn.getTransactionCharges()));
						invoice.setTransactionCharges(txn.getTransactionCharges());
						BillBusiness business = BillDataConverter.getBusiness(dbBusiness);
						BillUser vendor = business.getOwner();
						BillDBUserFinancialDetails dbFinancials = new BillGenericDaoImpl(session).getEntityByKey(BillDBUserFinancialDetails.class, "user.id", vendor.getId() ,true);
						if(dbFinancials != null) {
							BillFinancialDetails financials = new BillFinancialDetails();
							new NullAwareBeanUtils().copyProperties(financials, dbFinancials);
							vendor.setFinancialDetails(financials);
						}
						vendor.setCurrentInvoice(invoice);
						businesses.add(business);
					}
				}
				response.setBusinesses(businesses);
				if(StringUtils.equalsIgnoreCase(ACTION_SETTLEMENTS_DATA_EXPORT, request.getRequestType())) {
					//Generate Excel for NEFT transfers
					response.setFile(BillExcelUtil.generateExcel(businesses));
				}
				if(StringUtils.equalsIgnoreCase(ACTION_SETTLEMENT, request.getRequestType())) {
					//Send mails to vendors with details of the settlement
					if(CollectionUtils.isNotEmpty(paidInvoiceMap.entrySet()) && CollectionUtils.isNotEmpty(businesses)) {
						for(Entry<Integer, List<BillUser>> e: paidInvoiceMap.entrySet()) {
							for(BillBusiness business: businesses) {
								if(business.getId() == e.getKey()) {
									BillUser owner = business.getOwner();
									BillBusiness ownerBusiness = new BillBusiness();
									ownerBusiness.setName(business.getName());
									owner.setCurrentBusiness(ownerBusiness);
									BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_SETTLEMENT_SUMMARY, owner);
									mailUtil.setUser(owner);
									mailUtil.setUsers(e.getValue());
									BillInvoice currentInvoice = owner.getCurrentInvoice();
									currentInvoice.setPaidDate(new Date());
									currentInvoice.setPayable(currentInvoice.getAmount());
									currentInvoice.setPaymentId(request.getInvoice().getPaymentId());
									mailUtil.setInvoice(currentInvoice);
									mailUtil.setCopyAdmins(true);
									executor.execute(mailUtil);
									BillSMSUtil.sendSMS(owner, currentInvoice, MAIL_TYPE_SETTLEMENT_SUMMARY, null);
									break;
								}
							}
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

	public BillServiceResponse updateOrders(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			if(request.getItem() != null) {
				if(request.getItem().getParentItemId() != null) {
					Date toDate = null;
					if(request.getItem().getChangeLog() != null) {
						toDate = request.getItem().getChangeLog().getToDate();
					}
					//Update all orders with this parent item
					List<BillDBOrderItems> orderItems = new BillOrderDaoImpl(session).getOrderItems(request.getRequestedDate(), toDate, request.getItem().getParentItemId(), request.getItem().getPriceType());
					if(CollectionUtils.isNotEmpty(orderItems)) {
						for(BillDBOrderItems orderItem: orderItems) {
							if(orderItem.getQuantity() == null) {
								continue;
							}
							if(StringUtils.equalsIgnoreCase("UPDATESP", request.getRequestType()) && orderItem.getAmount() != null) {
								BigDecimal newPrice = orderItem.getQuantity().multiply(request.getItem().getPrice());
								BigDecimal difference = newPrice.subtract(orderItem.getAmount());
								BillDBOrders order = new BillGenericDaoImpl(session).getEntityByKey(BillDBOrders.class, ID_ATTR, orderItem.getOrder().getId(), false);
								order.setAmount(order.getAmount().add(difference));
								orderItem.setAmount(newPrice);
								LoggingUtil.logMessage("Changed value =>" + difference + " .. " + orderItem.getOrder().getId() + " amount =>" + order.getAmount() + " item amount " + orderItem.getAmount());
							} else if(request.getItem().getCostPrice() != null) {
								orderItem.setCostPrice(orderItem.getQuantity().multiply(request.getItem().getCostPrice()));
							}
						}
					}
				}
			}
			tx.commit();
			LoggingUtil.logMessage("Transaction committed ..");
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse getTransactions(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			List<BillDBTransactions> transactions = new BillTransactionsDaoImpl(session).getTransactions(null, null, null);
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

	public BillServiceResponse updateLocations(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			if(request.getLocation().getId() != null) {
				BillDBLocation existing = new BillGenericDaoImpl(session).getEntityByKey(BillDBLocation.class, ID_ATTR, request.getLocation().getId(), true);
				if(existing == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_INVALID_ITEM);
					return response;
				}
				new NullAwareBeanUtils().copyProperties(existing, request.getLocation());
			} else {
				BillDBLocation location = new BillDBLocation();
				new NullAwareBeanUtils().copyProperties(location, request.getLocation());
				location.setCreatedDate(new Date());
				location.setStatus(STATUS_ACTIVE);
				session.persist(location);
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

	public BillServiceResponse sendNotifications(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;		
		try {
			session = this.sessionFactory.openSession();
			BillNotification notification = request.getNotification();
			List<BillUser> users = null;
			if(StringUtils.isBlank(notification.getRecepients())) {
				users = new ArrayList<BillUser>();
				Integer sector = null;
				if(request.getSector() != null) {
					sector = request.getSector().getId();
				}
				List<BillDBUserBusiness> userBusinesses = new BillAdminDaoImpl(session).getBusinesses(sector);
				if(CollectionUtils.isNotEmpty(userBusinesses)) {
					for(BillDBUserBusiness userBusiness: userBusinesses) {
						BillUser user = new BillUser();
						NullAwareBeanUtils nullBeans = new NullAwareBeanUtils();
						nullBeans.copyProperties(user, userBusiness.getUser());
						users.add(user);
					}
				}
			}
			
			if(StringUtils.equals(request.getRequestType(), "EMAIL")) {
				BillUser user = new BillUser();
				user.setEmail(notification.getRecepients());
				BillMailUtil generic = new BillMailUtil(MAIL_TYPE_GENERIC, user);
				generic.setMailSubject(notification.getSubject());
				generic.setMessageText(notification.getText());
				generic.setUsers(users);
				generic.setBulkEmail(true);
				executor.execute(generic);
			} else {
				BillUser user = new BillUser();
				if(StringUtils.isNotBlank(notification.getRecepients())) {
					user.setPhone(notification.getRecepients());
				} else {
					user.setPhone(BillSMSUtil.getPhones(users));
				}
				BillSMSUtil smsUtil = new BillSMSUtil();
				smsUtil.setMessageText(notification.getText());
				smsUtil.sendSms(user, null, MAIL_TYPE_GENERIC, null);
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(ERROR_CODE_FATAL, ERROR_IN_PROCESSING);
		} finally {
			CommonUtils.closeSession(session);
		}
		return response;
	}

	public BillServiceResponse notifyCustomers(BillServiceRequest request) {
		BillServiceResponse response = new BillServiceResponse();
		Session session = null;		
		try {
			session = this.sessionFactory.openSession();
			BillNotification notification = request.getNotification();
			if(StringUtils.equals("SCHEME_NOTIFY", request.getRequestType())) {
				//Get scheme details
				BillDBSchemes schemes = new BillGenericDaoImpl(session).getEntityByKey(BillDBSchemes.class, ID_ATTR, request.getScheme().getId(), true);
				if(schemes == null) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_FOUND);
					return response;
				}
				if (schemes.getValidFrom() != null && schemes.getValidFrom().compareTo(new Date()) > 0) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_NOT_STARTED);
					return response;
				}
				if (schemes.getValidTill() != null && schemes.getValidTill().compareTo(new Date()) < 0) {
					response.setResponse(ERROR_CODE_GENERIC, ERROR_SCHEME_EXPIRED);
					return response;
				}
				BillScheme scheme = new BillScheme();
				new NullAwareBeanUtils().copyProperties(scheme, schemes);
				BillBusiness currentBusiness = BillDataConverter.getBusiness(schemes.getBusiness());
				//Notify the (paying) customers about a new scheme
				List<Object[]> onlinePayers = new BillSubscriptionDAOImpl(session).getOnlinePayers(null, null);
				if(CollectionUtils.isNotEmpty(onlinePayers)) {
					System.out.println(onlinePayers.size());
					List<BillUser> users = new ArrayList<BillUser>();
					for(Object[] tx: onlinePayers) {
						LoggingUtil.logMessage(tx[0] + ":" + tx[1] + ":" + tx[2] + ":" + tx[3] + ":" + tx[4], LoggingUtil.smsLogger);
						BillUser user = new BillUser();
						user.setPhone(CommonUtils.getString(tx[0]));
						user.setName(CommonUtils.getString(tx[1]));
						user.setEmail(CommonUtils.getString(tx[2]));
						if(notification != null && StringUtils.isNotBlank(notification.getRecepients())) {
							if(notification.getChannel() != null && StringUtils.equals(REQUEST_TYPE_EMAIL, notification.getChannel()) && !StringUtils.equals(notification.getRecepients(), user.getEmail())) {
								continue;
							} else if (!CommonUtils.comparePhoneNumbers(notification.getRecepients(), user.getPhone())) {
								continue;
							}
							
						}
						BillInvoice invoice = new BillInvoice();
						invoice.setId(CommonUtils.getValue(tx[3], Integer.class));
						invoice.setPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + invoice.getId());
						invoice.setShortUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + invoice.getId());
						user.setCurrentInvoice(invoice);
						user.setCurrentBusiness(currentBusiness);
						users.add(user);
					}
					BillMessageBroadcaster broadcaster = new BillMessageBroadcaster(users, MAIL_TYPE_SCHEME_PROMOTION, notification.getChannel());
					broadcaster.setScheme(scheme);
					broadcaster.setSubject(scheme.getSchemeName());
					executor.execute(broadcaster);
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


}
