package com.rns.web.billapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.rns.web.billapp.service.bo.api.BillAdminBo;
import com.rns.web.billapp.service.bo.api.BillSchedulerBo;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Component
@Path("/admin")
public class BillAdminController {

	private static final String APPLICATION_PDF = "application/pdf";
	@Autowired(required = true)
	@Qualifier(value = "adminBo")
	BillAdminBo adminBo;
	
	public BillAdminBo getAdminBo() {
		return adminBo;
	}
	
	public void setAdminBo(BillAdminBo adminBo) {
		this.adminBo = adminBo;
	}
	
	@Autowired(required = true)
	@Qualifier(value = "schedulerBo")
	BillSchedulerBo schedulerBo;
	
	public BillSchedulerBo getSchedulerBo() {
		return schedulerBo;
	}
	public void setSchedulerBo(BillSchedulerBo schedulerBo) {
		this.schedulerBo = schedulerBo;
	}
	
	@POST
	@Path("/updateParentItem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateUserBasicInfo(
		@FormDataParam("image") InputStream image, @FormDataParam("image") FormDataContentDisposition imageFileDetails,
		@FormDataParam("item") String item) {
		LoggingUtil.logObject("Parent Item update request", item);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			BillItem billItem = mapper.readValue(item, BillItem.class);
			if(billItem != null) {
				if(image != null) {
					BillFile file = new BillFile();
					file.setFileData(image);
					file.setFileSize(new BigDecimal((imageFileDetails.getSize())));
					file.setFileType(imageFileDetails.getType());
					file.setFilePath(imageFileDetails.getFileName());
					billItem.setImage(file);
				}
				BillServiceRequest request = new BillServiceRequest();
				request.setItem(billItem);
				response = adminBo.updateItem(request);
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
		LoggingUtil.logObject("Parent Item update response", response);
		return response;
	}
	
	@GET
	@Path("/getParentItemImage/{itemId}")
	@Produces(MediaType.MULTIPART_FORM_DATA)
	public Response getImage(@PathParam("itemId") Integer itemId) {
		//LoggingUtil.logObject("Image request:", userId);
		try {
			
			BillItem item = new BillItem();
			item.setId(itemId);
			InputStream is = adminBo.getImage(item);
			ResponseBuilder response = Response.ok(is);
			if(item.getImage() != null) {
				response.header("Content-Disposition", "filename=" + item.getImage().getFileName());
			} else {
				response.header("Content-Disposition", "filename=" + "image.png");
			}
			return response.build();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@POST
	@Path("/getAllItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllItems() {
		return adminBo.getAllparentItems(new BillServiceRequest());
	}
	
	@POST
	@Path("/generateOrders")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse generateOrders(BillServiceRequest request) {
		return schedulerBo.calculateInvoices(request.getRequestedDate());
	}
	
	@POST
	@Path("/updateUserStatus")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse approveVendor(BillServiceRequest request) {
		return adminBo.updateUserStatus(request);
	}
	
	
}