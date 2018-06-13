package com.rns.web.billapp.service.bo.impl;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.api.BillAdminBo;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBSector;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillBusinessConverter;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillPaymentUtil;
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
		if(item == null) {
			response.setResponse(ERROR_CODE_GENERIC, ERROR_INSUFFICIENT_FIELDS);
			return response;
		}
		Session session = null;
		try {
			session = this.sessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBItemParent dbItem = dao.getEntityByKey(BillDBItemParent.class, ID_ATTR, item.getId(), true);
			if(dbItem == null) {
				dbItem = new BillDBItemParent();
				dbItem.setCreatedDate(new Date());
				dbItem.setStatus(STATUS_ACTIVE);
			}
			NullAwareBeanUtils nullBean = new NullAwareBeanUtils();
			nullBean.copyProperties(dbItem, item);
			if(dbItem.getId() == null) {
				session.persist(dbItem);
			}
			if(dbItem.getSector() == null) {
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
		String folderPath = ROOT_FOLDER_LOCATION + "Items/" +  dbItem.getId() + "/";
		File folderLocation = new File(folderPath);
		if(!folderLocation.exists()) {
			folderLocation.mkdirs();
		}
		if(item.getImage() != null) {
			String imgPath = folderPath + item.getImage().getFilePath();
			CommonUtils.writeToFile(item.getImage().getFileData(), imgPath);
			dbItem.setImagePath(imgPath);
		}
		
	}

	public InputStream getImage(BillItem item) {
		if(item == null) {
			return null;
		}
		Session session = null;
		InputStream is = null;
		try {
			session = this.sessionFactory.openSession();
			BillGenericDaoImpl dao = new BillGenericDaoImpl(session);
			BillDBItemParent dbItem = dao.getEntityByKey(BillDBItemParent.class, ID_ATTR, item.getId(), true);
			if(dbItem != null && StringUtils.isNotBlank(dbItem.getImagePath())) {
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
			List<BillDBItemParent> items = dao.getEntities(BillDBItemParent.class,  true);
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
			if(existingUser != null) {
				NullAwareBeanUtils nullAwareBeanUtils = new NullAwareBeanUtils();
				nullAwareBeanUtils.copyProperties(existingUser, user);
				if(StringUtils.equals(STATUS_ACTIVE, user.getStatus())) {
					BillUser approvedUser = new BillUser();
					nullAwareBeanUtils.copyProperties(approvedUser, existingUser);
					//User activated notification
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



}
