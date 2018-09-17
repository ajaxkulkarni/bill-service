package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.rns.web.billapp.service.dao.domain.BillDBOrderItems;
import com.rns.web.billapp.service.util.BillConstants;

public class BillOrderDaoImpl {

	private Session session;

	public BillOrderDaoImpl(Session session) {
		this.session = session;
	}
	
	public List<BillDBOrderItems> getOrderItems(Date date, Integer parentItemId) {
		Query query = session.createQuery("from BillDBOrderItems where order.orderDate=:date AND order.status=:active AND quantity>0 AND businessItem.parent.id=:parentId");
		query.setDate("date", date);
		query.setInteger("parentId", parentItemId);
		query.setString("active", BillConstants.STATUS_ACTIVE);
		return query.list();
	}
	
}
