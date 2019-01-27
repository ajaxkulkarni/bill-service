package com.rns.web.billapp.service.bo.domain;

import java.math.BigInteger;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillBusinessSummary {

	private BigInteger onlinePaidCount;
	private BigInteger customerCount;
	private BillUser user;
	
	public BigInteger getOnlinePaidCount() {
		return onlinePaidCount;
	}
	public void setOnlinePaidCount(BigInteger onlinePaidCount) {
		this.onlinePaidCount = onlinePaidCount;
	}
	public BigInteger getCustomerCount() {
		return customerCount;
	}
	public void setCustomerCount(BigInteger customerCount) {
		this.customerCount = customerCount;
	}
	public BillUser getUser() {
		return user;
	}
	public void setUser(BillUser user) {
		this.user = user;
	}
	
	
	
	
}
