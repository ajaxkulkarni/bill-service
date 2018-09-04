package com.rns.web.billapp.service.bo.api;

import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillCustomerBo {

	BillServiceResponse getScheme(BillServiceRequest request); //get scheme details
	BillServiceResponse updateScheme(BillServiceRequest request); //Accept/update coupon
	BillServiceResponse payScheme(BillServiceRequest request); //Complete Payment
	BillServiceResponse getAllSchemes(BillServiceRequest request); //Get all available offers
	
}
