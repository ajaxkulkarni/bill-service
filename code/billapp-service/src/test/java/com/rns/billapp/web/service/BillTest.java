package com.rns.billapp.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import com.rns.web.billapp.service.bo.impl.BillUserBoImpl;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.domain.BillServiceRequest;

public class BillTest {
	
	private BillUserBoImpl userbo;

	@Before
	public void init() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		userbo = (BillUserBoImpl) ctx.getBean("userBo");
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
	public void updateBusinessItem() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		BillItem item = new BillItem();
		item.setId(2);
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
		user.setAddress("Navi peth, Pune 30");
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
		//item.setId(3);
		item.setQuantity(new BigDecimal(1));
		item.setWeekDays("1,7");
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
	
}
