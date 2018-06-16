package com.rns.web.billapp.service.bo.api;

import java.io.InputStream;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillAdminBo {
	
	BillServiceResponse updateUserStatus(BillServiceRequest request);
	BillServiceResponse updateItem(BillServiceRequest request);
	InputStream getImage(BillItem item);
	BillServiceResponse getAllparentItems(BillServiceRequest request);
	
}