package com.rns.web.billapp.service.bo.api;

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
	
	/*Read APIs*/
	
	BillServiceResponse loadProfile(BillServiceRequest request); //also login
	BillServiceResponse getAllAreas();
	BillServiceResponse getSectorItems(BillServiceRequest request);
	BillServiceResponse getBusinessItems(BillServiceRequest request);
	BillServiceResponse getAllBusinessCustomers(BillServiceRequest request);
	BillServiceResponse loadDeliveries(BillServiceRequest request);
	BillServiceResponse getCustomerProfile(BillServiceRequest request);
	BillServiceResponse getCustomerInvoices(BillServiceRequest request);
	
}
