package com.rns.web.billapp.service.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillUser;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillServiceRequest {
	
	private BillUser user;
	private BillBusiness business;
	private BillItem item;
	
	public BillUser getUser() {
		return user;
	}

	public void setUser(BillUser user) {
		this.user = user;
	}

	public BillBusiness getBusiness() {
		return business;
	}

	public void setBusiness(BillBusiness business) {
		this.business = business;
	}

	public BillItem getItem() {
		return item;
	}

	public void setItem(BillItem item) {
		this.item = item;
	}

}
