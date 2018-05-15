package com.rns.web.billapp.service.util;

public interface BillConstants {
	
	String ROOT_FOLDER_LOCATION = "/home/service/BillData/";
	
	String DATE_FORMAT = "yyyy-MM-dd";
	Integer STATUS_OK = 200;
	String RESPONSE_OK = "OK";
	
	Integer ERROR_CODE_FATAL = -999;
	Integer ERROR_CODE_GENERIC = -111;
	
	String ERROR_INSUFFICIENT_FIELDS = "Insufficient fields! Please send all the required fields!";
	String ERROR_NO_USER = "User profile was not found!";
	String ERROR_IN_PROCESSING = "Some error occurred while processing ..";
	String ERROR_MOBILE_PRESENT = "The mobile number is already registered!";
	
	String MSG_REGISTERED_SUCCESS = "Your profile has been registered successfully! Our team will review your profile and contact you further to approve it.";
	
	String STATUS_DELETED = "D";
	String STATUS_ACTIVE = "A";
	String STATUS_PENDING = "P";
	String USER_DB_ATTR_PHONE = "phone";
	String ID_ATTR = "id";
	String LOG_CHANGE_TEMP = "TEMP";
	String LOG_CHANGE_PERM = "PERM";

}
