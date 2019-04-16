package com.rns.web.billapp.service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;

import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.dao.domain.BillDBCustomerCoupons;
import com.rns.web.billapp.service.dao.domain.BillDBSchemes;

public class CommonUtils {
	
	
	public static void closeSession(Session session) {
		if(session == null || !session.isOpen())  {
			return;
		}
		session.close();
		//System.out.println("Session closed!");
	}
	

	public static String convertDate(Date date) {
		try {
			return new SimpleDateFormat(BillConstants.DATE_FORMAT).format(date);
		} catch (Exception e) {
		}
		return null;
	}
	
	public static Date convertDate(String date) {
		try {
			return new SimpleDateFormat(BillConstants.DATE_FORMAT).parse(date);
		} catch (Exception e) {
		}
		return null;
	}
	
	public static String readFile(String contentPath) throws FileNotFoundException {
		File file = getFile(contentPath);
		Scanner scanner = new Scanner(file);
		StringBuilder result = new StringBuilder();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			result.append(line).append("\n");
		}

		scanner.close();
		return result.toString();
	}

	public static File getFile(String contentPath) {
		ClassLoader classLoader = new CommonUtils().getClass().getClassLoader();
		URL resource = classLoader.getResource(contentPath);
		File file = new File(resource.getFile());
		return file;
	}
	
	
	public static String getStringValue(String value) {
		return StringUtils.isNotEmpty(value) ? value : "";
	}


	public static String getFileName(String filePath) {
		String[] tokens = StringUtils.split(filePath, "/");
		if(tokens == null || tokens.length == 0) {
			return null;
		}
		return tokens[tokens.length - 1];
	}

	
	public static boolean isAmountPresent(BigDecimal amount) {
		if(amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
			return true;
		}
		return false;
	}


	public static Integer getCalendarValue(Date date1, int value) {
		if(date1 == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date1);
		int result = cal.get(value);
		if(Calendar.MONTH == value) {
			result++;
		}
		return result;
	}

	public static String getStringValue(Integer value) {
		if(value == null) {
			return "";
		}
		return value.toString();
	}

	public static String getStringValue(BigDecimal value, boolean stripZeroes) {
		if(value == null) {
			return "";
		}
		if(!stripZeroes) {
			return value.toString();
		}
		return value.stripTrailingZeros().toPlainString();
	}

	public static String getDate(Date date) {
		if(date == null) {
			return null;
		}
		try {
			return new SimpleDateFormat(BillConstants.DATE_FORMAT).format(date);
		} catch (Exception e) {
			
		}
		return null;
	}
	
	public static int writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) throws IOException {

		OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
		int read = 0;
		byte[] bytes = new byte[1024];
		int size = 0;
		out = new FileOutputStream(new File(uploadedFileLocation));
		while ((read = uploadedInputStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
			size = size + read;
		}
		out.flush();
		out.close();
		return size;
	}

	public static String getFileType(String filePath) {
		if(StringUtils.isNotBlank(filePath)) {
			String[] values = StringUtils.split(filePath, ".");
			if(values != null && values.length > 0) {
				return values[values.length - 1];
			}
		}
		return null;
	}

	public static Date getWeekFirstDate() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
		return c.getTime();
	}
	
	public static Date getWeekLastDate() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		return c.getTime();
	}
	
	public static Date getMonthFirstDate(Integer month, Integer year) {
		Calendar c = Calendar.getInstance();
		if(month > 12) {
			month = 1;
		}
		if(month != null) {
			c.set(Calendar.MONTH, month - 1);
		}
		if(year != null) {
			c.set(Calendar.YEAR, year);
		}
		c.set(Calendar.DAY_OF_MONTH, 1);
		setZero(c);
		return c.getTime();
	}
	
	public static Date getMonthLastDate(Integer month,  Integer year) {
		Calendar c = Calendar.getInstance();
		if(month != null) {
			c.set(Calendar.MONTH, month - 1);
		}
		if(year != null) {
			c.set(Calendar.YEAR, year);
		}
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
		setZero(c);
		return c.getTime();
	}
	
	public static boolean isSameDate(Date date1, Date date2) {
		if(date1 == null || date2 == null) {
			return false;
		}
		return DateUtils.isSameDay(date1, date2);
	}
	
	public static boolean isGreaterThan(Date date1, Date date2) {
		if(date1 == null || date2 == null) {
			return false;
		}
		return date1.compareTo(date2) >= 0;
	}


	public static Date startDate(Date date) {
		if(date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		setZero(cal);
		return cal.getTime();
	}


	public static void setZero(Calendar cal) {
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}
	
	public static Date setZero(Date date) {
		if(date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		setZero(cal);
		return cal.getTime();
	}
	
	public static Date endDate(Date date) {
		if(date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, 1);
		setZero(cal);
		return cal.getTime();
	}


	public static String convertDate(Date date, String dateFormat) {
		if(date == null) {
			return "";
		}
		try {
			return new SimpleDateFormat(dateFormat).format(date);
		} catch (Exception e) {
		}
		return "";
	}


	public static String encode(String string) throws UnsupportedEncodingException {
		return URLEncoder.encode(string, "UTF-8");
	}


	public static long noOfDays(Date date, Date date2) {
		if(date == null || date2 == null) {
			return 0;
		}
		return (date.getTime() - date2.getTime())/(1000*60*60*24);
	}


	public static Date addToDate(Date date, int field, Integer duration) {
		if(date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(field, duration);
		return cal.getTime();
	}


	public static String generateCouponCode(BillDBSchemes schemes, BillDBCustomerCoupons coupons) {
		if(schemes == null || coupons == null) {
			return "";
		}
		if(StringUtils.isBlank(schemes.getSchemeCode()) || coupons.getId() == null) {
			return  "";
		}
		String couponId = StringUtils.reverse(coupons.getId().toString());
		couponId = StringUtils.substring(couponId, 0, 6); 
		couponId = StringUtils.leftPad(couponId, 6, "0");
		Long randomValue = Math.round(Math.random()*100);
		couponId = StringUtils.replaceOnce(couponId, "00", randomValue.toString());
		return schemes.getSchemeCode() + couponId;
	}
	
	public static BigDecimal getAmount(Object value) {
		BigDecimal amount = (BigDecimal) value;
		if(amount != null) {
			return amount;
		}
		return BigDecimal.ZERO;
	}
	
	public static String getString(Object value) {
		if(value != null) {
			return value.toString();
		}
		return null;
	}
	
	public static <T> T getValue(Object value, Class<T> type) {
		if(value != null) {
			return (T) value;
		}
		return null;
	}
	
	public static BigDecimal formatDecimal(BigDecimal value) {
		if(value == null) {
			return value;
		}
		//value = value.round(new MathContext(1, RoundingMode.HALF_UP));
		value = value.setScale(2, RoundingMode.HALF_EVEN);
		return value.stripTrailingZeros();
	}
	
	public static boolean comparePhoneNumbers(String ph1, String ph2) {
		if(StringUtils.isBlank(ph2) || StringUtils.isBlank(ph1)) {
			return false;
		}
		String phone1 = new String(StringUtils.removeStart(StringUtils.replacePattern(ph1, "\\s+",""), "+91"));
		String phone2 = new String(StringUtils.removeStart(StringUtils.replacePattern(ph2, "\\s+",""), "+91"));
		return StringUtils.equals(phone1, phone2);
	}


	public static Date getDate(Integer month, Integer year) {
		if(month == null || year == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.YEAR, year);
		return cal.getTime();
	}


	public static void addIfNotPresent(BillUser customer, List<BillUser> users) {
		if(customer == null) {
			return;
		}
		if(CollectionUtils.isEmpty(users)) {
			users = new ArrayList<BillUser>();
			users.add(customer);
			return;
		}
		for(BillUser user: users) {
			if(user.getId().intValue() == customer.getId().intValue()) {
				return;
			}
		}
		users.add(customer);
	}


	public static String trimPhoneNumber(String phone) {
		if(StringUtils.isBlank(phone)) {
			return phone;
		}
		phone = StringUtils.replacePattern(phone, "\\s+", "");
		phone = StringUtils.trimToEmpty(phone);
		phone = StringUtils.substring(phone, phone.length() - 10, phone.length());
		return phone;
	}
	
	public static Integer getMonthDays(Integer month) {
		if(month == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, month - 1);
		return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
	}
}
