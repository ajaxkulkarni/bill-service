package com.rns.billapp.web.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.bo.impl.BillAdminBoImpl;
import com.rns.web.billapp.service.bo.impl.BillSchedulerBoImpl;
import com.rns.web.billapp.service.bo.impl.BillUserBoImpl;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBSubscription;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillLogAppender;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.rns.web.billapp.service.util.CommonUtils;
import com.rns.web.billapp.service.util.LoggingUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

//@Ignore
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
	public void testPauseDeliveryBusinessItem() throws InterruptedException {
		BillServiceRequest request = new BillServiceRequest();
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2018-08-19"));
		changeLog.setToDate(CommonUtils.convertDate("2018-08-23"));
		changeLog.setChangeType(BillConstants.LOG_CHANGE_TEMP);
		
		BillItem item1 = new BillItem();
		item1.setQuantity(new BigDecimal(0));
		item1.setChangeLog(changeLog);
		BillItem parentItem = new BillItem();
		parentItem.setId(5);
		item1.setParentItem(parentItem);
		
		BillItem item2 = new BillItem();
		item2.setQuantity(new BigDecimal(0));
		item2.setChangeLog(changeLog);
		BillItem parentItem2 = new BillItem();
		parentItem2.setId(6);
		item2.setParentItem(parentItem2);
		
		
		List<BillItem> items = new ArrayList<BillItem>();
		items.add(item1);
		items.add(item2);
		request.setItems(items);
		//request.setItem(item);
		BillUser user = new BillUser();
		request.setUser(user);
		System.out.println(userbo.updateCustomerItemTemporary(request).getResponse());
		
		Thread.currentThread().wait();
	}
	
	
	@Test
	public void testPauseDeliveryBusinessItemMultiple() throws IllegalAccessException, InvocationTargetException  {
		BillServiceRequest request = new BillServiceRequest();
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2018-08-19"));
		changeLog.setToDate(CommonUtils.convertDate("2018-08-19"));
		changeLog.setChangeType(BillConstants.LOG_CHANGE_TEMP);
		Session session = userbo.getSessionFactory().openSession();
		List<BillDBItemBusiness> items = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBItemBusiness.class, "business.id", 11, true);
		if(CollectionUtils.isNotEmpty(items)) {
			List<BillItem> requestItems = new ArrayList<BillItem>();
			System.out.println("Found " + items.size());
			for(BillDBItemBusiness itemBusiness: items) {
				BillItem item = new BillItem();
				item.setChangeLog(changeLog);
				item.setQuantity(BigDecimal.ZERO);
				item.setParentItem(BillDataConverter.getItem(itemBusiness));
				requestItems.add(item);
			}
			request.setItems(requestItems);
			BillUser user = new BillUser();
			request.setUser(user);
			System.out.println(userbo.updateCustomerItemTemporary(request).getResponse());
		}
				
		CommonUtils.closeSession(session);
		//request.setItem(item);
		
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
		List<BillItem> items = new ArrayList<BillItem>();
		BillItem item1 = new BillItem();
		item1.setId(3);
		item1.setQuantity(BigDecimal.TEN);
		item1.setPrice(new BigDecimal(100));
		BillItem parent1 = new BillItem();
		parent1.setId(1);
		item1.setParentItem(parent1);
		items.add(item1);
		invoice.setInvoiceItems(items);
		
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
	public void testLogQueries() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setId(1);
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(6);
		request.setInvoice(invoice);
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(userbo.getCustomerActivity(request).getOrders()));
	}
	
	@Test
	public void testInvoiceRoutine() throws ParseException {
		/*for(int i = 3; i <= 31; i++) {
			String day = "" + i;
			if(i < 10) {
				day = "0" + i;
			}
			scheduler.calculateInvoices(new SimpleDateFormat(BillConstants.DATE_FORMAT).parse("2018-07-" + day));
		}*/
		
		scheduler.calculateInvoices(new SimpleDateFormat(BillConstants.DATE_FORMAT).parse("2018-08-19"));
	}
	
	@Test
	public void testGetAllCustomerInvoices() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setId(1);
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(userbo.getCustomerInvoices(request).getInvoices()));
	}
	
	@Test
	public void testPaymentUrl() throws InterruptedException {
		BillServiceRequest request = new BillServiceRequest();
		BillInvoice invoice = new BillInvoice();
		invoice.setId(3);
		request.setInvoice(invoice);
		request.setRequestType("EMAIL");
		userbo.sendCustomerInvoice(request);
		Thread.sleep(10000);
	}
	
	@Test
	public void testGetToken() {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setId(17);
		request.setUser(user);
		userbo.updatePaymentCredentials(request);
	}
	
	@Test
	public void testPickups() throws HibernateException, ParseException {
		List<Object[]> list = new BillVendorDaoImpl(userbo.getSessionFactory().openSession()).getItemOrderSummary(new SimpleDateFormat("yyyy-MM-dd").parse("2018-05-24"), 2);
		for(Object[] o: list) {
			System.out.println(o[0] + ":" + o[1] + ":" + o[2]);
		}
	}
	
	@Test
	public void testMail() {
		BillMailUtil mailUtil = new BillMailUtil(BillConstants.MAIL_TYPE_REGISTRATION);
		BillUser user = new BillUser();
		user.setEmail("ajinkyashiva@gmail.com");
		user.setName("Abhishek");
		mailUtil.setUser(user);
		mailUtil.sendMail();
	}
	
	@Test
	public void testUploadUsers() throws FileNotFoundException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		BillFile file = new BillFile();
		file.setFileData(new FileInputStream("F:\\Resoneuronance\\BillDue\\Documents\\sample_excel.xlsx"));
		request.setBusiness(business);
		request.setFile(file);
		System.out.println(adminBo.uploadVendorData(request).getResponse());
	}
	
	@Test
	public void customLogtest() {
		Logger logger = Logger.getLogger(this.getClass());
		logger.addAppender(new BillLogAppender());
		logger.info("Something");
	}
	
	@Test
	public void testHdfc() {
		BillInvoice invoice = new BillInvoice();
		invoice.setAmount(new BigDecimal(1));
		invoice.setId(1);
		//BillPaymentUtil.prepareHdfcRequest(invoice);
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(config);
		client.addFilter(new LoggingFilter(System.out));
		
		String url = BillPropertyUtil.getProperty(BillPropertyUtil.HDFC_URL);
		WebResource webResource = client.resource(url);

		LoggingUtil.logMessage("Calling HDFC payment request URL ==>" + url);

		MultivaluedMap<String, String> request = new MultivaluedMapImpl();
		request.add("encRequest", invoice.getHdfcRequest());
		request.add("access_code", invoice.getHdfcAccessCode());

		ClientResponse response = webResource.post(ClientResponse.class, request);

		System.out.println("HEADERS ==>" + response.getHeaders());
		String entity = response.getEntity(String.class);
		LoggingUtil.logMessage("Output from HDFC Payment request URL ...." + response.getStatus() + " RESP:" + entity + " \n");
	}
	
	@Test
	public void testGenerateBills() {
		BillServiceRequest request = new BillServiceRequest();
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(7);
		invoice.setYear(2018);
		request.setInvoice(invoice);
		adminBo.generateBills(request);
	}
	
	@Test
	public void testHoliday() {
		new BillLogDAOImpl(adminBo.getSessionFactory().openSession()).getHolidays(9, 23, CommonUtils.convertDate("2018-09-23"));
	}
}
