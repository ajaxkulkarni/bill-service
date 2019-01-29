package com.rns.web.billapp.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.rns.web.billapp.service.bo.api.BillAdminBo;
import com.rns.web.billapp.service.bo.api.BillSchedulerBo;
import com.rns.web.billapp.service.bo.api.BillUserBo;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;
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
	@Path("/updateParentItem")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateParentItem(
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
			InputStream is = adminBo.getImage(item, "ParentItem");
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
	
	@GET
	@Path("/getBusinessItemImage/{itemId}")
	@Produces(MediaType.MULTIPART_FORM_DATA)
	public Response getBusinessItemImage(@PathParam("itemId") Integer itemId) {
		//LoggingUtil.logObject("Image request:", userId);
		try {
			
			BillItem item = new BillItem();
			item.setId(itemId);
			InputStream is = adminBo.getImage(item, "BusinessItem");
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
	
	
	@POST
	@Path("/uploadCustomers")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse uploadCustomers(
		@FormDataParam("data") InputStream customerData, @FormDataParam("data") FormDataContentDisposition customerDataDetails,
		@FormDataParam("businessId") Integer businessId, @FormDataParam("month") Integer month, @FormDataParam("year") Integer year, @FormDataParam("customerServiceCharge") BigDecimal serviceCharge) {
		BillServiceResponse response = new BillServiceResponse();
		if(customerData != null) {
			BillFile file = new BillFile();
			file.setFileData(customerData);
			file.setFileSize(new BigDecimal((customerDataDetails.getSize())));
			file.setFileType(customerDataDetails.getType());
			file.setFilePath(customerDataDetails.getFileName());
			BillServiceRequest request = new BillServiceRequest();
			request.setFile(file);
			BillBusiness business = new BillBusiness();
			business.setId(businessId);
			request.setBusiness(business);
			if(month != null && year != null) {
				BillInvoice invoice = new BillInvoice();
				invoice.setYear(year);
				invoice.setMonth(month);
				invoice.setServiceCharge(serviceCharge);
				request.setInvoice(invoice);
			}
			response = adminBo.uploadVendorData(request);
		}
		return response;
	}
	
	@POST
	@Path("/generateInvoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse generateInvoices(BillServiceRequest request) {
		return adminBo.generateBills(request);
	}
	
	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse login(BillServiceRequest request) {
		return adminBo.login(request);
	}
	
	@POST
	@Path("/dashboardSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse dashboardSummary(BillServiceRequest request) {
		return adminBo.getSummary(request);
	}
	
	@POST
	@Path("/getAllVendors")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllVendors(BillServiceRequest request) {
		return adminBo.getAllVendors(request);
	}
	
	@POST
	@Path("/updateBusinessInfo")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessInfo(@FormDataParam("logo") InputStream logoFile,
			@FormDataParam("logo") FormDataContentDisposition logoFileDetails, @FormDataParam("business") String user, @FormDataParam("items") String items) {
		LoggingUtil.logObject("Business/user update request", user);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			BillUser billUser = mapper.readValue(user, BillUser.class);
			if (billUser != null) {
				if (logoFile != null) {
					BillFile file = new BillFile();
					file.setFileData(logoFile);
					file.setFileSize(new BigDecimal((logoFileDetails.getSize())));
					file.setFileType(logoFileDetails.getType());
					file.setFilePath(logoFileDetails.getFileName());
					billUser.getCurrentBusiness().setLogo(file);
				}
				BillServiceRequest request = new BillServiceRequest();
				request.setUser(billUser);
				response = userBo.updateUserInfo(request);
				if(StringUtils.isNotBlank(items)) {
					List<BillItem> itemList = mapper.readValue(items, new TypeReference<List<BillItem>>(){});
					request.setItems(itemList);
					request.setBusiness(request.getUser().getCurrentBusiness());
					if(CollectionUtils.isNotEmpty(itemList) && request.getUser().getCurrentBusiness() != null && request.getUser().getCurrentBusiness().getId() != null) {
						userBo.updateBusinessItem(request);
					}
				}
				
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
		LoggingUtil.logObject("Business/user update response", response);
		return response;
	}
	
	@POST
	@Path("/downloadTransferExcel")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/vnd.ms-excel")
	public Response downloadTransferExcel(BillServiceRequest request) {
		try {
			BillFile file = adminBo.getSettlements(request).getFile();
			XSSFWorkbook wb = (XSSFWorkbook) file.getWb();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			wb.write(os);
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			ResponseBuilder response = Response.ok(is);
			response.header("Content-Disposition","attachment; filename=" + file.getFileName());  
			return response.build();
		} catch (Exception e) {
			LoggingUtil.logError("Error in download data - " + e);
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return Response.serverError().build();
	}
	
	@POST
	@Path("/getSettlements")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getSettlements(BillServiceRequest request) {
		return adminBo.getSettlements(request);
	}
	
	@POST
	@Path("/updateOrders")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateParentTemp(BillServiceRequest request) {
		return adminBo.updateOrders(request);
	}
	
	@POST
	@Path("/getTransactions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getTransactions(BillServiceRequest request) {
		return adminBo.getTransactions(request);
	}
	
	@POST
	@Path("/updateLocation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateLocation(BillServiceRequest request) {
		return adminBo.updateLocations(request);
	}
	
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse notify(BillServiceRequest request) {
		return adminBo.sendNotifications(request);
	}
	
}
