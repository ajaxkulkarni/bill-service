package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BillPaymentUtil {

	private static final String COMMON_KEY = "bill_vendor";
	private static final String AUTHORIZATION_HEADER = "Authorization";

	public static BillPaymentCredentials createNewUser(BillUser user, String token) {

		try {
			ClientConfig config = new DefaultClientConfig();
			config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			Client client = Client.create(config);

			String url = BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_URL) + "v2/users/";
			WebResource webResource = client.resource(url);

			LoggingUtil.logMessage("Calling create insta user URL ==>" + url);
			LoggingUtil.logMessage("Calling create insta user request:" + user.getEmail());

			MultivaluedMap<String, String> request = new MultivaluedMapImpl();
			request.add("email", user.getEmail());
			request.add("password", COMMON_KEY);
			request.add("phone", user.getPhone());
			request.add("referrer", BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_REFERRER));
			BillPaymentCredentials clientCredentials = getToken(null, null);
			ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).header(AUTHORIZATION_HEADER, "Bearer " + clientCredentials.getAccess_token()).post(ClientResponse.class, request);

			String entity = response.getEntity(String.class);
			LoggingUtil.logMessage("Output from create insta user URL ...." + response.getStatus() + " RESP:" + entity + " \n");

			if (response.getStatus() != 200 && response.getStatus() != 201) {
				LoggingUtil.logMessage("Create payment user failed : HTTP error code : " + response.getStatus() + " RESP:" + entity);
				return null;
			}
			JsonNode node = new ObjectMapper().readTree(new StringReader(entity));
			BillPaymentCredentials credentials = new BillPaymentCredentials();
			credentials.setInstaId(node.get("id").getTextValue());
			BillPaymentCredentials userToken = getToken(null, node.get("username").getTextValue());
			credentials.setRefresh_token(userToken.getRefresh_token());
			credentials.setAccess_token(userToken.getAccess_token());
			return credentials;
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

		return null;

	}

	public static BillPaymentCredentials getToken(String token, String name) throws JsonParseException, JsonMappingException, IOException {
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(config);
		client.addFilter(new LoggingFilter(System.out));

		MultivaluedMap<String, String> request = new MultivaluedMapImpl();
		request.add("client_id", BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_CLIENT_ID));
		request.add("client_secret", BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_CLIENT_SECRET));

		String url = BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_URL) + "oauth2/token/";
		WebResource webResource = client.resource(url);

		if (StringUtils.isNotBlank(token)) {
			request.add("refresh_token", token);
			request.add("grant_type", "refresh_token");
		} else if(StringUtils.isNotBlank(name)) {
			request.add("username", name);
			request.add("password", COMMON_KEY);
			request.add("grant_type", "password");
		} else {
			request.add("grant_type", "client_credentials");
		}

		ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, request);

		String entity = response.getEntity(String.class);
		LoggingUtil.logMessage("Output from Access token ...." + response.getStatus() + " RESP:" + entity + " \n");

		if (response.getStatus() != 200) {
			LoggingUtil.logMessage("Access token request failed : HTTP error code : " + response.getStatus() + " RESP:" + entity);
			return null;
		}

		return new ObjectMapper().readValue(entity, BillPaymentCredentials.class);
	}

	public static BillPaymentCredentials updateBankDetails(BillFinancialDetails details, BillPaymentCredentials credentials, boolean retry) {

		try {
			ClientConfig config = new DefaultClientConfig();
			config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			Client client = Client.create(config);

			String url = BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_URL) + "v2/users/" + credentials.getInstaId() + "/inrbankaccount";
			WebResource webResource = client.resource(url);

			LoggingUtil.logMessage("Calling bank details URL ==>" + url);
			LoggingUtil.logMessage("Calling bank details URL with request:" + details.getAccountNumber());

			MultivaluedMap<String, String> request = new MultivaluedMapImpl();
			request.add("account_holder_name", details.getAccountHolderName());
			request.add("account_number", details.getAccountNumber());
			request.add("ifsc_code", details.getIfscCode());

			ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).header(AUTHORIZATION_HEADER, "Bearer " + credentials.getAccess_token())
					.put(ClientResponse.class, request);

			String entity = response.getEntity(String.class);
			LoggingUtil.logMessage("Output from Bank details URL ...." + response.getStatus() + " RESP:" + entity + " \n");
			
			if(response.getStatus() == 401 && retry) {
				credentials = getToken(credentials.getRefresh_token(), null);
				LoggingUtil.logMessage("Got new token --" + credentials.getAccess_token());
				return updateBankDetails(details, credentials, false);
			}

		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return credentials;
	}

	public static BillPaymentCredentials createPaymentRequest(BillUser customer, BillPaymentCredentials credentials, BillInvoice invoice, boolean retry) throws JsonParseException, JsonMappingException, IOException {
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(config);
		client.addFilter(new LoggingFilter(System.out));
		
		String url = BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_URL) + "v2/payment_requests/";
		WebResource webResource = client.resource(url);

		LoggingUtil.logMessage("Calling payment request URL ==>" + url);
		LoggingUtil.logMessage("Calling payment request with request:" + customer.getEmail() + " and token - " + credentials.getAccess_token());

		MultivaluedMap<String, String> request = new MultivaluedMapImpl();
		request.add("amount", invoice.getPayable().toString());
		request.add("purpose", invoicePurpose(invoice));
		request.add("buyer_name", customer.getName());
		request.add("email", customer.getEmail());
		request.add("phone", customer.getPhone());
		//request.add("redirect_url", customer.getName());
		request.add("webhook", BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_WEBHOOK));
		request.add("partner_fee_type", "fixed");
		request.add("partner_fee", "2.00");
		

		ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).header(AUTHORIZATION_HEADER, "Bearer " + credentials.getAccess_token())
				.post(ClientResponse.class, request);

		String entity = response.getEntity(String.class);
		LoggingUtil.logMessage("Output from Payment request URL ...." + response.getStatus() + " RESP:" + entity + " \n");
		
		if(response.getStatus() == 401 && retry) {
			credentials = getToken(credentials.getRefresh_token(), null);
			LoggingUtil.logMessage("Got new token --" + credentials.getAccess_token());
			return createPaymentRequest(customer, credentials, invoice, false);
		}
		JsonNode node = new ObjectMapper().readTree(new StringReader(entity));
		credentials.setLongUrl(node.get("longurl").getTextValue());
		credentials.setPaymentRequestId(node.get("id").getTextValue());
		return credentials;
	}

	public static String invoicePurpose(BillInvoice invoice) {
		return BillConstants.MONTHS[invoice.getMonth() - 1] + " " + invoice.getYear() + " monthly payment";
	}

}
