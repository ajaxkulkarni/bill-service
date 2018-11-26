package com.rns.web.billapp.service.util;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.ccavenue.security.AesCryptUtil;
import com.paytm.pg.merchant.CheckSumServiceHelper;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillPaymentCredentials;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BillPaymentUtil {

	public static final String TXID_SEPARATOR = ".";
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
			ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
					.header(AUTHORIZATION_HEADER, "Bearer " + clientCredentials.getAccess_token()).post(ClientResponse.class, request);

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
		} else if (StringUtils.isNotBlank(name)) {
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

			ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
					.header(AUTHORIZATION_HEADER, "Bearer " + credentials.getAccess_token()).put(ClientResponse.class, request);

			String entity = response.getEntity(String.class);
			LoggingUtil.logMessage("Output from Bank details URL ...." + response.getStatus() + " RESP:" + entity + " \n");

			if (response.getStatus() == 401 && retry) {
				credentials = getToken(credentials.getRefresh_token(), null);
				LoggingUtil.logMessage("Got new token --" + credentials.getAccess_token());
				return updateBankDetails(details, credentials, false);
			}

		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return credentials;
	}

	public static BillPaymentCredentials createPaymentRequest(BillUser customer, BillPaymentCredentials credentials, BillInvoice invoice, boolean retry)
			throws JsonParseException, JsonMappingException, IOException {
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
		// request.add("redirect_url", customer.getName());
		request.add("webhook", BillPropertyUtil.getProperty(BillPropertyUtil.PAYMENT_WEBHOOK));
		// BigDecimal internetHandlingFees = invoice.getPayable().multiply(new
		// BigDecimal(BillConstants.PAYMENT_CHARGE_PERCENT), new MathContext(2,
		// RoundingMode.HALF_UP));
		// request.add("partner_fee_type", "fixed");
		// request.add("partner_fee", "0"); //TODO change later

		// LoggingUtil.logMessage("Partner commission is ==>" +
		// internetHandlingFees.negate().toString());

		ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
				.header(AUTHORIZATION_HEADER, "Bearer " + credentials.getAccess_token()).post(ClientResponse.class, request);

		String entity = response.getEntity(String.class);
		LoggingUtil.logMessage("Output from Payment request URL ...." + response.getStatus() + " RESP:" + entity + " \n");

		if (response.getStatus() == 401 && retry) {
			credentials = getToken(credentials.getRefresh_token(), null);
			if (credentials == null) {
				return null;
			}
			LoggingUtil.logMessage("Got new token --" + credentials.getAccess_token());
			return createPaymentRequest(customer, credentials, invoice, false);
		}
		JsonNode node = new ObjectMapper().readTree(new StringReader(entity));
		if (node != null && node.get("longurl") != null) {
			credentials.setLongUrl(node.get("longurl").getTextValue());
			credentials.setPaymentRequestId(node.get("id").getTextValue());
		}
		return credentials;
	}

	public static String invoicePurpose(BillInvoice invoice) {
		if(invoice.getMonth() == null) {
			return "";
		}
		return BillConstants.MONTHS[invoice.getMonth() - 1] + " " + invoice.getYear() + " monthly payment";
	}

	public static void prepareHdfcRequest(BillInvoice invoice, BillUser customer) {
		AesCryptUtil crypt = new AesCryptUtil(BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_KEY));
		invoice.setHdfcRequest(crypt.encrypt("tid=" + invoice.getId() + "&merchant_id=" + BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_MERCHANT_ID)
				+ "&order_id=" + invoice.getId() + "&currency=INR" + "&amount=" + invoice.getPayable() + "&redirect_url="
				+ BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_PAYMENT_RESULT) + "&cancel_url="
				+ BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_PAYMENT_RESULT) + "&language=EN" + "&billing_name=" + customer.getName()
				+ "&billing_address=" + customer.getAddress() + "&billing_city=Pune" + "&billing_state=Maharashtra"
				/* + "&billing_zip=" */
				+ "&billing_country=India" + "&billing_tel=" + customer.getPhone() + "&billing_email=" + customer.getEmail() + "&delivery_name="
				+ customer.getName() + "&delivery_address=" + customer.getAddress() + "&delivery_city=Pune" + "&delivery_state=Maharashtra"
				/* + "&billing_zip=" */
				+ "&delivery_country=India" + "&delivery_tel=" + customer.getPhone() + "&delivery_email=" + customer.getEmail()));

		invoice.setHdfcAccessCode(BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_ACCESS_CODE));
		invoice.setHdfcPaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_URL));
	}

	// https://paynetzuat.atomtech.in/paynetz/epi/fts
	// ?login=231&pass=Test@123&ttype=NBFundTransfer&prodid=Multi&amt=1000.00
	// &txncurr=INR&txnscamt=0&clientcode=MTcwMDAwMTE3MTAxNDkw&udf2=abc@gmail.com
	// &udf5=231&txnid=1234&date=13/02/2018&custacc=1234567890
	// &ru=https://paynetzuat.atomtech.in/paynetzclient/ResponseParam.jsp
	// &signature=e7415bcbbccaa2f1ccd38736d0233876a459f1d489c2231e8cb1935e58990e6112c5e377f559b255526e0d9b5467788f5c4c2fa8cc308fc06e9f2ef5ff367376
	// &mprod=<products><product><id>1</id><name>ONE</name><amount>250.00</amount></product><product><id>2</id><name>TWO</name><amount>250.00</amount></product><product><id>3</id><name>THREE</name><amount>500.00</amount></product></products>

	public static void prepareAtomRequest(BillInvoice invoice, BillDBUser vendor) {
		String login = BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_LOGIN);
		String pass = BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_PASSWORD);
		;
		String ttype = "NBFundTransfer";
		String prodid = BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_PRODUCT_ID);
		// String prodid = "Multi";

		String txnid = invoice.getId().toString();
		String amt = invoice.getPayable().toString();
		String txncurr = "INR";
		String reqHashKey = BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_REQUEST_HASH);
		;
		// login,pass,ttype,prodid,txnid,amt,txncurr
		String signature_request = getEncodedValueWithSha2(reqHashKey, login, pass, ttype, prodid, txnid, amt, txncurr);
		System.out.println("Request signature ::" + signature_request);

		StringBuilder builder = new StringBuilder();
		builder.append(BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_PAYMENT_URL))
				.append("?").append("login=").append(login)
				.append("&").append("pass=").append(pass).append("&")
				.append("ttype=").append(ttype).append("&")
				.append("prodid=").append(BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_PRODUCT_ID)).append("&")
				.append("amt=").append(amt).append("&")
				.append("txncurr=").append(txncurr).append("&")
				.append("txnscamt=").append("0").append("&")
				.append("custacc=").append("1234567890").append("&")
				.append("clientcode=").append(BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_CLIENT_CODE)).append("&")
				.append("txnid=").append(txnid).append("&")
				.append("date=").append(CommonUtils.convertDate(new Date(), "dd/MM/yyyy"))
				.append("&").append("ru=").append(/* "https://paynetzuat.atomtech.in/paynetzclient/ResponseParam.jsp" */BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_REDIRECT_URL))
				.append("&").append("signature=").append(signature_request);
		// .append("&").append("mprod=").append(getProducts(vendor.getEmail(),
		// amt));
		
		LoggingUtil.logMessage("Atom payment URL == " + builder.toString() + " PROD ID - " + BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_PRODUCT_ID));
		invoice.setAtomPaymentUrl(builder.toString());
	}

	private static String getProducts(String vendorEmail, String amount) {
		StringBuilder builder = new StringBuilder();
		builder.append("<products>");
		builder.append(getProduct("ONE", "0", "1"));
		builder.append(getProduct("TWO", amount, "2"));
		builder.append("</products>");
		return builder.toString();
	}

	private static String getProduct(String vendorEmail, String amount, String productId) {
		return "<product><id>" + productId + "</id><name>" + vendorEmail + "</name><amount>" + amount + "</amount></product>";
	}

	public static String getResponseHash(BillInvoice invoice, String prodId) {
		// Response signature based on parameters

		String mmp_txn = invoice.getPaymentId();
		String mer_txn = invoice.getId().toString();
		String f_code = invoice.getStatus();
		String prod = prodId;
		String discriminator = invoice.getPaymentMode();
		String amt = invoice.getAmount().toString();
		String bank_txn = invoice.getPaymentRequestId();
		String respHashKey = BillPropertyUtil.getProperty(BillPropertyUtil.ATOM_RESPONSE_HASH);
		// mmp_txn,mer_txn, f_code, prod, discriminator, amt, bank_txn
		String signature_response = getEncodedValueWithSha2(respHashKey, mmp_txn, mer_txn, f_code, prod, discriminator, amt, bank_txn);
		System.out.println("Response signature ::" + signature_response);
		return signature_response;
	}

	public static byte[] encodeWithHMACSHA2(String text, String keyString)
			throws java.security.NoSuchAlgorithmException, java.security.InvalidKeyException, java.io.UnsupportedEncodingException {

		java.security.Key sk = new javax.crypto.spec.SecretKeySpec(keyString.getBytes("UTF-8"), "HMACSHA512");
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance(sk.getAlgorithm());
		mac.init(sk);

		byte[] hmac = mac.doFinal(text.getBytes("UTF-8"));

		return hmac;
	}

	/*
	 * Convert from byte array to HexString
	 */
	public static String byteToHexString(byte byData[]) {
		StringBuilder sb = new StringBuilder(byData.length * 2);

		for (int i = 0; i < byData.length; i++) {
			int v = byData[i] & 0xff;
			if (v < 16)
				sb.append('0');
			sb.append(Integer.toHexString(v));
		}

		return sb.toString();
	}

	/*
	 * Encoded with HMACSHA512 and encoded with utf-8 using url encoder for
	 * given list of parameter values appended with the key
	 */
	public static String getEncodedValueWithSha2(String hashKey, String... param) {
		String resp = null;

		StringBuilder sb = new StringBuilder();
		for (String s : param) {
			sb.append(s);
		}

		try {
			System.out.println("[getEncodedValueWithSha2]String to Encode =" + sb.toString());
			resp = byteToHexString(encodeWithHMACSHA2(sb.toString(), hashKey));
			// resp = URLEncoder.encode(resp,"UTF-8");

		} catch (Exception e) {
			System.out.println("[getEncodedValueWithSha2]Unable to encocd value with key :" + hashKey + " and input :" + sb.toString());
			e.printStackTrace();
		}

		return resp;
	}

	public static void prepareCashFreeSignature(BillInvoice invoice, BillUser customer, Integer paymentAttempt) throws NoSuchAlgorithmException, InvalidKeyException {
		Map<String, String> postData = new HashMap<String, String>();
		String appId = BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_APP_ID);
		postData.put("appId", appId);
		String orderId = getOrderId(invoice, paymentAttempt);
		postData.put("orderId", orderId);
		postData.put("orderAmount", CommonUtils.getStringValue(invoice.getPayable(), true));
		postData.put("orderCurrency", "INR");
		String invoicePurpose = invoicePurpose(invoice);
		postData.put("orderNote", invoicePurpose);
		postData.put("customerName", customer.getName());
		if(StringUtils.isBlank(customer.getEmail())) {
			customer.setEmail("contact@payperbill.in");
		}
		postData.put("customerEmail", customer.getEmail());
		postData.put("customerPhone", customer.getPhone());
		String returnUrl = BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_RETURN_URL);
		postData.put("returnUrl", returnUrl);
		postData.put("notifyUrl", returnUrl);
		System.out.println("== DATA == " + postData);
		String data = "";
		SortedSet<String> keys = new TreeSet<String>(postData.keySet());
		for (String key : keys) {
			data = data + key + postData.get(key);
		}
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key_spec = new SecretKeySpec(BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_APP_SECRET).getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key_spec);
		invoice.setCashfreeSignature(Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes())));
		invoice.setCashFreeRedirectUrl(returnUrl);
		invoice.setCashFreeAppId(appId);
		invoice.setCashFreePaymentUrl(BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_PAYMENT_URL));
		invoice.setComments(invoicePurpose);
		invoice.setCashFreeTxId(orderId);
	}

	private static String getOrderId(BillInvoice invoice, Integer paymentAttempt) {
		return CommonUtils.getStringValue(invoice.getId()) + TXID_SEPARATOR + CommonUtils.getStringValue(paymentAttempt);
	}

	public static boolean verifyCashfreeSignature(BillInvoice invoice, String orderId) {
		try {
			LinkedHashMap<String, String> postData = new LinkedHashMap<String, String>();
			postData.put("orderId", orderId);
			postData.put("orderAmount", CommonUtils.getStringValue(invoice.getAmount(), false));
			postData.put("referenceId", invoice.getPaymentId());
			postData.put("txStatus", invoice.getStatus());
			postData.put("paymentMode", invoice.getPaymentMode());
			postData.put("txMsg", invoice.getComments());
			postData.put("txTime", invoice.getPaymentRequestId());

			String data = "";
			Set<String> keys = postData.keySet();

			for (String key : keys) {
				data = data + postData.get(key);
			}
			String secretKey = BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_APP_SECRET);
			Mac sha256_HMAC;
			sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key_spec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secret_key_spec);
			String signature = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes()));
			if (StringUtils.equalsIgnoreCase(signature, invoice.getCashfreeSignature())) {
				return true;
			}
		} catch (NoSuchAlgorithmException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		} catch (InvalidKeyException e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	public static void preparePayTmRequest(BillInvoice invoice, BillUser customer, Integer paymentAttempt) {
		try {
			if(invoice == null || customer == null || invoice.getId() == null || customer.getId() == null) {
				return;
			}
			String returnUrl = BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_RETURN_URL);
			invoice.setPaytmRedirectUrl(returnUrl);
			invoice.setPaytmChannel(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_CHANNEL));
			invoice.setPaytmMid(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_MID));
			invoice.setPaytmWebsite(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_WEBSITE));
			invoice.setPaytmUrl(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_URL));

			TreeMap<String, String> paytmParams = new TreeMap<String, String>();
			paytmParams.put("MID", invoice.getPaytmMid());
			paytmParams.put("ORDER_ID", getOrderId(invoice, paymentAttempt));
			paytmParams.put("CHANNEL_ID", invoice.getPaytmChannel());
			paytmParams.put("CUST_ID", customer.getId().toString());
			paytmParams.put("MOBILE_NO", customer.getPhone());
			paytmParams.put("EMAIL", customer.getEmail());
			paytmParams.put("TXN_AMOUNT", CommonUtils.getStringValue(invoice.getPayable(), true));
			paytmParams.put("WEBSITE", invoice.getPaytmWebsite());
			paytmParams.put("INDUSTRY_TYPE_ID", "Retail");
			paytmParams.put("CALLBACK_URL", invoice.getPaytmRedirectUrl());
			String paytmChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_SECRET), paytmParams);
			invoice.setPaytmChecksum(paytmChecksum);
		} catch (Exception e) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e));
		}

	}
	
	public static boolean matchPayTmChecksum(MultivaluedMap<String, String> formParams) {
		boolean isValidChecksum = false;
		try {
			TreeMap<String, String> paytmParams = new TreeMap<String, String>();
			String paytmChecksum = "";
			// Request is HttpServletRequest
			for (Entry<String, List<String>> e : formParams.entrySet()) {
				if(CollectionUtils.isEmpty(e.getValue())) {
					continue;
				}
			    if ("CHECKSUMHASH".equalsIgnoreCase(e.getKey())){
			        paytmChecksum = e.getValue().get(0);
			    } else {
			        paytmParams.put(e.getKey(), e.getValue().get(0));
			    }
			}
			// Call the method for verification
			isValidChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().verifycheckSum(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_SECRET), paytmParams, paytmChecksum);
		} catch (Exception e1) {
			LoggingUtil.logError(ExceptionUtils.getStackTrace(e1));
		}
		// If isValidChecksum is false, then checksum is not valid
		return isValidChecksum;
	}

}
