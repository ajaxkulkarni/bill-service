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
	
	public static final String CASHFREE_APP_ID = "cashfree.app.id";
	public static final String CASHFREE_APP_SECRET = "cashfree.app.secret";
	public static final String CASHFREE_RETURN_URL = "cashfree.return.url";
	public static final String CASHFREE_PAYMENT_URL = "cashfree.payment.url";

	
	public static final String ADMIN_USERNAME = "admin.username";
	public static final String ADMIN_PASSWORD = "admin.password";
	public static final String ADMIN_TOKEN = "admin.token";

	public static final String LOGO_URL = "logo.url";

	public static final String PAYTM_RETURN_URL = "paytm.return.url";
	public static final String PAYTM_WEBSITE = "paytm.website";
	public static final String PAYTM_MID = "paytm.mid";
	public static final String PAYTM_CHANNEL = "paytm.channel.id";
	public static final String PAYTM_SECRET = "paytm.secret";
	public static final String PAYTM_URL = "paytm.url";
	
	//Emails
	public static final String MAIL_HOST = "mail.host";
	public static final String MAIL_ID = "mail.from";
	public static final String MAIL_USERNAME = "mail.user";//
	public static final String MAIL_PASSWORD = "mail.password";
	//
	public static final String MAIL_AUTH = "true";
	public static final String MAIL_PORT = "mail.port";//"587";//"465";// "587";
	
	
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
