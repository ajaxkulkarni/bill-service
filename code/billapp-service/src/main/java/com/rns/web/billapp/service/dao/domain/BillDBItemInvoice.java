package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "invoice_items")
public class BillDBItemInvoice {
	
	private Integer id;
	private Date createdDate;
	private String status;
	private BigDecimal quantity;
	private BigDecimal price;
	private BillDBInvoice invoice;
	private BillDBItemBusiness businessItem;
	private BillDBItemSubscription subscribedItem;
	
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "quantity")
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	@Column(name = "amount")
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice")
	public BillDBInvoice getInvoice() {
		return invoice;
	}
	public void setInvoice(BillDBInvoice invoice) {
		this.invoice = invoice;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "business_item")
	public BillDBItemBusiness getBusinessItem() {
		return businessItem;
	}
	public void setBusinessItem(BillDBItemBusiness businessItem) {
		this.businessItem = businessItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "subscribed_item")
	public BillDBItemSubscription getSubscribedItem() {
		return subscribedItem;
	}
	public void setSubscribedItem(BillDBItemSubscription subscribedItem) {
		this.subscribedItem = subscribedItem;
	}
	
}
