package com.rns.web.billapp.service.bo.api;

import java.io.InputStream;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillAdminBo {
	
	BillServiceResponse login(BillServiceRequest request);
	BillServiceResponse updateUserStatus(BillServiceRequest request);
	BillServiceResponse updateItem(BillServiceRequest request);
	InputStream getImage(BillItem item, String type);
	BillServiceResponse getAllparentItems(BillServiceRequest request);
	BillServiceResponse uploadVendorData(BillServiceRequest request);
	BillServiceResponse generateBills(BillServiceRequest request);
	BillServiceResponse getSummary(BillServiceRequest request);
	BillServiceResponse getAllVendors(BillServiceRequest request);
	//BillServiceResponse updateBusiness(BillServiceRequest request);
	BillServiceResponse getSettlements(BillServiceRequest request); //get initiated/pending/settled
	BillServiceResponse updateOrders(BillServiceRequest request);
	BillServiceResponse getTransactions(BillServiceRequest request);
	BillServiceResponse updateLocations(BillServiceRequest request);
	BillServiceResponse sendNotifications(BillServiceRequest request);
	BillServiceResponse notifyCustomers(BillServiceRequest request);
}
