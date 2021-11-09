package com.rns.billapp.web.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.itextpdf.text.DocumentException;
import com.paytm.pg.merchant.CheckSumServiceHelper;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillCustomerGroup;
import com.rns.web.billapp.service.bo.domain.BillFinancialDetails;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillNotification;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillSubscription;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.bo.impl.BillAdminBoImpl;
import com.rns.web.billapp.service.bo.impl.BillBusinessBoImpl;
import com.rns.web.billapp.service.bo.impl.BillCustomerBoImpl;
import com.rns.web.billapp.service.bo.impl.BillSchedulerBoImpl;
import com.rns.web.billapp.service.bo.impl.BillUserBoImpl;
import com.rns.web.billapp.service.dao.domain.BillDBItemBusiness;
import com.rns.web.billapp.service.dao.domain.BillDBItemParent;
import com.rns.web.billapp.service.dao.domain.BillDBLocation;
import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.dao.impl.BillAdminDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillGenericDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillInvoiceDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillLogDAOImpl;
import com.rns.web.billapp.service.dao.impl.BillOrderDaoImpl;
import com.rns.web.billapp.service.dao.impl.BillVendorDaoImpl;
import com.rns.web.billapp.service.domain.BillFile;
import com.rns.web.billapp.service.domain.BillServiceRequest;
import com.rns.web.billapp.service.domain.BillServiceResponse;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.BillDataConverter;
import com.rns.web.billapp.service.util.BillFCMNotificationBroadcaster;
import com.rns.web.billapp.service.util.BillLogAppender;
import com.rns.web.billapp.service.util.BillMailUtil;
import com.rns.web.billapp.service.util.BillPDFUtil;
import com.rns.web.billapp.service.util.BillPayTmStatusCheck;
import com.rns.web.billapp.service.util.BillPaymentUtil;
import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.rns.web.billapp.service.util.BillSMSUtil;
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

	private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
	private static final String[] SCOPES = { MESSAGING_SCOPE };

	private BillUserBoImpl userbo;
	private BillAdminBoImpl adminBo;
	private BillSchedulerBoImpl scheduler;
	private BillBusinessBoImpl businessBo;
	private BillCustomerBoImpl customerBo;

	@Before
	public void init() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		userbo = (BillUserBoImpl) ctx.getBean("userBo");
		adminBo = (BillAdminBoImpl) ctx.getBean("adminBo");
		scheduler = (BillSchedulerBoImpl) ctx.getBean("schedulerBo");
		businessBo = (BillBusinessBoImpl) ctx.getBean("businessBo");
		customerBo = (BillCustomerBoImpl) ctx.getBean("customerBo");
	}

	@Test
	public void testUpdateUserInfo() {

		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setName("Sakal company");
		// user.setPanDetails("AK12345");
		user.setPhone("555555555");
		user.setAadharNumber("444411114444");
		// user.setId(1);
		// User business
		BillBusiness business = new BillBusiness();
		// business.setId(2);
		business.setName("Sakal paper company");
		business.setDescription("Testing the new registration");
		business.setAddress("Pune");
		BillSector businessSector = new BillSector(2);
		business.setBusinessSector(businessSector);

		List<BillLocation> locations = new ArrayList<BillLocation>();
		BillLocation loc1 = new BillLocation(2);
		locations.add(loc1);
		// business.setBusinessLocations(locations);
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
		// item.setId(1);
		BillItem parentItem = new BillItem();
		parentItem.setId(1);
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
		// currentSubscription.setId(1);
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
		// request.setItem(item);
		BillUser user = new BillUser();
		request.setUser(user);
		System.out.println(userbo.updateCustomerItemTemporary(request).getResponse());

		Thread.currentThread().wait();
	}

	@Test
	public void testPauseDeliveryBusinessItemMultiple() throws IllegalAccessException, InvocationTargetException {
		BillServiceRequest request = new BillServiceRequest();
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2018-08-19"));
		changeLog.setToDate(CommonUtils.convertDate("2018-08-19"));
		changeLog.setChangeType(BillConstants.LOG_CHANGE_TEMP);
		Session session = getSession();
		List<BillDBItemBusiness> items = new BillGenericDaoImpl(session).getEntitiesByKey(BillDBItemBusiness.class, "business.id", 11, true, null, null);
		if (CollectionUtils.isNotEmpty(items)) {
			List<BillItem> requestItems = new ArrayList<BillItem>();
			System.out.println("Found " + items.size());
			for (BillDBItemBusiness itemBusiness : items) {
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
		// request.setItem(item);

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
		System.out.println(userbo.getSectorItems(request).getItems().get(0).getName());
	}

	@Test
	public void testGetBusinessItems() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(2);
		request.setBusiness(business);
		System.out.println(userbo.getBusinessItems(request).getItems().get(0).getParentItem().getName());
	}

	@Test
	public void testGetBusinessCustomers() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(48);
		request.setBusiness(business);
		System.out.println(userbo.getAllBusinessCustomers(request).getUsers().get(0).getName());
	}

	@Test
	public void testGetDeliveries() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(5);
		request.setBusiness(business);
		request.setRequestedDate(CommonUtils.convertDate("2018-09-10"));
		System.out.println(userbo.loadDeliveries(request).getUsers().get(0).getName());

		// System.out.println(new
		// BillVendorDaoImpl(userbo.getSessionFactory().openSession()).getDeliveries(null).get(0).getPhone());
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
		
		/*for (int i = 1; i <= 31; i++) {
			String day = "" + i;
			if (i < 10) {
				day = "0" + i;
			}
			scheduler.calculateInvoices(new SimpleDateFormat(BillConstants.DATE_FORMAT).parse("2019-03-" + day));
		}*/
		 

		scheduler.calculateInvoices(new SimpleDateFormat(BillConstants.DATE_FORMAT).parse("2019-05-27"));
	}

	@Test
	public void testGetAllCustomerInvoices() throws JsonGenerationException, JsonMappingException, IOException, InterruptedException {
		/*
		 * BillServiceRequest request = new BillServiceRequest(); BillUser user
		 * = new BillUser(); user.setId(1); request.setUser(user);
		 * System.out.println(new
		 * ObjectMapper().writeValueAsString(userbo.getCustomerInvoices(request)
		 * .getInvoices()));
		 */
		// 130,60,140,40,25

		// Set SMS UrLS
		/*
		 * Session session = getSession(); session.flush(); Transaction tx =
		 * session.beginTransaction(); List<BillDBInvoice> invoices = new
		 * BillGenericDaoImpl(session).getEntities(BillDBInvoice.class, false,
		 * "id", "desc"); for(BillDBInvoice invoice: invoices) {
		 * if(!StringUtils.equalsIgnoreCase(BillConstants.
		 * INVOICE_STATUS_DELETED, invoice.getStatus()) &&
		 * StringUtils.isBlank(invoice.getShortUrl())) { String shortenUrl =
		 * BillSMSUtil.shortenUrl(null,
		 * BillRuleEngine.preparePaymentUrl(invoice.getId()));
		 * invoice.setShortUrl(shortenUrl); System.out.println("Updated " +
		 * shortenUrl); if(StringUtils.isBlank(shortenUrl)) {
		 * System.out.println("................................");
		 * Thread.sleep(60100); } } } tx.commit(); session.close();
		 */
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
		/*
		 * List<Object[]> list = new
		 * BillVendorDaoImpl(getSession()).getItemOrderSummary(new
		 * SimpleDateFormat("yyyy-MM-dd").parse("2018-05-24"), 2); for(Object[]
		 * o: list) { System.out.println(o[0] + ":" + o[1] + ":" + o[2]); }
		 */
	}

	@Test
	public void testMail() {
		BillMailUtil mailUtil = new BillMailUtil(BillConstants.MAIL_TYPE_REGISTRATION);
		BillUser user = new BillUser();
		user.setEmail("ajinkyashiva@gmail.com");
		user.setName("Ajinkya");
		mailUtil.setUser(user);
		mailUtil.sendMail();
	}

	@Test
	public void testCouponMail() {
		BillUser customer = new BillUser();
		customer.setName("Ajinkya");
		customer.setEmail("ajinkyashiva@gmail.com");
		BillBusiness currentBusiness = new BillBusiness();
		currentBusiness.setId(46);
		currentBusiness.setName("McDonalds");
		currentBusiness.setMapLocation("https://goo.gl/maps/eq4KYVSVyM82");
		BillUser owner = new BillUser();
		owner.setEmail("mcd@gmail.com");
		owner.setPhone("+911231231231");
		currentBusiness.setOwner(owner);
		customer.setCurrentBusiness(currentBusiness);
		BillMailUtil couponMail = new BillMailUtil(BillConstants.MAIL_TYPE_COUPON_ACCEPTED, customer);
		BillScheme selectedScheme = new BillScheme();
		selectedScheme.setSchemeName("50% OFF FLAT");
		selectedScheme.setComments("50% OFF on all items");
		selectedScheme.setValidTill(new Date());
		selectedScheme.setCouponCode("MCD102018670001");
		couponMail.setSelectedScheme(selectedScheme);
		couponMail.sendMail();
	}

	@Test
	public void testSMS() {

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
		user.setName("Ajinkya");
		user.setPhone("9923283604");
		BillBusiness currentBusiness = new BillBusiness();
		currentBusiness.setName("My Business");
		user.setCurrentBusiness(currentBusiness);
		BillSubscription currentSubscription = new BillSubscription();
		currentSubscription.setId(1);
		user.setCurrentSubscription(currentSubscription);

		BillSMSUtil.sendSMS(user, invoice, BillConstants.MAIL_TYPE_INVOICE, null);
	}

	@Test
	public void testUploadUsers() throws FileNotFoundException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(254);//P
		BillFile file = new BillFile();

		// String filePath =
		// "F:\\Resoneuronance\\BillDue\\Documents\\sample_excel.xlsx";
		// String filePath =
		// "F:\\Resoneuronance\\BillDue\\Documents\\Vendors\\Customer
		// lists\\Satyajit Harpude\\Dwarka Sai New.xlsx";
		String filePath = "F:\\Resoneuronance\\BillDue\\Documents\\Vendors\\Customer lists\\Nitin Jadhav\\CN Nitin Customer Details.xlsx";
		file.setFileData(new FileInputStream(filePath));
		request.setBusiness(business);
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(4);
		invoice.setYear(2019);
		invoice.setServiceCharge(BigDecimal.ZERO);
		request.setInvoice(invoice);
		request.setFile(file);
		System.out.println(adminBo.uploadVendorData(request).getResponse());
	}

	// 52 - Harpude, 85 - Kakade, Mate - 151
	@Test
	public void testUploadCustomersExternal() throws FileNotFoundException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(85);
		BillFile file = new BillFile();

		// String filePath =
		// "F:\\Resoneuronance\\BillDue\\Documents\\sample_excel.xlsx";
		String filePath = "F:\\Resoneuronance\\BillDue\\Documents\\Vendors\\Customer lists\\Sachin Kakade\\DP Road.xls";
		file.setFileData(new FileInputStream(filePath));
		request.setBusiness(business);
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(1);
		invoice.setYear(2019);
		invoice.setServiceCharge(BigDecimal.ZERO);
		request.setInvoice(invoice);
		request.setFile(file);
		request.setRequestType("EXTERNAL");
		BillCustomerGroup customerGroup = new BillCustomerGroup();
		customerGroup.setGroupName("DP Road");
		request.setCustomerGroup(customerGroup);
		System.out.println(adminBo.uploadVendorData(request).getResponse());
		// Invoice correct after 4052
	}

	/*
	 * @Test public void testCorrectBusinessInvoices() { Session session =
	 * getSession(); Transaction tx = session.beginTransaction();
	 * List<BillDBInvoice> invoices = new
	 * BillInvoiceDaoImpl(session).getAllInvoicesForMonth(1, 2019, 52);
	 * if(CollectionUtils.isNotEmpty(invoices)) { for(BillDBInvoice invoice:
	 * invoices) { if(invoice.getId() < 4052) {
	 * System.out.println("Changing invoice " + invoice.getId());
	 * if(CollectionUtils.isNotEmpty(invoice.getItems())) { BigDecimal total =
	 * BigDecimal.ZERO; for(BillDBItemInvoice itemInvoice: invoice.getItems()) {
	 * if(itemInvoice.getPrice() != null) { total =
	 * total.add(itemInvoice.getPrice()); } } invoice.setAmount(total);
	 * System.out.println("Changed the invoice amount to " + invoice.getAmount()
	 * + " for " + invoice.getId()); } } } } tx.commit(); session.close(); }
	 */

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
		// BillPaymentUtil.prepareHdfcRequest(invoice);
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
	public void testGenerateBills() throws FileNotFoundException {
		BillServiceRequest request = new BillServiceRequest();
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(5);
		invoice.setYear(2019);
		request.setInvoice(invoice);
		BillFile file = new BillFile();
		file.setFileData(new FileInputStream("F:\\Resoneuronance\\BillDue\\Documents\\Vendors\\Pricelist May.xlsx"));
		request.setFile(file);
		/*BillBusiness business = new BillBusiness();
		business.setId(229);
		request.setBusiness(business);*/
		/*BillUser customer = new BillUser();
		customer.setId(79);
		request.setUser(customer);
		request.setRequestType(BillConstants.REQUEST_TYPE_OVERWRITE);*/
		adminBo.generateBills(request);
	}

	@Test
	public void testSendInvoice() throws InterruptedException {

		synchronized (new Object()) {
			BillServiceRequest request = new BillServiceRequest();
			request.setRequestType(BillConstants.REQUEST_TYPE_EMAIL);
			BillInvoice invoice = new BillInvoice();
			invoice.setId(18);
			request.setInvoice(invoice);
			userbo.sendCustomerInvoice(request);

			Thread.currentThread().sleep(100000);
		}
	}

	@Test
	public void testHoliday() {
		new BillLogDAOImpl(adminBo.getSessionFactory().openSession()).getHolidays(9, 23, CommonUtils.convertDate("2018-09-23"));
	}

	@Test
	public void testAtom() {
		BillInvoice invoice = new BillInvoice();
		invoice.setId(1);
		invoice.setPayable(new BigDecimal("100"));
		BillDBUser vendor = new BillDBUser();
		vendor.setEmail("vendor@gmail.com");
		BillPaymentUtil.prepareAtomRequest(invoice, vendor);
		System.out.println(invoice.getAtomPaymentUrl());
	}

	@Test
	public void testSum() {
		Map<String, Object> restr = new HashMap<String, Object>();
		// restr.put("status", BillConstants.INVOICE_STATUS_PAID);
		Date fromDate = CommonUtils.convertDate("2018-08-01");
		Date toDate = CommonUtils.convertDate("2018-08-31");
		System.out.println(new BillGenericDaoImpl(getSession()).getSum(BillDBUserBusiness.class, "id", restr, fromDate, toDate, "count", null, null));
	}

	@Test
	public void testOutstanding() {
		System.out.println(new BillInvoiceDaoImpl(getSession()).getCustomerOutstanding(8, 2018, 8).get(0)[0]);
	}

	@Test
	public void testCheckSum() throws NoSuchAlgorithmException, InvalidKeyException {
		Map<String, String> postData = new HashMap<String, String>();
		String appId = BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_APP_ID);
		postData.put("appId", appId);
		postData.put("orderId", "15.17");
		postData.put("orderAmount", "25.00");
		postData.put("orderCurrency", "INR");
		postData.put("orderNote", "January 2018 monthly payment");
		postData.put("customerName", "Shiva");
		postData.put("customerEmail", "ajinkyakulkarni1491@yahoo.com");
		postData.put("customerPhone", "9923283604");
		String returnUrl = BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_RETURN_URL);
		postData.put("returnUrl", returnUrl);
		// postData.put("notifyUrl", "");
		System.out.println("== DATA == " + postData);
		String data = "";
		SortedSet<String> keys = new TreeSet<String>(postData.keySet());
		for (String key : keys) {
			data = data + key + postData.get(key);
		}
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key_spec = new SecretKeySpec(BillPropertyUtil.getProperty(BillPropertyUtil.CASHFREE_APP_SECRET).getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key_spec);
		System.out.println(Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes())));
	}
	// {txStatus=[SUCCESS], orderAmount=[25.00], orderId=[15.20],
	// paymentMode=[NET_BANKING], txTime=[2018-09-02 19:56:56],
	// signature=[edaQObEw8HR13Ti+gL+4kOE/sm7blzKQlsYeWsrMIcM=], txMsg=[Y],
	// referenceId=[3876647]}

	@Test
	public void testSorting() {
		List<BillDBLocation> entities = new BillGenericDaoImpl(getSession()).getEntities(BillDBLocation.class, true, "name", "asc");
		System.out.println(entities.get(0).getName());

		List<BillDBItemParent> items = new BillGenericDaoImpl(getSession()).getEntities(BillDBItemParent.class, true, "name", "asc");
		System.out.println(items.get(0).getName());
	}

	@Test
	public void testSettlement() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		request.setRequestType(BillConstants.INVOICE_STATUS_PAID);
		System.out.println(new ObjectMapper().writeValueAsString(adminBo.getSettlements(request)));
	}

	@Test
	public void testUpdateOrders() {
		//System.out.println(new BillOrderDaoImpl(getSession()).getOrderItems(CommonUtils.convertDate("2018-06-01"), new Date(), 1, "MONTHLY").size());
		BillServiceRequest request = new BillServiceRequest();
		BillItem item = new BillItem();
		item.setPrice(new BigDecimal(5));
		item.setParentItemId(32);
		BillUserLog changeLog = new BillUserLog();
		changeLog.setToDate(CommonUtils.convertDate("2019-03-31"));
		item.setChangeLog(changeLog);
		request.setItem(item);
		request.setRequestedDate(CommonUtils.convertDate("2019-03-01"));
		request.setRequestType("UPDATESP");
		adminBo.updateOrders(request);
	}

	@Test
	public void testGetTransactions() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(9);
		request.setBusiness(business);
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(8);
		invoice.setYear(2018);
		request.setInvoice(invoice);
		System.out.println(new ObjectMapper().writeValueAsString(userbo.getTransactions(request)));
	}

	@Test
	public void testDistributors() {
		List<BillDBLocation> locations = new ArrayList<BillDBLocation>();
		BillDBLocation loc = new BillDBLocation();
		loc.setId(3);
		locations.add(loc);
		new BillVendorDaoImpl(getSession()).getBusinessesByItemAccess(1, "Distributor", locations);
	}

	private Session getSession() {
		return userbo.getSessionFactory().openSession();
	}

	@Test
	public void testFormatDecimal() {
		System.out.println(CommonUtils.formatDecimal(new BigDecimal(53.00)));
	}

	@Test
	public void testBillSummary() {
		/*
		 * List<Object[]> itemInvoice = new
		 * BillVendorDaoImpl(getSession()).getBillSummary(9, 1, 5, 2018);
		 * System.out.println(" ------------- "); for(Object[] row: itemInvoice)
		 * { BillDBItemInvoice itemI = (BillDBItemInvoice) row[0]; BillDBInvoice
		 * invoice = (BillDBInvoice) row[1]; BillDBSubscription sub =
		 * (BillDBSubscription) row[2];
		 * //System.out.println(itemI.getInvoice());
		 * //System.out.println(invoice.getAmount());
		 * System.out.println("Subscription - " + sub.getName()); }
		 */
	}

	@Test
	public void testNotifications() {
		BillServiceRequest request = new BillServiceRequest();
		BillNotification notification = new BillNotification();
		notification.setText("<h2>Happy Diwali to all!!</p>");
		notification.setSubject("Diwali celebration with PayPerBill");
		request.setRequestType("EMAIL");
		BillSector sector = new BillSector();
		sector.setId(2);
		request.setSector(sector);
		request.setNotification(notification);
		adminBo.sendNotifications(request);
	}

	@Test
	public void testGetInvoice() {
		System.out.println(new BillInvoiceDaoImpl(getSession()).getBusinessInvoice(99).getItems());
	}

	@Test
	public void testCal() {
		// System.out.println(CommonUtils.setZero(new Date()));
		Date date = new Date();
		System.out.println(date);
		SimpleDateFormat simpDate;
		simpDate = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		System.out.println(simpDate.format(date));

	}

	@Test
	public void testBitly() {
		System.out.println(BillSMSUtil.shortenUrl(null, "https://payperbill.in/home.html?#/cashfree/invoice/2470"));
	}

	@Test
	public void testUpdateInvoiceItem() {
		BillServiceRequest request = new BillServiceRequest();
		BillItem item = new BillItem();
		item.setParentItemId(9);
		item.setPriceType(BillConstants.FREQ_MONTHLY);
		item.setPrice(new BigDecimal(25));
		request.setItem(item);
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(7);
		invoice.setYear(2018);
		request.setInvoice(invoice);
		userbo.updateInvoiceItems(request);
	}

	@Test
	public void testGetAllBusinessInvoices() {
		/*
		 * BillServiceRequest request = new BillServiceRequest(); BillBusiness b
		 * = new BillBusiness(); b.setId(49); request.setBusiness(b);
		 * businessBo.getAllInvoices(request);
		 */

		String s = "0.00";
		DecimalFormat decimalFormat = new DecimalFormat("0.#####");
		String result = decimalFormat.format(Double.valueOf(s));
		System.out.println(result);
	}

	@Test
	public void testGetCustomerProfile() {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setId(1);
		request.setUser(user);
		userbo.getCustomerProfile(request);
	}

	@Test
	public void testUpdateBusinessInvoice() {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(44);// Test distributor 1
		BillUser user = new BillUser();
		BillBusiness currentBusiness = new BillBusiness();
		currentBusiness.setId(9);
		BillInvoice currentInvoice = new BillInvoice();
		List<BillItem> invoiceItems = new ArrayList<BillItem>();
		BillItem item = new BillItem();
		item.setParentItemId(35);// Own
		BillItem parent = new BillItem();
		parent.setId(12);// Distributor
		item.setParentItem(parent);
		item.setQuantity(new BigDecimal(20));
		item.setPrice(new BigDecimal(100));
		invoiceItems.add(item);
		currentInvoice.setInvoiceDate(new Date());
		currentInvoice.setAmount(new BigDecimal(100));
		currentInvoice.setInvoiceItems(invoiceItems);
		user.setCurrentInvoice(currentInvoice);
		user.setCurrentBusiness(currentBusiness);
		request.setBusiness(business);
		request.setUser(user);
		System.out.println(userbo.updateBusinessInvoice(request).getResponse());
	}

	@Test
	public void testBusinessesByType() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(9);// John n son s
		request.setBusiness(business);
		System.out.println(new ObjectMapper().writeValueAsString(userbo.getBusinessesByType(request).getBusinesses()));
	}

	@Test
	public void testPaymentReport() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(69);
		BillItem item = new BillItem();
		BillUserLog changeLog = new BillUserLog();
		changeLog.setFromDate(CommonUtils.convertDate("2019-02-01"));
		changeLog.setToDate(CommonUtils.convertDate("2019-02-28"));
		item.setChangeLog(changeLog);
		request.setItem(item);
		request.setBusiness(business);
		BillCustomerGroup customerGroup = new BillCustomerGroup();
		customerGroup.setId(74);
		request.setCustomerGroup(customerGroup);
		System.out.println(new ObjectMapper().writeValueAsString(userbo.getPaymentsReport(request)));
	}

	@Test
	public void testPaytmTransactionCheck() throws Exception {
		TreeMap<String, String> paytmParams = new TreeMap<String, String>();
		paytmParams.put("MID", BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_MID));
		paytmParams.put("ORDERID", "25965029");
		String paytmChecksum = CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(BillPropertyUtil.getProperty(BillPropertyUtil.PAYTM_SECRET),
				paytmParams);
		paytmParams.put("CHECKSUMHASH", paytmChecksum);
		System.out.println(new ObjectMapper().writeValueAsString(BillPayTmStatusCheck.getTransactionStatus(paytmParams)));
	}

	@Test
	public void testFCM() throws IOException {
		/*GoogleCredential googleCredential = GoogleCredential.fromStream(new FileInputStream("/home/service/properties/admin-sdk.json"))
				.createScoped(Arrays.asList(SCOPES));
		googleCredential.refreshToken();
		System.out.println(googleCredential.getAccessToken());*/
		
		BillUser user = new BillUser();
		user.setId(16);
		user.setName("John doe");
		BillInvoice invoice = new BillInvoice();
		invoice.setId(123);
		invoice.setMonth(3);
		invoice.setYear(2019);
		invoice.setStatus(BillConstants.INVOICE_STATUS_PAID);
		invoice.setPayable(new BigDecimal(230));
		BillFCMNotificationBroadcaster broadcaster = new BillFCMNotificationBroadcaster(user, invoice, userbo.getSessionFactory());
		broadcaster.setNotificationType(BillConstants.MAIL_TYPE_PAYMENT_RESULT_VENDOR);
		broadcaster.broadcastNotification();
		
	}
	
	@Test
	public void testSchemePromotion() throws InterruptedException {
		BillServiceRequest request = new BillServiceRequest();
		request.setRequestType("SCHEME_NOTIFY");
		BillScheme scheme = new BillScheme();
		scheme.setId(5);
		BillNotification notification = new BillNotification();
		notification.setChannel("PHONE");
		//notification.setRecepients("ajinkyashiva@gmail.com");
		notification.setRecepients("9423040642");
		request.setNotification(notification);
		request.setScheme(scheme);
		System.out.println(adminBo.notifyCustomers(request).getResponse());
		Thread.sleep(20000);
	}

	@Test
	public void testInvoiceCount() {
		BillUser user = new BillUser();
		user.setId(1);
		BillBusiness currentBusiness = new BillBusiness();
		currentBusiness.setId(9);
		user.setCurrentBusiness(currentBusiness);
		//System.out.println(new BillInvoiceDaoImpl(getSession()).getTotalPendingCustomers(9));
		System.out.println(BillDataConverter.loadUserStats(user, getSession()).getPendingAmount());
	}
	
	@Test
	public void testBillData() {
		BillServiceRequest request = new BillServiceRequest();
		BillInvoice invoice = new BillInvoice();
		invoice.setMonth(6);
		invoice.setYear(2019);
		request.setInvoice(invoice);
		//Date startDate = CommonUtils.convertDate();
		//Date endDate = CommonUtils.convertDate();
		/*List<Object[]> monthlyBillData = new BillAdminDaoImpl(getSession()).getMonthlyBillData(1, "2019-06-01", "2019-06-30");
		System.out.println(monthlyBillData);
		for(Object[] row: monthlyBillData) {
			System.out.println(row[0] + ":" + row[1]);
		}
		*/
		BillServiceResponse monthlyData = adminBo.getMonthlyData(request);
		if(monthlyData.getItems() != null) {
			for(BillItem item: monthlyData.getItems()) {
				System.out.println(item.getName() + ":" + item.getPrice());
			}
		}
	}
	
	@Test
	public void testPdf() throws DocumentException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillBusiness business = new BillBusiness();
		business.setId(9);
		request.setBusiness(business);
		
		BillCustomerGroup customerGroup = new BillCustomerGroup();
		customerGroup.setId(1);
		request.setCustomerGroup(customerGroup);
		
		userbo.downloadPendingInvoices(request);
	}
	
	@Test
	public void testCustomerProfile() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(customerBo.loadCustomerProfile(request)));
	}
	
	@Test
	public void testRegisterCustomerProfile() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		user.setName("Ajinkya Customer");
		user.setEmail("ajinkyashiva@gmail.com");
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(customerBo.registerCustomer(request)));
	}
	
	@Test
	public void testCustomerBusinesses() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(customerBo.loadCustomerBusinesses(request)));
	}
	
	@Test
	public void testCustomerDashboard() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		request.setUser(user);
		System.out.println(new ObjectMapper().writeValueAsString(customerBo.loadCustomerDashboard(request)));
	}
	
	@Test
	public void testLoadCustomerInvoice() throws JsonGenerationException, JsonMappingException, IOException {
		BillServiceRequest request = new BillServiceRequest();
		BillUser user = new BillUser();
		user.setPhone("9423040642");
		request.setUser(user);
		BillBusiness business = new BillBusiness();
		business.setId(9);
		request.setBusiness(business);
		System.out.println(new ObjectMapper().writeValueAsString(customerBo.loadInvoice(request)));
	}
}
