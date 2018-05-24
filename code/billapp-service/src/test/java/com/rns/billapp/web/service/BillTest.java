package com.rns.billapp.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.bo.impl.BillAdminBoImpl;
import com.rns.web.billapp.service.bo.impl.BillSchedulerBoImpl;
import com.rns.web.billapp.service.bo.impl.BillUserBoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillInvoice;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

public class BillTest {
	
	private BillUserBoImpl userbo;
	private BillAdminBoImpl adminBo;
	private BillSchedulerBoImpl scheduler;

	@Before
	public void init() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		userbo = (BillUserBoImpl) ctx.getBean("userBo");
		adminBo = (BillAdminBoImpl) ctx.getBean("adminBo");
		scheduler = (BillSchedulerBoImpl) ctx.getBean("schedulerBo");
	}
	
	@Test
	public void testUpdateUserInfo() {
		
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setName("Ajinkya KUL");
		//user.setPanDetails("AK12345");
		user.setPhone("1231231231");
		user.setAadharNumber("1233");
		user.setId(1);
		//User business
		BillBusiness business = new BillBusiness();
		business.setId(2);
		business.setName("AK business and sons");
		business.setDescription("Some business");
		business.setAddress("AK Business center");
		BillSector businessSector = new BillSector(2);
		business.setBusinessSector(businessSector);
		
		List<BillLocation> locations = new ArrayList<BillLocation>();
		BillLocation loc1 = new BillLocation(2);
		locations.add(loc1);
		//business.setBusinessLocations(locations);
		user.setCurrentBusiness(business);
		request.setUser(user);
		System.out.println(userbo.updateUserInfo(request).getResponse());
	}
	
	@Test
	public void testUpdateUserFinancial() {
		BillServiceRequest request = new BillServiceRequest();
		
		BillUser user = new BillUser();
		user.setPhone("1231231231");
		BillFinancialDetails financialDetails = new BillFinancialDetails();
		financialDetails.setId(2);
		financialDetails.setAccountNumber("23235615263");
		financialDetails.setBankAddress("Pune 30");
		financialDetails.setBankName("My Dena bank");
		financialDetails.setIfscCode("IFSC123");
		user.setFinancialDetails(financialDetails);
		
		request.setUser(user);
		
		System.out.println(userbo.updateUserFinancialInfo(request).getResponse());
	}
	
	@Test
	public void testUpdateBusinessItem() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		BillItem item = new BillItem();
		item.setId(1);
		BillItem parentItem = new BillItem();
		parentItem.setId(2);
		item.setParentItem(parentItem);
		request.setBusiness(business);
		request.setItem(item);
		System.out.println(userbo.updateBusinessItem(request));
	}
	
	@Test
	public void testUpdateCustomer() {
		BillServiceRequest request = new BillServiceRequest();
		
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		user.setEmail("ajinkyakulkarni1391@yahoo.com");
		user.setName("Shiva");
		user.setAddress("Sadashiv peth, Pune 30");
		BillSubscription currentSubscription = new BillSubscription();
		BillLocation area = new BillLocation(2);
		//currentSubscription.setId(1);
		currentSubscription.setArea(area);
		currentSubscription.setServiceCharge(new BigDecimal(10));
		user.setCurrentSubscription(currentSubscription);
		
		BillBusiness business = new BillBusiness();
		business.setId(2);
		request.setBusiness(business);
		request.setUser(user);
		
		System.out.println(userbo.updateCustomerInfo(request).getResponse());
	}
	
	@Test
	public void testUpdateCustomerItem() {
		BillServiceRequest request = new BillServiceRequest();
		
		BillItem item = new BillItem();
		item.setId(3);
		item.setQuantity(new BigDecimal(1));
		item.setWeekDays("1,5,7");
		BillItem parentItem = new BillItem();
		parentItem.setId(1);
		item.setParentItem(parentItem);
		request.setItem(item);
		
		BillUser user = new BillUser();
		BillSubscription currentSubscription = new BillSubscription();
		currentSubscription.setId(1);
		user.setCurrentSubscription(currentSubscription);
		request.setUser(user);
		System.out.println(userbo.updateCustomerItem(request).getResponse());
	}
	
	@Test
	public void testPauseDelivery() {
		BillServiceRequest request = new BillServiceRequest();
		
		BillItem item = new BillItem();
		item.setId(3);
		item.setQuantity(new BigDecimal(0));
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2018-05-19"));
		changeLog.setToDate(CommonUtils.convertDate("2018-05-23"));
		changeLog.setChangeType(BillConstants.LOG_CHANGE_TEMP);
		item.setChangeLog(changeLog);
		BillItem parentItem = new BillItem();
		parentItem.setId(1);
		item.setParentItem(parentItem);
		request.setItem(item);
		
		BillUser user = new BillUser();
		BillSubscription currentSubscription = new BillSubscription();
		currentSubscription.setId(1);
		user.setCurrentSubscription(currentSubscription);
		request.setUser(user);
		System.out.println(userbo.updateCustomerItemTemporary(request).getResponse());
	}
	
	@Test
	public void testPauseDeliveryBusinessItem() {
		BillServiceRequest request = new BillServiceRequest();
		BillItem item = new BillItem();
		item.setQuantity(new BigDecimal(0));
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2018-05-19"));
		changeLog.setToDate(CommonUtils.convertDate("2018-05-23"));
		changeLog.setChangeType(BillConstants.LOG_CHANGE_TEMP);
		item.setChangeLog(changeLog);
		BillItem parentItem = new BillItem();
		parentItem.setId(1);
		item.setParentItem(parentItem);
		request.setItem(item);
		BillUser user = new BillUser();
		request.setUser(user);
		System.out.println(userbo.updateCustomerItemTemporary(request).getResponse());
	}
	
	@Test
	public void testUpdateInvoice() {
		BillServiceRequest request = new BillServiceRequest();
		
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(3);
		invoice.setYear(2018);
		invoice.setAmount(new BigDecimal(110));
		invoice.setPendingBalance(new BigDecimal(20));
		invoice.setComments("Pending service charge ..");
		
		BillUser user = new BillUser();
		BillSubscription currentSubscription = new BillSubscription();
		currentSubscription.setId(1);
		user.setCurrentSubscription(currentSubscription);
		request.setUser(user);
		request.setInvoice(invoice);
		System.out.println(userbo.updateCustomerInvoice(request).getResponse());
	}
	
	@Test
	public void testUpdateParentItem() {
		
		BillServiceRequest request = new BillServiceRequest();
		BillItem item = new BillItem();
		item.setId(3);
		item.setName("Sandhyanand Times");
		item.setPrice(new BigDecimal(10));
		item.setDescription("The worst newspaper in the history of mankind. It's AAJ ka ANAND");
		item.setFrequency("DAILY");
		item.setWeekDays("1,2,3,4,5,6,7");
		item.setWeeklyPricing("10,10,10,10,10,15,15");
		request.setItem(item);
		adminBo.updateItem(request);
	}
	
	@Test
	public void testLoadProfile() {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("1231231231");
		request.setUser(user);
		BillServiceResponse response = userbo.loadProfile(request);
		System.out.println(response.getResponse() + ":" + response.getWarningText());
		System.out.println(response.getUser());
	}
	
	@Test
	public void testGetAreas() {
		System.out.println(userbo.getAllAreas().getLocations().size());
	}
	
	@Test
	public void testGetSectorItems() {
		BillServiceRequest request = new BillServiceRequest();
		BillSector sector = new BillSector(2);
		request.setSector(sector);
		System.out.println(userbo.getSectorItems(request).getItems());
	}
	
	@Test
	public void testGetBusinessItems() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		request.setBusiness(business);
		System.out.println(userbo.getBusinessItems(request).getItems());
	}
	
	@Test
	public void testGetBusinessCustomers() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		request.setBusiness(business);
		System.out.println(userbo.getAllBusinessCustomers(request).getUsers());
	}
	
	@Test
	public void testGetDeliveries() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		request.setBusiness(business);
		request.setRequestedDate(new Date());
		//System.out.println(userbo.loadDeliveries(request).getUsers());
		
		System.out.println(new BillVendorDaoImpl(userbo.getSessionFactory().openSession()).getDeliveries(null).get(0).getPhone());
	}
	
	@Test
	public void testLogQueries() {
		List<Object[]> objects = new BillLogDAOImpl(userbo.getSessionFactory().openSession()).getParentItemQuantityLogs(CommonUtils.getDate(new Date()));
		for(Object[] array: objects) {
			System.out.println(array[0] + ":" + array[1] + ":" + array[2]);
		}
		System.out.println(objects);
	}
	
	@Test
	public void testInvoiceRoutine() {
		scheduler.calculateInvoices();
	}
}
