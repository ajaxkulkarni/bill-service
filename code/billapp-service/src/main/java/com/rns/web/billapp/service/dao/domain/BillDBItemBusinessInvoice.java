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
@Table(name = "business_invoice_items")
public class BillDBItemBusinessInvoice {
	
	private Integer id;
	private Date createdDate;
	private String status;
	private BigDecimal quantity;
	private BigDecimal price;
	private BillDBBusinessInvoice invoice;
	private BillDBItemBusiness fromBusinessItem;
	private BillDBItemBusiness toBusinessItem;
	private BigDecimal unitSellingPrice;
	
	
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
	
	@Column(name = "price")
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice")
	public BillDBBusinessInvoice getInvoice() {
		return invoice;
	}
	public void setInvoice(BillDBBusinessInvoice invoice) {
		this.invoice = invoice;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "from_business_item")
	public BillDBItemBusiness getFromBusinessItem() {
		return fromBusinessItem;
	}
	
	public void setFromBusinessItem(BillDBItemBusiness fromBusinessItem) {
		this.fromBusinessItem = fromBusinessItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "to_business_item")
	public BillDBItemBusiness getToBusinessItem() {
		return toBusinessItem;
	}
	
	public void setToBusinessItem(BillDBItemBusiness toBusinessItem) {
		this.toBusinessItem = toBusinessItem;
	}
	
	@Column(name = "selling_price")
	public BigDecimal getUnitSellingPrice() {
		return unitSellingPrice;
	}
	public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
		this.unitSellingPrice = unitSellingPrice;
	}
	
}
