package com.rns.web.billapp.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class BillPropertyUtil {
	
	
	public static final String PROPERTIES_PATH = "/home/service/properties/billapp.properties";
	
	//Insta
	public static final String PAYMENT_URL = "payment.url";
	public static final String PAYMENT_REFERRER =  "public.refferer";
	public static final String PAYMENT_CLIENT_ID = "client.id";
	public static final String PAYMENT_CLIENT_SECRET = "client.secret";
	public static final String PAYMENT_AUTH = "payment.auth";
	public static final String PAYMENT_USER = "payment.user";
	public static final String PAYMENT_PASSWORD = "payment.password";
	public static final String PAYMENT_WEBHOOK = "payment.webhook";
	public static final String PAYMENT_RESULT = "payment.result";
	public static final String PAYMENT_LINK = "payment.link";
	//HDFC
	public static final String HDFC_KEY = "hdfc.working.key";
	public static final String HDFC_MERCHANT_ID = "hdfc.merchant.id";
	public static final String HDFC_ACCESS_CODE = "hdfc.access.code";
	public static final String HDFC_URL = "hdfc.url";
	public static final String HDFC_PAYMENT_RESULT = "hdfc.payment.webhook";
	//Atom
	public static final String ATOM_LOGIN = "atom.login";
	public static final String ATOM_PASSWORD = "atom.password";
	public static final String ATOM_REQUEST_HASH = "atom.request.hash";
	public static final String ATOM_RESPONSE_HASH = "atom.response.hash";
	public static final String ATOM_PRODUCT_ID = "atom.product.id";
	public static final String ATOM_CLIENT_CODE = "atom.client.code";
	public static final String ATOM_PAYMENT_URL = "atom.payment.url";
	public static final String ATOM_REDIRECT_URL = "atom.redirect.url";
	
	
	
	public static String getProperty(String name) {
		try {
			//File file = new File(BillPaymentUtil.class.getClassLoader().getResource("app.properties").getFile());
			File file = new File(PROPERTIES_PATH);
			FileInputStream fileInput = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(fileInput);
			fileInput.close();
			return properties.getProperty(name);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
