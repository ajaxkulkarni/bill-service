package com.rns.web.billapp.service.bo.api;

import java.util.Date;

import com.rns.web.billapp.service.domain.BillServiceResponse;

public interface BillSchedulerBo {
	
	void calculateOrders();
	BillServiceResponse calculateInvoices(Date date);

}
