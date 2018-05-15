package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.Date;

public class BillSubscription {
	
	private Integer id;
	private BigDecimal serviceCharge;
	private Date createdDate;
	private String status;
	private BillLocation area;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public BigDecimal getServiceCharge() {
		return serviceCharge;
	}
	public void setServiceCharge(BigDecimal serviceCharge) {
		this.serviceCharge = serviceCharge;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public BillLocation getArea() {
		return area;
	}
	public void setArea(BillLocation area) {
		this.area = area;
	}
	
}
