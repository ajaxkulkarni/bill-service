package com.rns.web.billapp.service.dao.domain;

import java.util.Map;

public class BillMyCriteria {
	
	private String associationPath;
	private Map<String, Object> restrictions;
	
	public BillMyCriteria() {
	}

	public BillMyCriteria(String associationPath, Map<String, Object> keys) {
		setAssociationPath(associationPath);
		setRestrictions(keys);
	}
	
	public String getAssociationPath() {
		return associationPath;
	}
	public void setAssociationPath(String associationPath) {
		this.associationPath = associationPath;
	}
	public Map<String, Object> getRestrictions() {
		return restrictions;
	}
	public void setRestrictions(Map<String, Object> restrictions) {
		this.restrictions = restrictions;
	}
	
	

}
