package com.rns.web.billapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.rns.web.billapp.service.bo.api.BillUserBo;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillInvoice;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Component
@Path("/user")
public class BillUserController {

	private static final String APPLICATION_PDF = "application/pdf";
	@Autowired(required = true)
	@Qualifier(value = "userBo")
	BillUserBo userBo;
	
	public void setUserBo(BillUserBo userBo) {
		this.userBo = userBo;
	}
	
	public BillUserBo getUserBo() {
		return userBo;
	}
	
	@POST
	@Path("/updateUserBasicInfo")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateUserBasicInfo(
		@FormDataParam("pan") InputStream panCardFile, @FormDataParam("pan") FormDataContentDisposition panCardFileDetails,
		@FormDataParam("aadhar") InputStream aadharCardFile, @FormDataParam("aadhar") FormDataContentDisposition aadharCardFileDetails,
		@FormDataParam("user") String user) {
		LoggingUtil.logObject("User update request", user);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			BillUser billUser = mapper.readValue(user, BillUser.class);
			if(billUser != null) {
				if(panCardFile != null) {
					BillFile file = new BillFile();
					file.setFileData(panCardFile);
					file.setFileSize(new BigDecimal((panCardFileDetails.getSize())));
					file.setFileType(panCardFileDetails.getType());
					file.setFilePath(panCardFileDetails.getFileName());
					billUser.setPanFile(file);
				}
				if(aadharCardFile != null) {
					BillFile file = new BillFile();
					file.setFileData(aadharCardFile);
					file.setFileSize(new BigDecimal((aadharCardFileDetails.getSize())));
					file.setFileType(aadharCardFileDetails.getType());
					file.setFilePath(aadharCardFileDetails.getFileName());
					billUser.setAadharFile(file);
				}
				BillServiceRequest request = new BillServiceRequest();
				request.setUser(billUser);
				response = userBo.updateUserInfo(request);
			} else {
				response.setResponse(BillConstants.ERROR_CODE_GENERIC, BillConstants.ERROR_IN_PROCESSING);
			}
			
		} catch (JsonParseException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(BillConstants.ERROR_CODE_GENERIC, BillConstants.ERROR_IN_PROCESSING);
		} catch (JsonMappingException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(BillConstants.ERROR_CODE_GENERIC, BillConstants.ERROR_IN_PROCESSING);
		} catch (IOException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
			response.setResponse(BillConstants.ERROR_CODE_GENERIC, BillConstants.ERROR_IN_PROCESSING);
		}
		LoggingUtil.logObject("User update response", response);
		return response;
	}
	
	@POST
	@Path("/loadProfile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse loadProfile(BillServiceRequest request){
		return userBo.loadProfile(request);
	}
	
	@POST
	@Path("/getAllAreas")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAreas(BillServiceRequest request){
		return userBo.getAllAreas();
	}
	
	@POST
	@Path("/updateUserProfile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateUserProfile(BillServiceRequest request){
		return userBo.updateUserInfo(request);
	}
	
	@POST
	@Path("/updateBankDetails")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBankDetails(BillServiceRequest request){
		return userBo.updateUserFinancialInfo(request);
	}
	
	@POST
	@Path("/loadBusinessItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse loadBusinessItems(BillServiceRequest request){
		return userBo.getBusinessItems(request);
	}
	
	@POST
	@Path("/loadSectorItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse loadSectorItems(BillServiceRequest request){
		return userBo.getSectorItems(request);
	}
	
	@POST
	@Path("/updateBusinessItem")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessItem(BillServiceRequest request){
		return userBo.updateBusinessItem(request);
	}
	
	@POST
	@Path("/getAllCustomers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllCustomers(BillServiceRequest request){
		return userBo.getAllBusinessCustomers(request);
	}
	
	@POST
	@Path("/updateCustomer")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomer(BillServiceRequest request){
		return userBo.updateCustomerInfo(request);
	}
	
	@POST
	@Path("/getCustomerProfile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerProfile(BillServiceRequest request){
		return userBo.getCustomerProfile(request);
	}
	
	@POST
	@Path("/updateCustomerItem")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerItem(BillServiceRequest request){
		return userBo.updateCustomerItem(request);
	}
	
	@POST
	@Path("/updateCustomerItemTemp")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerItemTemp(BillServiceRequest request){
		return userBo.updateCustomerItemTemporary(request);
	}
	
	@POST
	@Path("/getCustomerInvoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerInvoices(BillServiceRequest request){
		return userBo.getCustomerInvoices(request);
	}
	
	@POST
	@Path("/updateCustomerInvoice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerInvoice(BillServiceRequest request){
		return userBo.updateCustomerInvoice(request);
	}
	
	@POST
	@Path("/sendInvoice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse sendInvoice(BillServiceRequest request){
		return userBo.sendCustomerInvoice(request);
	}
	
	//{amount=[105.00], fees=[2.00], shorturl=[], purpose=[January 2018 monthly payment], buyer_phone=[+911122112211], buyer_name=[Anand], payment_request_id=[1337c3d8fcc94be992c70827bc052461], mac=[9ae3e3e13488a282b4f6322b0a9d5ae0d883c0fb], buyer=[ajinkyashiva@gmail.com], payment_id=[MOJO8603005A85529733], longurl=[https://test.instamojo.com/@crkstructural/1337c3d8fcc94be992c70827bc052461], currency=[INR], status=[Credit]}
	@POST
	@Path("/paymentResult")
	//@Produces(MediaType.APPLICATION_JSON)
	public Response paymentResult(MultivaluedMap<String, String> formParams){
		URI url = null;
		try {
			LoggingUtil.logMessage("Payment result -- " + formParams);
			BillInvoice invoice = new BillInvoice();
			invoice.setPaymentRequestId(formParams.get("payment_request_id").get(0).toString());
			invoice.setPaymentId(formParams.get("payment_id").get(0).toString());
			invoice.setStatus(formParams.get("status").get(0).toString());
			invoice.setAmount(new BigDecimal(formParams.get("amount").get(0).toString()));
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			url = new URI(URLEncoder.encode(response.getInvoice().getPaymentUrl() , "UTF-8"));
		} catch (URISyntaxException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} catch (UnsupportedEncodingException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		
		return Response.temporaryRedirect(url).build();
	}
}
