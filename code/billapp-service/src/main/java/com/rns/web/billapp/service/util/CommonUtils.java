package com.rns.web.billapp.service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;

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

	public static Date getFirstDate(Integer year, Integer month) {
		if(year == null || month == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, 1);
		return cal.getTime();
	}
	
	public static Date getLastDate(Integer year, Integer month) {
		if(year == null || month == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal.getTime();
	}

	public static Integer getCalendarValue(Date date1, int value) {
		if(date1 == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date1);
		return cal.get(value);
	}

	public static String getStringValue(Integer value) {
		if(value == null) {
			return "";
		}
		return value.toString();
	}

	public static String getStringValue(BigDecimal value) {
		if(value == null) {
			return "";
		}
		return value.toString();
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
	
	public static Date getMonthFirstDate() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, 1);
		return c.getTime();
	}
	
	public static Date getMonthLastDate() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
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
	
}
