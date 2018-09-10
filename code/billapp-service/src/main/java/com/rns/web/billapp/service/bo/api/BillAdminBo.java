package com.rns.web.billapp.service.bo.api;

import java.io.InputStream;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillAdminBo {
	
	BillServiceResponse login(BillServiceRequest request);
	BillServiceResponse updateUserStatus(BillServiceRequest request);
	BillServiceResponse updateItem(BillServiceRequest request);
	InputStream getImage(BillItem item);
	BillServiceResponse getAllparentItems(BillServiceRequest request);
	BillServiceResponse uploadVendorData(BillServiceRequest request);
	BillServiceResponse generateBills(BillServiceRequest request);
	BillServiceResponse getSummary(BillServiceRequest request);
	BillServiceResponse getAllVendors(BillServiceRequest request);
	//BillServiceResponse updateBusiness(BillServiceRequest request);
	
	
}
