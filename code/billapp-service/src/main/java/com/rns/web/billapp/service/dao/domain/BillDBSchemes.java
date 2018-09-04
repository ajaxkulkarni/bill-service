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
@Table(name = "schemes")
public class BillDBSchemes {
	
	private Integer id;
	private BillDBUserBusiness business;
	private BillDBItemBusiness businessItem;
	private BillDBItemParent parentItem;
	private BillDBSector sector;
	private String schemeType;
	private BigDecimal price;
	private BigDecimal discount;
	private BigDecimal originalPrice;
	private String comments;
	private Date validFrom;
	private Date validTill;
	private String paymentType;
	private String schemeName;
	private String schemeCode;
	private Integer duration;
	private BigDecimal vendorCommission;
	private String commissionPaidType;
	private String status;
	private Date createdDate;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
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
	@JoinColumn(name = "business_item")
	public BillDBItemBusiness getBusinessItem() {
		return businessItem;
	}
	public void setBusinessItem(BillDBItemBusiness businessItem) {
		this.businessItem = businessItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_item")
	public BillDBItemParent getParentItem() {
		return parentItem;
	}
	public void setParentItem(BillDBItemParent parentItem) {
		this.parentItem = parentItem;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "sector")
	public BillDBSector getSector() {
		return sector;
	}
	public void setSector(BillDBSector sector) {
		this.sector = sector;
	}
	
	@Column(name = "scheme_type")
	public String getSchemeType() {
		return schemeType;
	}
	public void setSchemeType(String schemeType) {
		this.schemeType = schemeType;
	}
	
	@Column(name = "price")
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@Column(name = "discount")
	public BigDecimal getDiscount() {
		return discount;
	}
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}
	
	@Column(name = "original_price")
	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}
	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}
	
	@Column(name = "comments")
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	@Column(name = "valid_from")
	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}
	
	@Column(name = "valid_till")
	public Date getValidTill() {
		return validTill;
	}
	public void setValidTill(Date validTill) {
		this.validTill = validTill;
	}
	
	@Column(name = "payment_type")
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	
	@Column(name = "scheme_name")
	public String getSchemeName() {
		return schemeName;
	}
	public void setSchemeName(String schemeName) {
		this.schemeName = schemeName;
	}
	
	@Column(name = "scheme_code")
	public String getSchemeCode() {
		return schemeCode;
	}
	public void setSchemeCode(String schemeCode) {
		this.schemeCode = schemeCode;
	}
	
	@Column(name = "duration")
	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	
	@Column(name = "vendor_commission")
	public BigDecimal getVendorCommission() {
		return vendorCommission;
	}
	public void setVendorCommission(BigDecimal vendorCommission) {
		this.vendorCommission = vendorCommission;
	}
	
	@Column(name = "commission_paid")
	public String getCommissionPaidType() {
		return commissionPaidType;
	}
	public void setCommissionPaidType(String commissionPaidType) {
		this.commissionPaidType = commissionPaidType;
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
	
}
