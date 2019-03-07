package com.rns.web.billapp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.rns.web.billapp.service.bo.api.BillBusinessBo;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

@Component
@Path("/business")
public class BillBusinessController {

	@Autowired(required = true)
	@Qualifier(value = "businessBo")
	private BillBusinessBo businessBo;
	
	public BillBusinessBo getBusinessBo() {
		return businessBo;
	}
	
	public void setBusinessBo(BillBusinessBo businessBo) {
		this.businessBo = businessBo;
	}
	
	@POST
	@Path("/updateScheme")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateScheme(BillServiceRequest request) {
		return businessBo.updateScheme(request);
	}
	
	@POST
	@Path("/updateBill")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBill(BillServiceRequest request) {
		return businessBo.updateCustomerBill(request);
	}
	
	@POST
	@Path("/getAllInvoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllInvoices(BillServiceRequest request) {
		return businessBo.getAllInvoices(request);
	}
	
}
