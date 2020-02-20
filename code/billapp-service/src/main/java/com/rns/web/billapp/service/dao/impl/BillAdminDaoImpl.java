package com.rns.web.billapp.service.dao.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.util.BillConstants;
import com.rns.web.billapp.service.util.CommonUtils;

public class BillAdminDaoImpl {

	private Session session;
	
	public BillAdminDaoImpl(Session session) {
		this.session = session;
	}

	public List<BillDBUserBusiness> getBusinesses(Integer sectorId) {
		Criteria criteria = session.createCriteria(BillDBUserBusiness.class);
		criteria.createCriteria("user", JoinType.INNER_JOIN);
		Criteria sectorCriteria = criteria.createCriteria("sector", JoinType.LEFT_OUTER_JOIN);
		if(sectorId != null) {
			sectorCriteria.add(Restrictions.eq("id", sectorId));
		}
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return criteria.list();
	}
	
	public List<Object[]> getVendorProgressSummary(Session session) {
		String queryString = "select user_business.id,users.full_name,phone,user_business.name,customerCount.cnt,maxPaid.pd, onlinePaid.onlineCount,user_business.created_date,reminders.noOfReminders,user_business.type from "
				+ " users join user_business on users.id = user_business.user "
				+ " left join (select count(*) as cnt, business from subscriptions group by business) as customerCount on user_business.id = customerCount.business "
				+ " left join (select max(paid_date) as pd, subscriptions.business as business from invoices join subscriptions on invoices.subscription = subscriptions.id group by subscriptions.business) as maxPaid on maxPaid.business = user_business.id "
				+ " left join (select sum(no_of_reminders) as noOfReminders, subscriptions.business as business from invoices join subscriptions on invoices.subscription = subscriptions.id group by subscriptions.business) as reminders on reminders.business = user_business.id "
				+ " left join (select count(*) as onlineCount, subscriptions.business as business from invoices join subscriptions on invoices.subscription = subscriptions.id where invoices.payment_type=:online group by subscriptions.business) as onlinePaid on onlinePaid.business = user_business.id "
				+ " where user_business.sector=:sectorId AND users.status=:active" + " order by customerCount.cnt asc ";

		Query query = session.createSQLQuery(queryString);
		query.setInteger("sectorId", 4);//4 for production
		query.setString("active", BillConstants.STATUS_ACTIVE);
		query.setString("online", BillConstants.PAYMENT_ONLINE);
		return query.list();
	}
	
	public List<Object[]> getMonthlyBillData(Integer daysOfMonth, String startDate, String endDate) {
		String queryString = "select count(*) , q.billTotal, q.parent_name , q.parent_id from (" +
 "select count(*) as orderCnt,sum(order_items.amount) as billTotal,item_parent.name as parent_name, item_parent.id as parent_id,order_items.subscribed_item_id from order_items join business_items on order_items.business_item_id = business_items.id " +
 "join item_parent on business_items.item_parent = item_parent.id " +
 "where order_items.created_date > :startDate AND order_items.created_date < :endDate AND order_items.amount > 0 " + 
 "group by item_parent.id,order_items.subscribed_item_id having count(*) > :days) as q group by parent_id, billTotal";

		Query query = session.createSQLQuery(queryString);
		query.setInteger("days", daysOfMonth);
		query.setString("startDate", startDate);
		query.setString("endDate", endDate);
		return query.list();
	}

}
