package com.rns.web.billapp.service.bo.api;

import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillBusinessBo {
	
	BillServiceResponse updateScheme(BillServiceRequest request);
	BillServiceResponse applyCoupon(BillServiceRequest request);
	BillServiceResponse getAllSchemes(BillServiceRequest request);
	BillServiceResponse getAllVendors(BillServiceRequest request);
	BillServiceResponse updateCustomerBill(BillServiceRequest request);
	BillServiceResponse getAllInvoices(BillServiceRequest request);

}
