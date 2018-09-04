package com.rns.web.billapp.service.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class BillLogAppender extends AppenderSkeleton {

	public void close() {
		
	}

	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		System.out.println("Bill Log ... " +  event.getTimeStamp() + " -- "+ event.getMessage());
	}

}
