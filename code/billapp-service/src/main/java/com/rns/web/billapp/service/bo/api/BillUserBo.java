package com.rns.web.billapp.service.bo.api;

import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillUserBo {
	
	BillServiceResponse updateUserInfo(BillServiceRequest request);
	BillServiceResponse updateUserFinancialInfo(BillServiceRequest request);
	BillServiceResponse updateBusinessItem(BillServiceRequest request);
	BillServiceResponse updateCustomerInfo(BillServiceRequest request);
	BillServiceResponse updateCustomerItem(BillServiceRequest request);
	
}
