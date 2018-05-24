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
@Table(name = "order_items")
public class BillDBOrderItems {
	
	private Integer id;
	private BigDecimal amount;
	private BigDecimal quantity;
	private String status;
	private Date createdDate;
	private BillDBItemBusiness businessItem;
	private BillDBItemSubscription subscribedItem;
	private BillDBOrders order;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	@Column(name = "quantity")
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "business_item_id")
	public BillDBItemBusiness getBusinessItem() {
		return businessItem;
	}
	public void setBusinessItem(BillDBItemBusiness businessItem) {
		this.businessItem = businessItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "subscribed_item_id")
	public BillDBItemSubscription getSubscribedItem() {
		return subscribedItem;
	}
	public void setSubscribedItem(BillDBItemSubscription subscribedItem) {
		this.subscribedItem = subscribedItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	public BillDBOrders getOrder() {
		return order;
	}
	public void setOrder(BillDBOrders order) {
		this.order = order;
	}
	

}
