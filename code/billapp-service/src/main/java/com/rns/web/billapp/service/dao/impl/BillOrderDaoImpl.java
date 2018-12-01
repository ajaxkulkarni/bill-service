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
	
	public List<BillDBOrderItems> getOrderItems(Date date, Date toDate, Integer parentItemId, String priceType) {
		String queryString = "from BillDBOrderItems where order.status=:active AND quantity>0 AND businessItem.parent.id=:parentId";
		if(priceType != null) {
			queryString = queryString + " AND subscribedItem.priceType=:priceType";
		}
		if(toDate == null) {
			queryString = queryString + " AND order.orderDate=:date"; 
		} else {
			queryString = queryString + " AND order.orderDate>=:date AND order.orderDate<=:toDate"; 
		}
		Query query = session.createQuery(queryString);
		query.setDate("date", date);
		query.setInteger("parentId", parentItemId);
		query.setString("active", BillConstants.STATUS_ACTIVE);
		if(priceType != null) {
			query.setString("priceType", priceType);
		}
		if(toDate != null) {
			query.setDate("toDate", toDate);
		}
		return query.list();
	}
	
}
