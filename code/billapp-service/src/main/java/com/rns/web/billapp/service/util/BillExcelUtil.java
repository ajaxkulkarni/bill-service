package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hibernate.Session;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;

public class BillExcelUtil {

	private static final String TITLE_NAME = "Name";
	private static final String TITLE_PHONE = "Phone";
	private static final String TITLE_EMAIL = "Email";
	private static final String TITLE_ADDRESS = "Address";
	private static final String TITLE_LOCATION = "Location";
	private static final String TITLE_ITEMS = "Items";
	private static final String TITLE_SERVICE_CHARGE = "Service charge";

	public static void uploadCustomers(InputStream excel, BillBusiness business, Session session, ThreadPoolTaskExecutor executor) throws InvalidFormatException, IOException, IllegalAccessException, InvocationTargetException {
		Workbook workbook = WorkbookFactory.create(excel);

		Sheet sheet = workbook.getSheetAt(0);

		// Create a DataFormatter to format and get each cell's value as String
		DataFormatter dataFormatter = new DataFormatter();

		Integer colName = null, colEmail = null, colPhone = null, colLoc = null, colAddress = null, colItems = null, colSC = null;
		for (Row row : sheet) {
			if (row.getRowNum() == 0) { // Title row
				for (Cell cell : row) {
					String cellValue = dataFormatter.formatCellValue(cell);
					System.out.print(cellValue + "\t");
					if (StringUtils.equalsIgnoreCase(cellValue, TITLE_NAME)) {
						colName = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_PHONE)) {
						colPhone = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_EMAIL)) {
						colEmail = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_ADDRESS)) {
						colAddress = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_LOCATION)) {
						colLoc = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_ITEMS)) {
						colItems = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_SERVICE_CHARGE)) {
						colSC = cell.getColumnIndex();
					}
				}
			} else {
				if(colName != null && colEmail != null && colPhone != null) {
					BillDBSubscription subscription = new BillDBSubscription();
					BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
					if(row.getCell(colPhone) != null) {
						String phone = dataFormatter.formatCellValue(row.getCell(colPhone));
						if(StringUtils.isBlank(phone)) {
							continue;
						}
						
						BillDBSubscription dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(phone, business.getId());
						if (dbSubscription != null) {
							continue;
						}
						BillDBUser existingUser = billGenericDaoImpl.getEntityByKey(BillDBUser.class, BillConstants.USER_DB_ATTR_PHONE, phone, true);
						if(existingUser != null) {
							continue;
						}
						subscription.setPhone(phone);
					}
					if(row.getCell(colName) != null) {
						subscription.setName(row.getCell(colName).getStringCellValue());
					}
					if(row.getCell(colEmail) != null) {
						subscription.setEmail(row.getCell(colEmail).getStringCellValue());
					}
					if(row.getCell(colAddress) != null) {
						subscription.setAddress(row.getCell(colAddress).getStringCellValue());
					}
					if(row.getCell(colLoc) != null) {
						BillDBLocation loc = new BillDBLocation();
						loc.setId(new Integer(dataFormatter.formatCellValue(row.getCell(colLoc))));
						subscription.setLocation(loc);
					}
					if(row.getCell(colSC) != null) {
						subscription.setServiceCharge(new BigDecimal(row.getCell(colSC).getNumericCellValue()));
					}
					BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
					dbBusiness.setId(business.getId());
					subscription.setBusiness(dbBusiness);
					subscription.setCreatedDate(new Date());
					subscription.setStatus(BillConstants.STATUS_ACTIVE);
					session.persist(subscription);
					BillUser customer = new BillUser();
					new NullAwareBeanUtils().copyProperties(customer, subscription);
					customer.setCurrentBusiness(business);
					if(StringUtils.isNotBlank(subscription.getEmail())) {
						executor.execute(new BillMailUtil(BillConstants.MAIL_TYPE_NEW_CUSTOMER, customer));
					}
					BillSMSUtil.sendSMS(customer, null, BillConstants.MAIL_TYPE_NEW_CUSTOMER);
					LoggingUtil.logMessage("Added customer ..." + customer.getName());
					if(row.getCell(colItems) != null) {
						String[] items = StringUtils.split(dataFormatter.formatCellValue(row.getCell(colItems)), ",");
						if(ArrayUtils.isNotEmpty(items)) {
							for(String item: items) {
								if(StringUtils.isBlank(item)) {
									continue;
								}
								BillDBItemSubscription itemSubscription = new BillDBItemSubscription();
								BillDBItemBusiness businessItemByParent = new BillVendorDaoImpl(session).getBusinessItemByParent(new Integer(item), business.getId());
								if(businessItemByParent == null) {
									continue;
								}
								itemSubscription.setBusinessItem(businessItemByParent);
								itemSubscription.setCreatedDate(new Date());
								itemSubscription.setQuantity(new BigDecimal(1));
								itemSubscription.setStatus(BillConstants.STATUS_ACTIVE);
								itemSubscription.setSubscription(subscription);
								session.persist(itemSubscription);
								//subItems.add(itemSubscription);
							}
							//subscription.setSubscriptions(subItems);
						}
					}
				}
			}
			System.out.println();
		}


	}

}
