package com.rns.web.billapp.service.bo.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillExcelUtil;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillPropertyUtil;
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
			updateItemImage(item, dbItem);
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

	private void updateItemImage(BillItem item, BillDBItemParent dbItem) throws IOException {
		String folderPath = ROOT_FOLDER_LOCATION + "Items/" + dbItem.getId() + "/";
		File folderLocation = new File(folderPath);
		if (!folderLocation.exists()) {
			folderLocation.mkdirs();
		}
		if (item.getImage() != null) {
			String imgPath = folderPath + item.getImage().getFilePath();
			CommonUtils.writeToFile(item.getImage().getFileData(), imgPath);
			dbItem.setImagePath(imgPath);
		}

	}

	public InputStream getImage(BillItem item) {
		if (item == null) {
			return null;
		}
		Session session = null;
		InputStream is = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBItemParent dbItem = dao.getEntityByKey(BillDBItemParent.class, ID_ATTR, item.getId(), true);
			if (dbItem != null && StringUtils.isNotBlank(dbItem.getImagePath())) {
				is = new FileInputStream(dbItem.getImagePath());
				BillFile image = new BillFile();
				image.setFileName(CommonUtils.getFileName(dbItem.getImagePath()));
				item.setImage(image);
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
			List<BillDBItemParent> items = dao.getEntities(BillDBItemParent.class, true);
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
					BillSMSUtil.sendSMS(approvedUser, null, MAIL_TYPE_APPROVAL);
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
				BillExcelUtil.uploadCustomers(request.getFile().getFileData(), business, session, executor);
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
			List<Object[]> orderItemsResult = new BillInvoiceDaoImpl(session).getCustomerOrderItemSummary(fromDate, toDate);
			
			Map<Integer, BillUser> vendors = new HashMap<Integer, BillUser>();
			
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
					if (request.getBusiness() != null && request.getBusiness().getId() != subscription.getBusiness().getId()) {
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
					} else if (!StringUtils.equalsIgnoreCase("Overwrite", request.getRequestType())) {
						continue;
					}

					dbInvoice.setAmount(total);
					dbInvoice.setServiceCharge(subscription.getServiceCharge());

					if (CollectionUtils.isNotEmpty(orderItemsResult)) {
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
					//User map for sending mails later
					if(dbInvoice.getSubscription() != null && dbInvoice.getSubscription().getBusiness() != null && dbInvoice.getSubscription().getBusiness().getUser() != null) {
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
					}
				}
			}

			tx.commit();
			//Notify vendors for invoices generated
			if(CollectionUtils.isNotEmpty(vendors.entrySet())) {
				for(BillUser key: vendors.values()) {
					BillMailUtil mailUtil = new BillMailUtil(MAIL_TYPE_INVOICE_GENERATION, key);
					mailUtil.setInvoice(key.getCurrentInvoice());
					executor.execute(mailUtil);
					BillSMSUtil.sendSMS(key, key.getCurrentInvoice(), MAIL_TYPE_INVOICE_GENERATION);
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
			dashboard.setPaidInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", restrictions, startDate, endDate, "count"));
			dashboard.setTotalPaid((BigDecimal) billGenericDaoImpl.getSum(BillDBInvoice.class, "amount", restrictions, startDate, endDate, "sum"));
			dashboard.setTotalGenerated((BigDecimal) billGenericDaoImpl.getSum(BillDBInvoice.class, "amount", null, startDate, endDate, "sum"));
			dashboard.setTotalInvoices((Long) billGenericDaoImpl.getSum(BillDBInvoice.class, "id", null, startDate, endDate, "count"));
			dashboard.setTotalCustomers((Long) billGenericDaoImpl.getSum(BillDBSubscription.class, "id", null, startDate, endDate, "count"));
			dashboard.setTotalBusinesses((Long) billGenericDaoImpl.getSum(BillDBUserBusiness.class, "id", null, startDate, endDate, "count"));
			restrictions.put("status", BillConstants.STATUS_PENDING);
			dashboard.setPendingApprovals((Long) billGenericDaoImpl.getSum(BillDBUser.class, "id", restrictions, startDate, endDate, "count"));
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

}
