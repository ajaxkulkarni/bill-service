package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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

import com.itextpdf.text.pdf.PdfEncodings;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerGroup;
import com.rns.web.billapp.service.dao.domain.BillDBInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemInvoice;
import com.rns.web.billapp.service.dao.domain.BillDBItemSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
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
		Integer colAmount = null, colLine = null, colDays = null;
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
							continue;
						}
						BillDBUser existingUser = billGenericDaoImpl.getEntityByKey(BillDBUser.class, BillConstants.USER_DB_ATTR_PHONE, phone, true);
						if (existingUser != null) {
							continue;
						}
						subscription.setPhone(phone);
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
					BillUser customer = new BillUser();
					new NullAwareBeanUtils().copyProperties(customer, subscription);
					customer.setCurrentBusiness(business);
					/*if (StringUtils.isNotBlank(subscription.getEmail())) {
						executor.execute(new BillMailUtil(BillConstants.MAIL_TYPE_NEW_CUSTOMER, customer));
					}*/
					//TODO Later on actual adding BillSMSUtil.sendSMS(customer, null, BillConstants.MAIL_TYPE_NEW_CUSTOMER, null);
					LoggingUtil.logMessage("Added customer ..." + customer.getName());
					if (row.getCell(colItems) != null) {
						String[] items = StringUtils.split(dataFormatter.formatCellValue(row.getCell(colItems)), ",");
						String[] days = null;
						if(colDays != null && row.getCell(colDays) != null) {
							days = StringUtils.split(row.getCell(colDays).getStringCellValue(), "|");
						}
						String[] amounts = null;
						if(colAmount != null && row.getCell(colAmount) != null) {
							amounts = StringUtils.split(row.getCell(colAmount).getStringCellValue(), ",");
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
										if(StringUtils.isNotBlank(itemAmount)) {
											if(dbInvoice == null && invoice != null) {
												dbInvoice = prepareInvoice(subscription, invoice);
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
												amount = amount.add(invoiceItem.getPrice());
											}
											dbInvoice.setAmount(amount);
										}
									}
								}
								i++;
							}
							// subscription.setSubscriptions(subItems);
							if(dbInvoice != null && dbInvoice.getAmount().equals(BigDecimal.ZERO)) {
								dbInvoice.setServiceCharge(subscription.getServiceCharge());
							}
						}
					}
				}
			}
			System.out.println(" .......... ");
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

}
