package com.rns.web.billapp.service.util;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;

public class BillNameSorter implements Comparator<Object> {

	public int compare(Object o1, Object o2) {
		if(o1 instanceof BillItem) {
			BillItem item1 = (BillItem) o1;
			BillItem item2 = (BillItem) o2;
			if(item1.getName() != null && item2.getName() != null) {
				return item1.getName().compareToIgnoreCase(item2.getName());
			}
			if(item1.getParentItem() != null && item2.getParentItem() != null && item1.getParentItem().getName() != null && item2.getParentItem().getName() != null) {
				return item1.getParentItem().getName().compareToIgnoreCase(item2.getParentItem().getName());
			}
		} else if (o1 instanceof BillUser) {
			BillUser user1 = (BillUser) o1;
			BillUser user2 = (BillUser) o2;
			if(user1.getName() != null && user2.getName() != null) {
				return user1.getName().compareToIgnoreCase(user2.getName());
			}
		}
		return 0;
	}

}
