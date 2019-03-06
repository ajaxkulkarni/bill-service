package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerGroup;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillSubscriptionDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;

public class BillExcelUtil {

	private static final String TITLE_NAME = "Name";
	private static final String TITLE_PHONE = "Phone";
	private static final String TITLE_EMAIL = "Email";
	private static final String TITLE_ADDRESS = "Address";
	private static final String TITLE_LOCATION = "Location";
	private static final String TITLE_ITEMS = "Items";
	private static final String TITLE_LINE = "Line";
	private static final String TITLE_AMOUNT = "Bill Amount";
	private static final String TITLE_SERVICE_CHARGE = "Service charge";
	private static final String TITLE_DAYS = "Days";
	
	private static String[] RBL_EXCEL_COLUMNS = {"Payment Type","Cust Ref Number","Source Account Number","Source Narration","Destination Account Number",
												"Currency","Amount","Destination Narration","Destination bank","Destination Bank IFS Code",
												"Beneficiary Name","Beneficiary Account Type","Email"};


	public static void uploadCustomers(InputStream excel, BillBusiness business, Session session, ThreadPoolTaskExecutor executor, BillInvoice invoice)
			throws InvalidFormatException, IOException, IllegalAccessException, InvocationTargetException {
		Workbook workbook = WorkbookFactory.create(excel);

		Sheet sheet = workbook.getSheetAt(0);

		// Create a DataFormatter to format and get each cell's value as String
		DataFormatter dataFormatter = new DataFormatter();

		Integer colName = null, colEmail = null, colPhone = null, colLoc = null, colAddress = null, colItems = null, colSC = null;
		Integer colAmount = null, colLine = null, colDays = null, colPending = null, colTotal = null, colCredit = null;
		BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
		dbBusiness.setId(business.getId());
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
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_AMOUNT)) {
						colAmount = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_LINE)) {
						colLine = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, TITLE_DAYS)) {
						colDays = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Pending")) {
						colPending = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Total")) {
						colTotal = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Credit")) {
						colCredit = cell.getColumnIndex();
					}
				}
			} else {
				if (colName != null && colEmail != null && colPhone != null) {
					BillDBSubscription subscription = new BillDBSubscription();
					BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
					if (row.getCell(colPhone) != null) {
						String phone = dataFormatter.formatCellValue(row.getCell(colPhone));
						if (StringUtils.isBlank(phone)) {
							continue;
						}

						BillDBSubscription dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(phone, business.getId());
						if (dbSubscription != null) {
							//TODO remove later?
							//BillDBSubscription subscription2 = (BillDBSubscription) session.get(BillDBSubscription.class, dbSubscription.getId());
							//addSubscriptionItems(business, session, invoice, dataFormatter, colItems, colAmount, colDays, row, subscription2);
							continue;
						}
						/*BillDBUser existingUser = billGenericDaoImpl.getEntityByKey(BillDBUser.class, BillConstants.USER_DB_ATTR_PHONE, phone, true);
						if (existingUser != null) {
							continue;
						}*/
						subscription.setPhone(phone);
					} else {
						System.out.println("Phone number is NULL");
						continue;
					}
					if (row.getCell(colName) != null) {
						subscription.setName(row.getCell(colName).getStringCellValue());
					}
					if (row.getCell(colEmail) != null) {
						subscription.setEmail(row.getCell(colEmail).getStringCellValue());
					}
					if (row.getCell(colAddress) != null) {
						subscription.setAddress(row.getCell(colAddress).getStringCellValue());
					}
					if (row.getCell(colLoc) != null) {
						BillDBLocation loc = new BillDBLocation();
						String cellValue = dataFormatter.formatCellValue(row.getCell(colLoc));
						if(StringUtils.isNotBlank(cellValue)) {
							loc.setId(new Integer(cellValue));
							subscription.setLocation(loc);
						}
					}
					if (row.getCell(colSC) != null) {
						subscription.setServiceCharge(new BigDecimal(row.getCell(colSC).getNumericCellValue()));
					}
					BillDBCustomerGroup deliveryLine = null;
					if(row.getCell(colLine) != null) {
						String groupName = row.getCell(colLine).getStringCellValue();
						if(StringUtils.isNotBlank(groupName)) {
							deliveryLine = billGenericDaoImpl.getEntityByKey(BillDBCustomerGroup.class, "groupName", groupName, true);
							if(deliveryLine == null) {
								deliveryLine = new BillDBCustomerGroup();
								deliveryLine.setGroupName(groupName);
								deliveryLine.setCreatedDate(new Date());
								deliveryLine.setStatus(BillConstants.STATUS_ACTIVE);
								deliveryLine.setBusiness(dbBusiness);
								session.persist(deliveryLine);
							}
						}
					}
					subscription.setBusiness(dbBusiness);
					subscription.setCreatedDate(new Date());
					subscription.setStatus(BillConstants.STATUS_ACTIVE);
					subscription.setCustomerGroup(deliveryLine);
					if(deliveryLine != null) {
						subscription.setGroupSequence(BillRuleEngine.getNextGroupNumber(billGenericDaoImpl, subscription));
					}
					session.persist(subscription);
					BillUser customer = new BillUser();
					new NullAwareBeanUtils().copyProperties(customer, subscription);
					customer.setCurrentBusiness(business);
					/*if (StringUtils.isNotBlank(subscription.getEmail())) {
						executor.execute(new BillMailUtil(BillConstants.MAIL_TYPE_NEW_CUSTOMER, customer));
					}*/
					//TODO Later on actual adding BillSMSUtil.sendSMS(customer, null, BillConstants.MAIL_TYPE_NEW_CUSTOMER, null);
					LoggingUtil.logMessage("Added customer ..." + customer.getName());
					if (colPending != null && row.getCell(colPending) != null) {
						invoice.setPendingBalance(new BigDecimal(row.getCell(colPending).getNumericCellValue()));
					}
					if (colCredit != null && row.getCell(colCredit) != null) {
						invoice.setCreditBalance(new BigDecimal(row.getCell(colCredit).getNumericCellValue()));
					}
					if(colTotal != null && row.getCell(colTotal) != null) {
						invoice.setAmount(new BigDecimal(row.getCell(colTotal).getNumericCellValue()));
					}
					addSubscriptionItems(business, session, invoice, dataFormatter, colItems, colAmount, colDays, row, subscription);
				}
			}
			System.out.println(" .......... ");
		}

	}

	private static void addSubscriptionItems(BillBusiness business, Session session, BillInvoice invoice, DataFormatter dataFormatter, Integer colItems,
			Integer colAmount, Integer colDays, Row row, BillDBSubscription subscription) {
		if (row.getCell(colItems) != null) {
			String[] items = StringUtils.split(dataFormatter.formatCellValue(row.getCell(colItems)), ",");
			String[] days = null;
			if(colDays != null && row.getCell(colDays) != null) {
				days = StringUtils.split(row.getCell(colDays).getStringCellValue(), "|");
			}
			String[] amounts = null;
			Cell cell = row.getCell(colAmount);
			if(colAmount != null && cell != null) {
				if(cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
					Double amount = cell.getNumericCellValue();
					List<String> amountsList = new ArrayList<String>();
					amountsList.add(amount.toString());
					amounts = amountsList.toArray(new String[1]);
				} else {
					amounts = StringUtils.split(cell.getStringCellValue(), ",");
				}
			}
			if (ArrayUtils.isNotEmpty(items)) {
				Integer i = 0;
				BigDecimal amount = BigDecimal.ZERO;
				BillDBInvoice dbInvoice = null;
				for (String item : items) {
					if (StringUtils.isBlank(item)) {
						continue;
					}
					BillDBItemSubscription itemSubscription = new BillDBItemSubscription();
					if(StringUtils.contains(item, "(S)")) {
						item = StringUtils.removeEnd(item, "(S)");
						itemSubscription.setPrice(BigDecimal.ZERO);
						itemSubscription.setPriceType(BillConstants.FREQ_MONTHLY);
						if(ArrayUtils.isNotEmpty(amounts) && i < amounts.length) {
							String itemAmount = amounts[i];
							if(itemAmount != null && StringUtils.isNumeric(itemAmount)) {
								itemSubscription.setPrice(new BigDecimal(itemAmount));
							}
						}
						
					}
					BillDBItemBusiness businessItemByParent = new BillVendorDaoImpl(session).getBusinessItemByParent(new Integer(item),
							business.getId());
					if (businessItemByParent == null) {
						continue;
					}
					itemSubscription.setBusinessItem(businessItemByParent);
					itemSubscription.setCreatedDate(new Date());
					itemSubscription.setQuantity(new BigDecimal(1));
					itemSubscription.setStatus(BillConstants.STATUS_ACTIVE);
					itemSubscription.setSubscription(subscription);
					if(ArrayUtils.isNotEmpty(days)) {
						if(i < days.length) {
							String weekDays = days[i];
							if(StringUtils.isNotBlank(weekDays)) {
								itemSubscription.setWeekDays(weekDays);
							}
						}
					}
					session.persist(itemSubscription);
					// subItems.add(itemSubscription);
					if(ArrayUtils.isNotEmpty(amounts)) {
						if(i < amounts.length) {
							String itemAmount = amounts[i];
							dbInvoice = addInvoiceAmount(session, invoice, subscription, dbInvoice, itemSubscription, businessItemByParent, itemAmount);
						}
					} 
					i++;
				}
				if(dbInvoice == null) {
					dbInvoice = prepareInvoice(subscription, invoice);
					dbInvoice.setAmount(invoice.getAmount());
					session.persist(dbInvoice);
				}
				// subscription.setSubscriptions(subItems);
				if(dbInvoice != null && dbInvoice.getAmount().equals(BigDecimal.ZERO)) {
					dbInvoice.setServiceCharge(subscription.getServiceCharge());
				}
				if(dbInvoice != null) {
					dbInvoice.setPendingBalance(invoice.getPendingBalance());
					dbInvoice.setCreditBalance(invoice.getCreditBalance());
				}
			}
		}
	}

	private static BillDBInvoice prepareInvoice(BillDBSubscription subscription, BillInvoice requestInvoice) {
		BillDBInvoice invoice = new BillDBInvoice();
		invoice.setStatus(BillConstants.INVOICE_STATUS_PENDING);
		invoice.setCreatedDate(new Date());
		invoice.setMonth(requestInvoice.getMonth());
		invoice.setYear(requestInvoice.getYear());
		if(requestInvoice.getServiceCharge() != null) {
			invoice.setServiceCharge(requestInvoice.getServiceCharge());
		} else {
			invoice.setServiceCharge(subscription.getServiceCharge());
		}
		invoice.setSubscription(subscription);
		return invoice;
	}

	public static BillFile generateExcel(List<BillBusiness> businesses) {
		// Create a Workbook
		Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for
												// generating `.xls` file

		/*
		 * CreationHelper helps us create instances of various things like
		 * DataFormat, Hyperlink, RichTextString etc, in a format (HSSF, XSSF)
		 * independent way
		 */
		CreationHelper createHelper = workbook.getCreationHelper();

		// Create a Sheet
		Sheet sheet = workbook.createSheet("Transfers");

		// Create a Font for styling header cells
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		//headerFont.setFontHeightInPoints((short) 14);
		//headerFont.setColor(IndexedColors.RED.getIndex());

		// Create a CellStyle with the font
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		// Create a Row
		Row headerRow = sheet.createRow(0);

		// Create cells
		for (int i = 0; i < RBL_EXCEL_COLUMNS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(RBL_EXCEL_COLUMNS[i]);
			cell.setCellStyle(headerCellStyle);
		}

		// Create Cell Style for formatting Date
		CellStyle dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

		// Create Other rows and cells with employees data
		int rowNum = 1;
		for (BillBusiness business : businesses) {
			
			if(business.getOwner() == null || business.getOwner().getFinancialDetails() == null) {
				continue;
			}
			
			Row row = sheet.createRow(rowNum++);

			row.createCell(0).setCellValue("NFT");

			row.createCell(1).setCellValue("" + business.getOwner().getId());
			
			row.createCell(2).setCellValue("409423040642");

		/*	Cell dateOfBirthCell = row.createCell(2);
			dateOfBirthCell.setCellValue(business.getDateOfBirth());
			dateOfBirthCell.setCellStyle(dateCellStyle);*/

			row.createCell(3).setCellValue("To NEFT");
			
			row.createCell(4).setCellValue(business.getOwner().getFinancialDetails().getAccountNumber());
			
			row.createCell(5).setCellValue("INR");
			
			row.createCell(6).setCellValue(CommonUtils.getStringValue(business.getOwner().getCurrentInvoice().getAmount(), true));
			
			row.createCell(7).setCellValue(business.getName());
			
			row.createCell(8).setCellValue(business.getOwner().getFinancialDetails().getBankName());
			
			row.createCell(9).setCellValue(business.getOwner().getFinancialDetails().getIfscCode());
			
			row.createCell(10).setCellValue(business.getOwner().getFinancialDetails().getAccountHolderName());
			
			row.createCell(11).setCellValue("Saving");
			
			row.createCell(12).setCellValue(business.getOwner().getEmail());
			
		}

		// Resize all columns to fit the content size
		for (int i = 0; i < RBL_EXCEL_COLUMNS.length; i++) {
			sheet.autoSizeColumn(i);
		}

		BillFile file = new BillFile();
		file.setFileName("transfers_" + CommonUtils.convertDate(new Date()) + ".xlsx");
		file.setWb(workbook);
		return file;
		/*// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream("transfers_" + CommonUtils.convertDate(new Date()) + ".xlsx");
		workbook.write(fileOut);
		fileOut.close();

		// Closing the workbook
		workbook.close();*/
	}
	
	public static void uploadCustomersFromExternal(InputStream excel, BillBusiness business, Session session, ThreadPoolTaskExecutor executor, String groupName, BillInvoice invoice) throws IllegalAccessException, InvocationTargetException, InvalidFormatException, IOException {
		Workbook workbook = WorkbookFactory.create(excel);
		Sheet sheet = workbook.getSheetAt(0);

		// Create a DataFormatter to format and get each cell's value as String
		DataFormatter dataFormatter = new DataFormatter();
		//S.N.	Customer name	Newspapers Details	Readers days	Qty	Mobile no
		Integer colName = null, colEmail = null, colPhone = null, colLoc = null, colAddress = null, colItems = null, colSC = null;
		Integer colAmount = null, colDays = null;
		BillDBUserBusiness dbBusiness = new BillDBUserBusiness();
		dbBusiness.setId(business.getId());
		Integer count = 0;
		for (Row row : sheet) {
			if (row.getRowNum() == 0) { // Title row
				for (Cell cell : row) {
					String cellValue = dataFormatter.formatCellValue(cell);
					System.out.print(cellValue + "\t");
					if (StringUtils.equalsIgnoreCase(cellValue, "Customer name")) {
						colName = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Mobile no")) {
						colPhone = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Newspapers Details")) {
						colItems = cell.getColumnIndex();
					} else if (StringUtils.equalsIgnoreCase(cellValue, "Readers days")) {
						colDays = cell.getColumnIndex();
					}
				}
			} else {
				if (colName != null && colPhone != null) {
					BillDBSubscription subscription = new BillDBSubscription();
					BillGenericDaoImpl billGenericDaoImpl = new BillGenericDaoImpl(session);
					if (row.getCell(colPhone) != null) {
						String phone = dataFormatter.formatCellValue(row.getCell(colPhone));
						if (StringUtils.isBlank(phone)) {
							continue;
						}

						BillDBSubscription dbSubscription = new BillSubscriptionDAOImpl(session).getActiveSubscription(phone, business.getId());
						if (dbSubscription != null) {
							System.out.println("Already saved the number .." + phone);
							continue;
						}
						/*BillDBUser existingUser = billGenericDaoImpl.getEntityByKey(BillDBUser.class, BillConstants.USER_DB_ATTR_PHONE, phone, true);
						if (existingUser != null) {
							continue;
						}*/
						subscription.setPhone(phone);
					}
					if (row.getCell(colName) != null) {
						subscription.setName(row.getCell(colName).getStringCellValue());
					}
					if(StringUtils.isBlank(subscription.getPhone()) || StringUtils.isBlank(subscription.getName())) {
						LoggingUtil.logMessage("Phone number/ name not found ..");
						continue;
					}
					/*if (row.getCell(colSC) != null) {
						subscription.setServiceCharge(new BigDecimal(row.getCell(colSC).getNumericCellValue()));
					}*/
					subscription.setServiceCharge(new BigDecimal(30)); //TODO 
					BillDBCustomerGroup deliveryLine = null;
					if(StringUtils.isNotBlank(groupName)) {
						deliveryLine = billGenericDaoImpl.getEntityByKey(BillDBCustomerGroup.class, "groupName", groupName, true);
						if(deliveryLine == null) {
							deliveryLine = new BillDBCustomerGroup();
							deliveryLine.setGroupName(groupName);
							deliveryLine.setCreatedDate(new Date());
							deliveryLine.setStatus(BillConstants.STATUS_ACTIVE);
							deliveryLine.setBusiness(dbBusiness);
							session.persist(deliveryLine);
						}
					}
					subscription.setBusiness(dbBusiness);
					subscription.setCreatedDate(new Date());
					subscription.setStatus(BillConstants.STATUS_ACTIVE);
					subscription.setCustomerGroup(deliveryLine);
					if(deliveryLine != null) {
						subscription.setGroupSequence(BillRuleEngine.getNextGroupNumber(billGenericDaoImpl, subscription));
					}
					session.persist(subscription);
					count++;
					/*if (StringUtils.isNotBlank(subscription.getEmail())) {
						executor.execute(new BillMailUtil(BillConstants.MAIL_TYPE_NEW_CUSTOMER, customer));
					}*/
					//TODO Later on actual adding BillSMSUtil.sendSMS(customer, null, BillConstants.MAIL_TYPE_NEW_CUSTOMER, null);
					LoggingUtil.logMessage("Added customer ..." + subscription.getName());
					
					
					if (row.getCell(colItems) != null) {
						String[] items = StringUtils.split(dataFormatter.formatCellValue(row.getCell(colItems)), "\n");
						String[] days = null;
						String[] amounts = {};
						if(colDays != null && row.getCell(colDays) != null) {
							days = StringUtils.split(row.getCell(colDays).getStringCellValue(), " ");
							if(days.length < items.length) {
								//Splitting on the basis of next line
								days = StringUtils.split(row.getCell(colDays).getStringCellValue(), "\n");
							}
						}
						//System.out.println("Items =>" + items[0]);
						//System.out.println("Days =>" + days[0]);
						
						if (ArrayUtils.isNotEmpty(items)) {
							Integer i = 0;
							BigDecimal amount = BigDecimal.ZERO;
							BillDBInvoice dbInvoice = null;
							for (String item : items) {
								if (StringUtils.isBlank(item)) {
									continue;
								}
								item = StringUtils.removeStart(item, "1.");
								item = StringUtils.removeStart(item, "2.");
								item = StringUtils.removeStart(item, "3.");
								item = StringUtils.removeStart(item, "4.");
								item = StringUtils.removeStart(item, "5.");
								item = StringUtils.removeStart(item, "6.");
								item = StringUtils.removeStart(item, "7.");
								item = StringUtils.removeStart(item, "8.");
								
								item = StringUtils.trimToEmpty(item);
								//Get parent item by name
								BillDBItemParent subParentItem = null;
								BillDBItemParent parentItem = billGenericDaoImpl.getEntityByKey(BillDBItemParent.class, "name", item, true);
								if(parentItem == null) {
									if(StringUtils.equalsIgnoreCase("TOI", item)) {
										//TOI means Times of India + Mirror
										parentItem = billGenericDaoImpl.getEntityByKey(BillDBItemParent.class, "name", "Times of India", true);
										subParentItem = billGenericDaoImpl.getEntityByKey(BillDBItemParent.class, "name", "Mirror", true);
									} else {
										System.out.println("parent item not found for ==> " + item);
										continue;
									}
								}
								System.out.println("parent item found for ==> " + item);
								BillDBItemSubscription itemSubscription = new BillDBItemSubscription();
								if(StringUtils.contains(days[i], "Scheme")) {
									itemSubscription.setPrice(BigDecimal.ZERO);
									itemSubscription.setPriceType(BillConstants.FREQ_MONTHLY);
									/*if(ArrayUtils.isNotEmpty(amounts) && i < amounts.length) {
										String itemAmount = amounts[i];
										if(itemAmount != null && StringUtils.isNumeric(itemAmount)) {
											itemSubscription.setPrice(new BigDecimal(itemAmount));
										}
									}*/
								} else if (StringUtils.contains(days[i], ",")) {
									String daysToDeliver = StringUtils.replace(days[i], "Sun", "1");
									daysToDeliver = StringUtils.replace(days[i], "Sun", "1");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Mon", "2");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Tue", "3");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Wed", "4");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Thu", "5");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Fri", "6");
									daysToDeliver = StringUtils.replace(daysToDeliver, "Sat", "7");
									daysToDeliver = StringUtils.replacePattern(daysToDeliver, "\\s+","");
									itemSubscription.setWeekDays(daysToDeliver);
									System.out.println("Days ..... " + daysToDeliver);
									
								}
								BillDBItemBusiness businessItemByParent = new BillVendorDaoImpl(session).getBusinessItemByParent(parentItem.getId(),
										business.getId());
								if (businessItemByParent == null) {
									continue;
								}
								itemSubscription.setBusinessItem(businessItemByParent);
								itemSubscription.setCreatedDate(new Date());
								itemSubscription.setQuantity(new BigDecimal(1));
								itemSubscription.setStatus(BillConstants.STATUS_ACTIVE);
								itemSubscription.setSubscription(subscription);
								
								BillDBItemSubscription subItemSubscription = null;
								BillDBItemBusiness subBusinessItem = null;
								if(subParentItem != null) {
									subBusinessItem = new BillVendorDaoImpl(session).getBusinessItemByParent(subParentItem.getId(),
											business.getId());
									subItemSubscription = new BillDBItemSubscription();
									new NullAwareBeanUtils().copyProperties(subItemSubscription, itemSubscription);
									subItemSubscription.setBusinessItem(subBusinessItem);
									session.persist(subItemSubscription);
								}
								
								session.persist(itemSubscription);
								if(ArrayUtils.isNotEmpty(amounts)) {
									if(i < amounts.length) {
										String itemAmount = amounts[i];
										dbInvoice = addInvoiceAmount(session, invoice, subscription, dbInvoice, itemSubscription, businessItemByParent,
												itemAmount);
									}
								} else {
									BigDecimal predefinedCost = COSTS.get(parentItem.getId());
									if(predefinedCost == null && itemSubscription.getPrice() == null) {
										System.out.println("Cost not found for .. " + parentItem.getId());
										continue;
									} else if (predefinedCost == null) {
										predefinedCost = itemSubscription.getPrice();
									}
									System.out.println("Calculated amount is =>" + predefinedCost);
									dbInvoice = addInvoiceAmount(session, invoice, subscription, dbInvoice, itemSubscription, businessItemByParent,
											predefinedCost.toString());
									//For sub item e.g. Mirror
									if(subItemSubscription != null) {
										BigDecimal predefinedCost2 = COSTS.get(subParentItem.getId());
										dbInvoice = addInvoiceAmount(session, invoice, subscription, dbInvoice, subItemSubscription, subBusinessItem,
												predefinedCost2.toString());
									}
								}
								i++;
							}
							if(dbInvoice != null && dbInvoice.getAmount().equals(BigDecimal.ZERO)) {
								dbInvoice.setServiceCharge(subscription.getServiceCharge());
							}
						}
					}
					
				}
			}
			System.out.println(" .......... ");
		}
		System.out.println("Total customers => " + count);
	}

	private static BillDBInvoice addInvoiceAmount(Session session, BillInvoice invoice, BillDBSubscription subscription,
			BillDBInvoice dbInvoice, BillDBItemSubscription itemSubscription, BillDBItemBusiness businessItemByParent, String itemAmount) {
		if(StringUtils.isNotBlank(itemAmount)) {
			if(dbInvoice == null && invoice != null) {
				dbInvoice = prepareInvoice(subscription, invoice);
				dbInvoice.setAmount(BigDecimal.ZERO);
				session.persist(dbInvoice);
				System.out.println("........ Created Invoice ...... #" + dbInvoice.getId());
			}
			if(dbInvoice != null) {
				BillDBItemInvoice invoiceItem = new BillDBItemInvoice();
				invoiceItem.setStatus(BillConstants.STATUS_ACTIVE);
				invoiceItem.setCreatedDate(new Date());
				invoiceItem.setInvoice(dbInvoice);
				invoiceItem.setQuantity(new BigDecimal("31"));
				invoiceItem.setPrice(new BigDecimal(itemAmount));
				invoiceItem.setSubscribedItem(itemSubscription);
				invoiceItem.setBusinessItem(businessItemByParent);
				session.persist(invoiceItem);
				dbInvoice.setAmount(dbInvoice.getAmount().add(invoiceItem.getPrice()));
			}
			//dbInvoice.setAmount(amount);
		}
		return dbInvoice;
	}
	
	private static Map<Integer, BigDecimal> COSTS = Collections.unmodifiableMap(new HashMap<Integer, BigDecimal>() {
		{
			/*put(8, new BigDecimal("165"));//Sakal
			put(9, new BigDecimal("180"));//Pune times (Without Mirror)
			put(13, new BigDecimal("95"));//Mirror
			put(14, new BigDecimal("185"));//Loksatta
			put(11, new BigDecimal("225"));//Economic
			put(16, new BigDecimal("160"));//Lokmat
			put(31, new BigDecimal("360"));//Hindu
			put(33, new BigDecimal("190"));//Navbharat
			put(29, new BigDecimal("250"));//Aaj anand
			put(40, new BigDecimal("220"));//Gujrat
			put(25, new BigDecimal("170"));//Prabhat
			put(32, new BigDecimal("185"));//NBT
			put(49, new BigDecimal("40"));//Zee disha
			//put(, new BigDecimal("360"));//Anand bazaar
			put(15, new BigDecimal("185")); //with +30 //Indian
			put(20, new BigDecimal("70"));//Wealth 8*5 = 40
			put(55, new BigDecimal("123"));//original 93
			put(37, new BigDecimal("166"));//Mint 136 + 30
			put(67, new BigDecimal("183"));//Inadu
			put(10, new BigDecimal("157"));//Ma Ta 127 is original
			put(43, new BigDecimal("158"));//Punyanagari 128 is original
			put(26, new BigDecimal("163"));//Pudhari 133
			put(66, new BigDecimal("158"));//Yashobhumi 128
			put(50, new BigDecimal("158"));//Samrat 128
			put(12, new BigDecimal("42"));//Speaking tree 12 = 3*4
			put(23, new BigDecimal("127"));//Hindustan times 97
			put(21, new BigDecimal("254"));//Business standard 224
			put(42, new BigDecimal("246"));//Business line 216
			put(30, new BigDecimal("247"));//Sandhyanand 217
			put(28, new BigDecimal("121"));//Agro one 91
			put(61, new BigDecimal("262.5"));//Matrubhumi 232.5
			put(27, new BigDecimal("154"));//Sakal times 124
			put(35, new BigDecimal("185"));//Pratyaksha 155
			put(70, new BigDecimal("189"));//Sakshi 159
			put(36, new BigDecimal("123"));//Mumbai Chopher 93
			*///Inqalab 222
			//Without SC
			put(8, new BigDecimal("127"));//Sakal
			put(9, new BigDecimal("141"));//Pune times (Without Mirror)
			put(13, new BigDecimal("93"));//Mirror
			put(14, new BigDecimal("150"));//Loksatta
			put(11, new BigDecimal("225"));//Economic
			put(16, new BigDecimal("120"));//Lokmat
			put(31, new BigDecimal("305"));//Hindu
			put(33, new BigDecimal("159"));//Navbharat
			put(29, new BigDecimal("217"));//Aaj anand
			put(40, new BigDecimal("180"));//Gujrat
			put(25, new BigDecimal("133"));//Prabhat
			put(32, new BigDecimal("155"));//NBT
			put(49, new BigDecimal("40"));//Zee disha
			//put(, new BigDecimal("360"));//Anand bazaar
			put(15, new BigDecimal("155")); //with +30 //Indian
			put(20, new BigDecimal("40"));//Wealth 8*5 = 40
			put(55, new BigDecimal("93"));//original 93
			put(37, new BigDecimal("136"));//Mint 136 + 30
			put(67, new BigDecimal("153"));//Inadu
			put(10, new BigDecimal("127"));//Ma Ta 127 is original
			put(43, new BigDecimal("128"));//Punyanagari 128 is original
			put(26, new BigDecimal("133"));//Pudhari 133
			put(66, new BigDecimal("128"));//Yashobhumi 128
			put(50, new BigDecimal("128"));//Samrat 128
			put(12, new BigDecimal("12"));//Speaking tree 12 = 3*4
			put(23, new BigDecimal("97"));//Hindustan times 97
			put(21, new BigDecimal("224"));//Business standard 224
			put(42, new BigDecimal("216"));//Business line 216
			put(30, new BigDecimal("217"));//Sandhyanand 217
			put(28, new BigDecimal("91"));//Agro one 91
			put(61, new BigDecimal("232.5"));//Matrubhumi 232.5
			put(27, new BigDecimal("124"));//Sakal times 124
			put(35, new BigDecimal("155"));//Pratyaksha 155
			put(70, new BigDecimal("159"));//Sakshi 159
			put(36, new BigDecimal("93"));//Mumbai Chopher 93
			put(57, new BigDecimal("155"));//Udayvani 155
			put(71, new BigDecimal("188"));//Manorama 188
			put(68, new BigDecimal("155"));//Malla karnatak 155
			put(34, new BigDecimal("121"));//Samana 121
			put(19, new BigDecimal("218"));//Financial 218
		
		}
	});
	//67,37,10,55,15,20
	//Not found from other app Eenadu -> Inadu, Pune times -> Times of India, Navbharat -> Hindi Navabharat, Times of India -> TOI + Mirror, Hindu -> THe Hindu, THe Hindustan Times - Hindustan Times

	public static Map<Integer, BigDecimal> createPriceMap(InputStream excel) throws InvalidFormatException, IOException {
		Workbook workbook = WorkbookFactory.create(excel);
		Sheet sheet = workbook.getSheetAt(0);
		Map<Integer, BigDecimal> priceMap = new HashMap<Integer, BigDecimal>();
		for (Row row : sheet) {
			if (row.getRowNum() == 0) { // Title row
				continue;
			} else {
				if (row.getCell(0) != null && row.getCell(1) != null) {
					priceMap.put(new Double(row.getCell(0).getNumericCellValue()).intValue(), new BigDecimal(row.getCell(1).getNumericCellValue()));
				}
			}
		}
		return priceMap;
	}
	
	public static void createInvoiceFromReference(Map<Integer, BigDecimal> priceMap, Session session, BillDBSubscription subscription,
			List<Object[]> orderItems, BillDBInvoice dbInvoice, Integer month)
			throws InvalidFormatException, IOException, IllegalAccessException, InvocationTargetException {
		if(CollectionUtils.isEmpty(priceMap.keySet())) {
			return;
		}
		BigDecimal quantity = new BigDecimal(CommonUtils.getMonthDays(month));
		BigDecimal total = BigDecimal.ZERO;
		for (Entry<Integer, BigDecimal> e : priceMap.entrySet()) {
			if (CollectionUtils.isNotEmpty(orderItems)) {
				for (Object[] subRow : orderItems) {
					if (ArrayUtils.isEmpty(subRow)) {
						continue;
					}
					BillDBOrderItems orderItem = (BillDBOrderItems) subRow[2];
					BillDBSubscription orderItemSub = (BillDBSubscription) subRow[3];
					if (orderItemSub.getId().intValue() == subscription.getId().intValue()) {
						if (orderItem.getBusinessItem() != null && orderItem.getBusinessItem().getParent() != null
								&& e.getKey() == orderItem.getBusinessItem().getParent().getId().intValue()) {
							if (CollectionUtils.isNotEmpty(dbInvoice.getItems())) {
								for (BillDBItemInvoice invoiceItem : dbInvoice.getItems()) {
									if (invoiceItem.getBusinessItem().getId().intValue() == orderItem.getBusinessItem().getId().intValue()) {
										invoiceItem.setPrice(e.getValue());
										invoiceItem.setQuantity(quantity);
										total = total.add(invoiceItem.getPrice());
									}
								}
							} else {
								BillDBItemInvoice itemInvoice = new BillDBItemInvoice();
								itemInvoice.setBusinessItem(orderItem.getBusinessItem());
								itemInvoice.setSubscribedItem(orderItem.getSubscribedItem());
								itemInvoice.setInvoice(dbInvoice);
								itemInvoice.setCreatedDate(new Date());
								itemInvoice.setStatus(BillConstants.STATUS_ACTIVE);
								itemInvoice.setPrice(e.getValue());
								itemInvoice.setQuantity(quantity);
								session.persist(itemInvoice);
								total = total.add(itemInvoice.getPrice());
							}
						}
					}
				}
			}
		}
		dbInvoice.setAmount(total);
		System.out.println(" .......... ");
	}
	
}
