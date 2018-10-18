package com.rns.web.billapp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.rns.web.billapp.service.bo.api.BillCustomerBo;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;

@Component
@Path("/customer")
public class BillCustomerController {
	
	@Autowired(required = true)
	@Qualifier(value = "customerBo")
	private BillCustomerBo customerBo;
	
	public BillCustomerBo getCustomerBo() {
		return customerBo;
	}
	public void setCustomerBo(BillCustomerBo customerBo) {
		this.customerBo = customerBo;
	}

	
	@POST
	@Path("/acceptScheme")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getInvoiceSummary(BillServiceRequest request) {
		return customerBo.updateScheme(request);
	}
	
	@POST
	@Path("/getSchemes")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getSchemes(BillServiceRequest request) {
		return customerBo.getAllSchemes(request);
	}
	
	@POST
	@Path("/redeemScheme")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse redeemScheme(BillServiceRequest request) {
		return customerBo.redeemScheme(request);
	}
}
