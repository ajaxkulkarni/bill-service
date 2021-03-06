package com.rns.web.billapp.service.bo.api;

import java.io.FileInputStream;

import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillUserBo {
	
	/*Write APIs*/
	
	BillServiceResponse updateUserInfo(BillServiceRequest request);
	BillServiceResponse updateUserFinancialInfo(BillServiceRequest request);
	BillServiceResponse updateBusinessItem(BillServiceRequest request);
	BillServiceResponse updateCustomerInfo(BillServiceRequest request);
	BillServiceResponse updateCustomerItem(BillServiceRequest request);
	BillServiceResponse updateCustomerItemTemporary(BillServiceRequest request);
	BillServiceResponse updateCustomerInvoice(BillServiceRequest request);
	BillServiceResponse sendCustomerInvoice(BillServiceRequest request);
	BillServiceResponse updatePaymentCredentials(BillServiceRequest request);
	BillServiceResponse completePayment(BillServiceRequest request);
	BillServiceResponse updateInvoiceItems(BillServiceRequest request);
	BillServiceResponse updateCustomerGroup(BillServiceRequest request);
	BillServiceResponse updateGroupCustomers(BillServiceRequest request);
	BillServiceResponse updateBusinessInvoice(BillServiceRequest request);
	
	
	/*Read APIs*/
	
	BillServiceResponse loadProfile(BillServiceRequest request); //also login
	BillServiceResponse getAllAreas();
	BillServiceResponse getAllSectors();
	BillServiceResponse getSectorItems(BillServiceRequest request);
	BillServiceResponse getBusinessItems(BillServiceRequest request);
	BillServiceResponse getAllBusinessCustomers(BillServiceRequest request);
	BillServiceResponse loadDeliveries(BillServiceRequest request);
	BillServiceResponse getCustomerProfile(BillServiceRequest request);
	BillServiceResponse getCustomerInvoices(BillServiceRequest request);
	BillServiceResponse getDailySummary(BillServiceRequest request);
	BillServiceResponse getInvoiceSummary(BillServiceRequest request);
	BillServiceResponse getCustomerActivity(BillServiceRequest request);
	BillServiceResponse getFile(BillServiceRequest request);
	BillServiceResponse getTransactions(BillServiceRequest request);
	BillServiceResponse getCustomerBillsSummary(BillServiceRequest request);
	BillServiceResponse getAllCustomerGroups(BillServiceRequest request);
	BillServiceResponse getPaymentsReport(BillServiceRequest request);
	BillServiceResponse getBusinessInvoicesForBusiness(BillServiceRequest request);
	BillServiceResponse getBusinessesByType(BillServiceRequest request);
	BillServiceResponse getBusinessItemsByDate(BillServiceRequest request);
	FileInputStream downloadPendingInvoices(BillServiceRequest request);
	
}
