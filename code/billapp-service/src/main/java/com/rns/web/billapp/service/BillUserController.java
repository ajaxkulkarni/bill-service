package com.rns.web.billapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
	
	
}
