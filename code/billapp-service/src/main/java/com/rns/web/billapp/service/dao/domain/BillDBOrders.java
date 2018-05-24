package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
public class BillDBOrders {
	
	private Integer id;
	private BigDecimal amount;
	private Date orderDate;
	private Date createdDate;
	private String status;
	private BillDBUserBusiness business;
	private BillDBSubscription subscription;
	private Set<BillDBOrderItems> orderItems = new HashSet<BillDBOrderItems>();
	
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
	
	@Column(name = "order_date")
	public Date getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
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
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "business")
	public BillDBUserBusiness getBusiness() {
		return business;
	}
	public void setBusiness(BillDBUserBusiness business) {
		this.business = business;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription")
	public BillDBSubscription getSubscription() {
		return subscription;
	}
	public void setSubscription(BillDBSubscription subscription) {
		this.subscription = subscription;
	}
	
	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	public Set<BillDBOrderItems> getOrderItems() {
		return orderItems;
	}
	public void setOrderItems(Set<BillDBOrderItems> orderItems) {
		this.orderItems = orderItems;
	}
	

}
