package com.rns.web.billapp.service.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.rns.web.billapp.service.bo.domain.BillCustomerGroup;
import com.rns.web.billapp.service.bo.domain.BillUser;

public class BillPDFUtil {
	
	private List<BillUser> customers;
	private BillUser vendor;
	private String type;
	private BillCustomerGroup group;
	
	public static String PENDING_INVOICES = "pendingInvoices";
	
	public BillPDFUtil() {

	}

	
	public BillPDFUtil(BillUser vendor, List<BillUser> subscriptions, String pdfType) {
		this.vendor = vendor;
		this.customers = subscriptions;
		this.type = pdfType;
	}
	
	public void setGroup(BillCustomerGroup group) {
		this.group = group;
	}

	private String generatePDFFromHTML(String content) throws DocumentException, IOException {
		Document document = new Document();
		String outputPath = BillConstants.ROOT_FOLDER_LOCATION +  "pdf/" + vendor.getCurrentBusiness().getId();
		File outputFolder = new File(outputPath);
		if(!outputFolder.exists()) {
			outputFolder.mkdirs();
		}
		outputPath = outputPath +  "/html.pdf";
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
		document.open();
		InputStream is = new ByteArrayInputStream(content.getBytes());
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, is);
		document.close();
		System.out.println("Done!");
		return outputPath;
	}
	
	public FileInputStream preparePDF() throws DocumentException, IOException {
		
		String content = CommonUtils.readFile("reports/" + PDF_TEMPLATES.get(type));
		if(vendor != null) {
			if(vendor.getCurrentBusiness() != null) {
				content = StringUtils.replace(content, "{businessName}", CommonUtils.getStringValue(vendor.getCurrentBusiness().getName()));
				String groupName = "Show all";
				if(group != null) {
					groupName = group.getGroupName();
				}
				content = StringUtils.replace(content, "{selectedGroup}", "Selected group: " + groupName);
			}
		}
		if(StringUtils.isNotBlank(content)) {
			if(CollectionUtils.isNotEmpty(customers)) {
				StringBuilder customerList = new StringBuilder();
				int index = 1;
				for(BillUser customer: customers) {
					customerList.append("<tr>");
					customerList.append("<td>" + index + "</td>");
					customerList.append("<td>" + CommonUtils.getStringValue(customer.getName()) + "</td>");
					customerList.append("<td>" + CommonUtils.getStringValue(customer.getAddress()) + "</td>");
					String payable = "NA";
					if(customer.getCurrentInvoice() != null && customer.getCurrentInvoice().getPayable() != null) {
						payable = CommonUtils.getStringValue(customer.getCurrentInvoice().getPayable(), true);
					}
					customerList.append("<td>" + payable + "</td>");
					customerList.append("</tr>");
					index++;
				}
				content = StringUtils.replace(content, "{tbody}", customerList.toString());
			}
		}
		return new FileInputStream(generatePDFFromHTML(content));
	}	
	
	private static Map<String, String> PDF_TEMPLATES = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put(PENDING_INVOICES, "pending-invoices.html");
		}
	});

}
