package com.rns.web.billapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ccavenue.security.AesCryptUtil;
import com.rns.web.billapp.service.bo.api.BillUserBo;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillPaymentUtil;
import com.rns.web.billapp.service.util.BillPropertyUtil;
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
	public BillServiceResponse updateUserBasicInfo(@FormDataParam("pan") InputStream panCardFile,
			@FormDataParam("pan") FormDataContentDisposition panCardFileDetails, @FormDataParam("aadhar") InputStream aadharCardFile,
			@FormDataParam("aadhar") FormDataContentDisposition aadharCardFileDetails, @FormDataParam("user") String user) {
		LoggingUtil.logObject("User update request", user);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			BillUser billUser = mapper.readValue(user, BillUser.class);
			if (billUser != null) {
				if (panCardFile != null) {
					BillFile file = new BillFile();
					file.setFileData(panCardFile);
					file.setFileSize(new BigDecimal((panCardFileDetails.getSize())));
					file.setFileType(panCardFileDetails.getType());
					file.setFilePath(panCardFileDetails.getFileName());
					billUser.setPanFile(file);
				}
				if (aadharCardFile != null) {
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
	public BillServiceResponse loadProfile(BillServiceRequest request) {
		return userBo.loadProfile(request);
	}

	@POST
	@Path("/getAllAreas")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAreas(BillServiceRequest request) {
		return userBo.getAllAreas();
	}
	
	@POST
	@Path("/getAllSectors")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllSectors(BillServiceRequest request) {
		return userBo.getAllSectors();
	}

	@POST
	@Path("/updateUserProfile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateUserProfile(BillServiceRequest request) {
		return userBo.updateUserInfo(request);
	}
	
	/*@POST
	@Path("/updateBusinessLogo")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessLogo(@FormParam("logo") String logoFile,
			@FormParam("fileName") String logoFileDetails, @FormParam("user") String user) {
		LoggingUtil.logObject("Logo update request", user);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			byte byteArray[] = Base64.decodeBase64(logoFile);
			BillUser billUser = mapper.readValue(user, BillUser.class);
			if (billUser != null && billUser.getCurrentBusiness() != null) {
				if (logoFile != null) {
					BillFile file = new BillFile();
					file.setFileData(new ByteArrayInputStream(byteArray));
					//file.setFileData(logoFile);
					file.setFilePath(logoFileDetails);
					billUser.getCurrentBusiness().setLogo(file);
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
		LoggingUtil.logObject("Logo update response", response);
		return response;
	}*/
	
	@POST
	@Path("/updateBusinessLogo")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessLogo(@FormDataParam("logo") InputStream logoFile,
			@FormDataParam("logo") FormDataContentDisposition logoFileDetails,@FormDataParam("user") String user) {
		LoggingUtil.logObject("Logo update request", user);
		ObjectMapper mapper = new ObjectMapper();
		BillServiceResponse response = new BillServiceResponse();
		try {
			BillUser billUser = mapper.readValue(user, BillUser.class);
			if (billUser != null && billUser.getCurrentBusiness() != null) {
				if (logoFile != null) {
					BillFile file = new BillFile();
					//file.setFileData(new ByteArrayInputStream(byteArray));
					file.setFileData(logoFile);
					file.setFilePath(logoFileDetails.getFileName());
					billUser.getCurrentBusiness().setLogo(file);
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
		LoggingUtil.logObject("Logo update response", response);
		return response;
	}


	@POST
	@Path("/updateBankDetails")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBankDetails(BillServiceRequest request) {
		return userBo.updateUserFinancialInfo(request);
	}

	@POST
	@Path("/loadBusinessItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse loadBusinessItems(BillServiceRequest request) {
		return userBo.getBusinessItems(request);
	}

	@POST
	@Path("/loadSectorItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse loadSectorItems(BillServiceRequest request) {
		return userBo.getSectorItems(request);
	}

	@POST
	@Path("/updateBusinessItem")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessItem(BillServiceRequest request) {
		return userBo.updateBusinessItem(request);
	}

	@POST
	@Path("/getAllCustomers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getAllCustomers(BillServiceRequest request) {
		return userBo.getAllBusinessCustomers(request);
	}

	@POST
	@Path("/updateCustomer")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomer(BillServiceRequest request) {
		return userBo.updateCustomerInfo(request);
	}

	@POST
	@Path("/getCustomerProfile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerProfile(BillServiceRequest request) {
		return userBo.getCustomerProfile(request);
	}

	@POST
	@Path("/updateCustomerItem")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerItem(BillServiceRequest request) {
		return userBo.updateCustomerItem(request);
	}

	@POST
	@Path("/updateCustomerItemTemp")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerItemTemp(BillServiceRequest request) {
		return userBo.updateCustomerItemTemporary(request);
	}

	@POST
	@Path("/getCustomerInvoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerInvoices(BillServiceRequest request) {
		return userBo.getCustomerInvoices(request);
	}

	@POST
	@Path("/updateCustomerInvoice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerInvoice(BillServiceRequest request) {
		return userBo.updateCustomerInvoice(request);
	}
	
	@POST
	@Path("/updateInvoiceItems")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateInvoiceItems(BillServiceRequest request) {
		return userBo.updateInvoiceItems(request);
	}

	@POST
	@Path("/sendInvoice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse sendInvoice(BillServiceRequest request) {
		return userBo.sendCustomerInvoice(request);
	}

	// {amount=[105.00], fees=[2.00], shorturl=[], purpose=[January 2018 monthly
	// payment], buyer_phone=[+911122112211], buyer_name=[Anand],
	// payment_request_id=[1337c3d8fcc94be992c70827bc052461],
	// mac=[9ae3e3e13488a282b4f6322b0a9d5ae0d883c0fb],
	// buyer=[ajinkyashiva@gmail.com], payment_id=[MOJO8603005A85529733],
	// longurl=[https://test.instamojo.com/@crkstructural/1337c3d8fcc94be992c70827bc052461],
	// currency=[INR], status=[Credit]}
	@POST
	@Path("/paymentResult")
	// @Produces(MediaType.APPLICATION_JSON)
	public Response paymentResult(MultivaluedMap<String, String> formParams) {
		URI url = null;
		try {
			LoggingUtil.logMessage("Payment result -- " + formParams);
			BillInvoice invoice = new BillInvoice();
			invoice.setPaymentRequestId(formParams.get("payment_request_id").get(0).toString());
			invoice.setPaymentId(formParams.get("payment_id").get(0).toString());
			invoice.setStatus(formParams.get("status").get(0).toString());
			invoice.setAmount(new BigDecimal(formParams.get("amount").get(0).toString()));
			invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_INSTA);
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			url = new URI(response.getInvoice().getPaymentUrl());
		} catch (URISyntaxException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} 

		return Response.temporaryRedirect(url).build();
	}

	@POST
	@Path("/getDeliveries")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getDeliveries(BillServiceRequest request) {
		return userBo.loadDeliveries(request);
	}

	@POST
	@Path("/getOrderSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getOrderSummary(BillServiceRequest request) {
		return userBo.getDailySummary(request);
	}

	@POST
	@Path("/getInvoiceSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getInvoiceSummary(BillServiceRequest request) {
		return userBo.getInvoiceSummary(request);
	}

	@POST
	@Path("/getCustomerActivity")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerActivity(BillServiceRequest request) {
		return userBo.getCustomerActivity(request);
	}

	@POST
	@Path("/hdfc/paymentResult")
	// @Produces(MediaType.APPLICATION_JSON)
	public Response hdfcPaymentResult(MultivaluedMap<String, String> formParams) {
		URI url = null;
		try {
			LoggingUtil.logMessage("Payment result -- " + formParams);
			AesCryptUtil aesUtil = new AesCryptUtil(BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_KEY));
			String decResp = aesUtil.decrypt(formParams.getFirst("encResp"));
			StringTokenizer tokenizer = new StringTokenizer(decResp, "&");
			String pair = null, pname = null, pvalue = null;
			Map<String, String> responseMap = new HashMap<String, String>();
			while (tokenizer.hasMoreTokens()) {
				pair = (String) tokenizer.nextToken();
				if (pair != null) {
					StringTokenizer strTok = new StringTokenizer(pair, "=");
					pname = "";
					pvalue = "";
					if (strTok.hasMoreTokens()) {
						pname = (String) strTok.nextToken();
						if (strTok.hasMoreTokens()) {
							pvalue = (String) strTok.nextToken();
						}
						LoggingUtil.logMessage("HDFC payment result parameter - " + pname + " -- " + pvalue);
						responseMap.put(pname , pvalue);
					}
					
				}
			}
			BillInvoice invoice = new BillInvoice();
			invoice.setId(new Integer(responseMap.get("order_id")));
			invoice.setPaymentRequestId(responseMap.get("bank_ref_no"));
			invoice.setPaymentId(responseMap.get("tracking_id"));
			invoice.setStatus(responseMap.get("order_status"));
			invoice.setAmount(new BigDecimal(responseMap.get("amount")));
			invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_HDFC);
			invoice.setPaymentMode(responseMap.get("payment_mode"));
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			LoggingUtil.logMessage("Redirect after payment to --" + response.getInvoice().getPaymentUrl());
			url = new URI(response.getInvoice().getPaymentUrl());
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return Response.temporaryRedirect(url).build();
	}
	
	//{date=[Thu Aug 23 02:05:39 IST 2018], surcharge=[0.00], CardNumber=[null], 
	//prod=[Multi], clientcode=[170000117101490], mmp_txn=[100001662544], 
	//signature=[e3d51f151f4d4cc926f16e123f6a8901ab3437c71edfc024bf681a02a19e788fc8e44c96fd06b0b693b876882e3c72d5ea95c12b78a22a5348e8fbd7c7bb7513],
	//udf5=[null], amt=[210.00], udf6=[null], udf3=[null], merchant_id=[231], udf4=[null], udf1=[null], udf2=[null], 
	//discriminator=[NB], mer_txn=[11], f_code=[Ok], bank_txn=[1000016625441], udf9=[null], bank_name=[Atom Bank]}
	@POST
	@Path("/atom/paymentResult")
	// @Produces(MediaType.APPLICATION_JSON)
	public Response atomPaymentResult(MultivaluedMap<String, String> formParams) {
		URI url = null;
		try {
			
			LoggingUtil.logMessage("ATOM Payment result -- " + formParams);
			
			BillInvoice invoice = new BillInvoice();
			invoice.setId(new Integer(formParams.getFirst("mer_txn")));
			invoice.setPaymentRequestId(formParams.getFirst("bank_txn"));
			invoice.setPaymentId(formParams.getFirst("mmp_txn"));
			invoice.setStatus(formParams.getFirst("f_code"));
			invoice.setAmount(new BigDecimal(formParams.getFirst("amt")));
			invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_ATOM);
			invoice.setPaymentMode(formParams.getFirst("discriminator"));
			String signature = BillPaymentUtil.getResponseHash(invoice, formParams.getFirst("prod"));
			if(!StringUtils.equals(signature, formParams.getFirst("signature"))) {
				/*url = new URI(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + "Failed") ;
				return Response.temporaryRedirect(url).build();*/
				invoice.setStatus(BillConstants.INVOICE_STATUS_FAILED);
				invoice.setComments("Signature not matched");
			}
			invoice.setPaymentResponse(formParams.toString());
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			LoggingUtil.logMessage("Redirect after payment to --" + response.getInvoice().getPaymentUrl());
			url = new URI(response.getInvoice().getPaymentUrl());
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return Response.temporaryRedirect(url).build();
	}
	
	@POST
	@Path("/updatePaymentCredentials")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateUserCredentials(BillServiceRequest request) {
		return userBo.updatePaymentCredentials(request);
	}
	
	@POST
	@Path("/cashfree/paymentResult")
	// @Produces(MediaType.APPLICATION_JSON)
	public Response cashfreePaymentResult(MultivaluedMap<String, String> formParams) {
		URI url = null;
		try {
			
			LoggingUtil.logMessage("Cashfree Payment result -- " + formParams);
			
			BillInvoice invoice = new BillInvoice();
			String orderId = formParams.getFirst("orderId");
			String[] split = StringUtils.split(orderId, BillPaymentUtil.TXID_SEPARATOR);
			invoice.setPaymentRequestId(formParams.getFirst("txTime"));
			invoice.setTxTime(formParams.getFirst("txTime"));
			invoice.setPaymentId(formParams.getFirst("referenceId"));
			invoice.setStatus(formParams.getFirst("txStatus"));
			invoice.setAmount(new BigDecimal(formParams.getFirst("orderAmount")));
			invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_CASHFREE);
			invoice.setPaymentMode(formParams.getFirst("paymentMode"));
			invoice.setCashfreeSignature(formParams.getFirst("signature"));
			invoice.setComments(formParams.getFirst("txMsg"));
			if(!BillPaymentUtil.verifyCashfreeSignature(invoice, orderId)) {
				/*url = new URI(BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_RESULT) + "Failed") ;
				return Response.temporaryRedirect(url).build();*/
				invoice.setStatus(BillConstants.INVOICE_STATUS_FAILED);
				invoice.setComments("Signature not matched");
				LoggingUtil.logMessage("Signature not matched for " + orderId);
			}
			invoice.setId(new Integer(split[0]));
			invoice.setPaymentResponse(formParams.toString());
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			if(response.getInvoice() != null) {
				LoggingUtil.logMessage("Redirect after payment to --" + response.getInvoice().getPaymentUrl());
				url = new URI(response.getInvoice().getPaymentUrl());
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return Response.temporaryRedirect(url).build();
	}
	
	@POST
	@Path("/paytm/paymentResult")
	// @Produces(MediaType.APPLICATION_JSON)
	public Response paytmPaymentResult(MultivaluedMap<String, String> formParams) {
		URI url = null;
		try {
			
			LoggingUtil.logMessage("PayTm Payment result -- " + formParams);
			
			BillInvoice invoice = new BillInvoice();
			String orderId = formParams.getFirst("ORDERID");
			//invoice.setId(new Integer(orderId));
			String[] split = StringUtils.split(orderId, BillPaymentUtil.TXID_SEPARATOR);
			invoice.setId(new Integer(split[0]));
			invoice.setTxTime(formParams.getFirst("TXNDATE"));
			invoice.setPaymentId(formParams.getFirst("TXNID"));
			invoice.setStatus(formParams.getFirst("STATUS"));
			if(StringUtils.equals("TXN_SUCCESS", invoice.getStatus())) {
				invoice.setStatus("Success");
			}
			invoice.setAmount(new BigDecimal(formParams.getFirst("TXNAMOUNT")));
			invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_PAYTM);
			invoice.setPaymentMode(formParams.getFirst("PAYMENTMODE"));
			if(StringUtils.equalsIgnoreCase("PPI", invoice.getPaymentMode())) {
				invoice.setPaymentMode("Wallet");
			}
			invoice.setComments(formParams.getFirst("RESPMSG"));
			invoice.setPaymentRequestId(formParams.getFirst("BANKTXNID"));
			invoice.setPaymentResponse(formParams.toString());
			if(!BillPaymentUtil.matchPayTmChecksum(formParams)) {
				invoice.setStatus(BillConstants.INVOICE_STATUS_FAILED);
				invoice.setComments("Signature not matched");
				LoggingUtil.logMessage("Signature not matched for " + orderId);
			}
			BillServiceRequest request = new BillServiceRequest();
			request.setInvoice(invoice);
			BillServiceResponse response = userBo.completePayment(request);
			if(response.getInvoice() != null) {
				LoggingUtil.logMessage("Redirect after payment to --" + response.getInvoice().getPaymentUrl());
				url = new URI(response.getInvoice().getPaymentUrl());
			}
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return Response.temporaryRedirect(url).build();
	}
	
	@GET
	@Path("/getImage/{type}/{id}")
	@Produces(MediaType.MULTIPART_FORM_DATA)
	public Response getImage(@PathParam("type") String type, @PathParam("id") Integer id) {
		//LoggingUtil.logObject("Image request:", userId);
		try {
			BillServiceRequest request = new BillServiceRequest();
			if(StringUtils.equals("logo", type)) {
				BillBusiness business = new BillBusiness();
				business.setId(id);
				request.setBusiness(business);
				request.setRequestType(type);
			}
			BillServiceResponse sResponse = userBo.getFile(request);
			ResponseBuilder response = null;
			if(sResponse.getFile() != null) {
				response = Response.ok(sResponse.getFile().getFileData());
				if(sResponse.getFile().getFileName() != null) {
					response.header("Content-Disposition", "filename=" + sResponse.getFile().getFileName());
				} else {
					response.header("Content-Disposition", "filename=" + "image.png");
				}
			}
			return response.build();

		} catch (Exception e) {
			e.printStackTrace();
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}
	
	@POST
	@Path("/getTransactions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getTransactions(BillServiceRequest request) {
		return userBo.getTransactions(request);
	}
	
	@POST
	@Path("/getBillSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getBillSummary(BillServiceRequest request) {
		return userBo.getCustomerBillsSummary(request);
	}
	
	@POST
	@Path("/updateBusinessItemImage")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessItemImage(
		@FormDataParam("image") InputStream image, @FormDataParam("image") FormDataContentDisposition imageFileDetails,
		@FormDataParam("item") String item, @FormDataParam("businessId") Integer businessId) {
		LoggingUtil.logObject("Business Item update request", item);
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
				List<BillItem> items = new ArrayList<BillItem>();
				items.add(billItem);
				request.setItems(items);
				if(businessId != null) {
					BillBusiness business = new BillBusiness();
					business.setId(businessId);
					request.setBusiness(business);
				}
				response = userBo.updateBusinessItem(request);
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
	
	@POST
	@Path("/updateCustomerGroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateCustomerGroup(BillServiceRequest request) {
		return userBo.updateCustomerGroup(request);
	}
	
	@POST
	@Path("/getCustomerGroups")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getCustomerGroups(BillServiceRequest request) {
		return userBo.getAllCustomerGroups(request);
	}
	
	@POST
	@Path("/updateGroupCustomers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateGroupCustomers(BillServiceRequest request) {
		return userBo.updateGroupCustomers(request);
	}
	
	@POST
	@Path("/getPaymentSummary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getPaymentSummary(BillServiceRequest request) {
		return userBo.getPaymentsReport(request);
	}
	
	@POST
	@Path("/getDistributors")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getDistributors(BillServiceRequest request) {
		return userBo.getBusinessesByType(request);
	}
	
	@POST
	@Path("/getBusinessInvoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getBusinessInvoices(BillServiceRequest request) {
		return userBo.getBusinessInvoicesForBusiness(request);
	}
	
	@POST
	@Path("/getBusinessItemsByDate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse getBusinessItemsByDate(BillServiceRequest request) {
		return userBo.getBusinessItemsByDate(request);
	}
	
	@POST
	@Path("/updateBusinessInvoice")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BillServiceResponse updateBusinessInvoice(BillServiceRequest request) {
		return userBo.updateBusinessInvoice(request);
	}
}
