package com.rns.web.billapp.service.bo.api;

import java.util.Date;

public interface BillSchedulerBo {
	
	void calculateInvoices();
	void calculateInvoices(Date date);

}
