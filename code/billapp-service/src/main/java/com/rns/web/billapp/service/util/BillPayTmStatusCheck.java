package com.rns.web.billapp.service.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SessionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.paytm.pg.merchant.CheckSumServiceHelper;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.impl.BillUserBoImpl;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class BillPayTmStatusCheck implements Runnable {
	
	public static Map<Integer, String> paytmPendingInvoices = new ConcurrentHashMap<Integer, String>();
	
	
	private SessionFactory sessionFactory;
	private ThreadPoolTaskExecutor executor;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		userBo = new BillUserBoImpl();
		userBo.setSessionFactory(sessionFactory);
		userBo.setExecutor(executor);
	}
	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}
	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}
	
	private BillUserBoImpl userBo;
	
	//{"MID":"rxazcv89315285244163",
	//"ORDERID":"order1",
	//"CHECKSUMHASH":"CsTeIGhOnegWColuGQaGphMizcsECToTPZ9x/oFPrNZk1TaiV2bFJZzfCwlU7/7ZDbDZIdIfCXfrNjNlFmoUjOMmg8tlR4/0gakLfFNIe2c="}'

	public void run() {
		LoggingUtil.logMessage("........ Running paytm logger ...", LoggingUtil.paytmLogger);
		if(paytmPendingInvoices == null || CollectionUtils.isEmpty(paytmPendingInvoices.entrySet())) {
			return;
		}
		for(Entry<Integer, String> e: paytmPendingInvoices.entrySet()) {
			try {
				LoggingUtil.logMessage("Checking paytm status for ... " + e.getKey(), LoggingUtil.paytmLogger);
				TreeMap<String, String> paytmParams = new TreeMap<String, String>();
				paytmParams.put("MID", BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_MID));
				paytmParams.put("ORDERID", e.getValue());
			    String paytmChecksum;
				paytmChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_SECRET), paytmParams);
				paytmParams.put("CHECKSUMHASH", paytmChecksum);
				BillInvoice invoice = getTransactionStatus(paytmParams);
				if(invoice != null) {
					invoice.setId(e.getKey());
					BillServiceRequest request = new BillServiceRequest();
					request.setInvoice(invoice);
					userBo.completePayment(request);
				}
			} catch (Exception e1) {
				LoggingUtil.logMessage(ExceptionUtils.getStackTrace(e1), LoggingUtil.paytmLogger);
			}
		    
		}
		LoggingUtil.logMessage("....... End of paytm logger ...", LoggingUtil.paytmLogger);
	}
	
	public static BillInvoice getTransactionStatus(Map<String, String> values) {
		try {
			/*TreeMap<String, String> paytmParams = new TreeMap<String, String>();
			paytmParams.put("MID", merchantMid);
			paytmParams.put("ORDERID", orderId);
		    String paytmChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(merchantKey, paytmParams);
		    paytmParams.put("CHECKSUMHASH", paytmChecksum);*/
		    JSONObject obj = new JSONObject(values);
		    String postData = "JsonData=" + obj.toString();
		    
		    String url = BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_TXN_STATUS_URL);
			
		    /*HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		    connection.setRequestMethod("POST");
		    connection.setRequestProperty("contentType", "application/json");
		    connection.setUseCaches(false);
		    connection.setDoOutput(true);

		    DataOutputStream requestWriter = new DataOutputStream(connection.getOutputStream());
		    requestWriter.writeBytes(postData);
		    requestWriter.close();
		    String responseData = "";
		    InputStream is = connection.getInputStream();
		    BufferedReader responseReader = new BufferedReader(new InputStreamReader(is));
		    if((responseData = responseReader.readLine()) != null) {
		        System.out.append("Response Json = " + responseData);
		    }
		    System.out.append("Requested Json = " + postData + " ");
		    responseReader.close();
		    return responseData;*/
		    
		    ClientConfig config = new DefaultClientConfig();
			config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			Client client = Client.create(config);

			WebResource webResource = client.resource(url);

			LoggingUtil.logMessage("Calling paytm transaction URL :" + url + " request:" + new ObjectMapper().writeValueAsString(postData), LoggingUtil.paytmLogger);

			ClientResponse response = webResource.type("application/json").post(ClientResponse.class, postData);

			if (response.getStatus() != 200) {
				LoggingUtil.logMessage("Failed in paytm transaction URL : HTTP error code : " + response.getStatus(), LoggingUtil.paytmLogger);
			}
			String output = response.getEntity(String.class);
			LoggingUtil.logMessage("Output from paytm transaction URL : " + response.getStatus() + ".... \n " + output, LoggingUtil.paytmLogger);
			JsonNode node = new ObjectMapper().readTree(output);
			JsonNode txnStatusNode = node.get("STATUS");
			if(txnStatusNode != null) {
				BillInvoice invoice = new BillInvoice();
				if(StringUtils.equals("TXN_SUCCESS", txnStatusNode.getTextValue())) {
					invoice.setStatus("Success");
				} else { 
					//If transaction is failed or in process or pending.. no need to send notification
					return null;
				}
				invoice.setAmount(new BigDecimal(getNodeValue(node, "TXNAMOUNT")));
				invoice.setTxTime(getNodeValue(node, "TXNDATE"));
				invoice.setPaymentId(getNodeValue(node, "TXNID"));
				invoice.setPaymentMedium(BillConstants.PAYMENT_MEDIUM_PAYTM);
				invoice.setPaymentMode(getNodeValue(node, "PAYMENTMODE"));
				if(StringUtils.equalsIgnoreCase("PPI", invoice.getPaymentMode())) {
					invoice.setPaymentMode("Wallet");
				}
				invoice.setComments(getNodeValue(node, "RESPMSG"));
				invoice.setPaymentRequestId(getNodeValue(node, "BANKTXNID"));
				invoice.setPaymentResponse(output);
				return invoice;
			}
		    
		} catch (Exception exception) {
		    LoggingUtil.logMessage(ExceptionUtils.getStackTrace(exception), LoggingUtil.paytmLogger);
		}
		return null;
	}
	
	private static String getNodeValue(JsonNode node, String key) {
		if(node != null && node.get(key) != null) {
			return node.get(key).getTextValue();
		}
		return null;
	}

}
