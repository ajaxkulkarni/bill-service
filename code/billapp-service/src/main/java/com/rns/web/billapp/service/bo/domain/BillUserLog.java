package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.Date;

public class BillUserLog {
	
	private Date fromDate;
	private Date toDate;
	private String changeType;
	private BigDecimal priceChange;
	private BigDecimal quantityChange;
	private Date createdDate;
	
	public Date getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public String getChangeType() {
		return changeType;
	}
	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}
	public BigDecimal getPriceChange() {
		return priceChange;
	}
	public void setPriceChange(BigDecimal priceChange) {
		this.priceChange = priceChange;
	}
	public BigDecimal getQuantityChange() {
		return quantityChange;
	}
	public void setQuantityChange(BigDecimal quantityChange) {
		this.quantityChange = quantityChange;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	

}
