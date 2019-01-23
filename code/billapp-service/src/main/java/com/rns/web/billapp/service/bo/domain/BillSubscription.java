package com.rns.web.billapp.service.bo.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillSubscription implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2123383471370181455L;
	private Integer id;
	private BigDecimal serviceCharge;
	private Date createdDate;
	private String status;
	private BillLocation area;
	private List<BillItem> items;
	private BigDecimal amount;
	private BigDecimal quantity;
	private Integer billsDue;
	private Date lastBillPaid;
	private BillCustomerGroup group;
	
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
	public List<BillItem> getItems() {
		return items;
	}
	public void setItems(List<BillItem> items) {
		this.items = items;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	public Integer getBillsDue() {
		return billsDue;
	}
	public void setBillsDue(Integer billsDue) {
		this.billsDue = billsDue;
	}
	public Date getLastBillPaid() {
		return lastBillPaid;
	}
	public void setLastBillPaid(Date lastBillPaid) {
		this.lastBillPaid = lastBillPaid;
	}
	public BillCustomerGroup getGroup() {
		return group;
	}
	public void setGroup(BillCustomerGroup group) {
		this.group = group;
	}
	
}
