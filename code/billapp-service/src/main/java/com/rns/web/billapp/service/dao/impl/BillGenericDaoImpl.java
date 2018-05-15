package com.rns.web.billapp.service.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.rns.web.billapp.service.dao.domain.BillDBUser;
import com.rns.web.billapp.service.dao.domain.BillDBUserBusiness;
import com.rns.web.billapp.service.util.BillConstants;

public class BillGenericDaoImpl {
	
	private Session session;
	
	public BillGenericDaoImpl(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}
	
	public <T> T getEntityByKey(Class<T> type, String key, Object value, boolean activeEntity) {
		 Criteria criteria = session.createCriteria(type).add(Restrictions.eq(key, value));
		 if(activeEntity) {
			 criteria.add(activeCriteria());
		 }
         Object result = criteria.uniqueResult();
         if(result != null) {
        	 return (T) result;
         }
         return null;
	}

	private SimpleExpression activeCriteria() {
		return Restrictions.eq("status", BillConstants.STATUS_ACTIVE);
	}


}
