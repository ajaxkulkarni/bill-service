package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillScheme {
	
	private Integer id;
	private BillItem itemBusiness;
	private BillItem itemParent;
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
	private BigDecimal amountPaid;
	private String paymentStatus;
	private String paymentId;
	private String couponCode;
	private String status;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public BillItem getItemBusiness() {
		return itemBusiness;
	}
	public void setItemBusiness(BillItem itemBusiness) {
		this.itemBusiness = itemBusiness;
	}
	public BillItem getItemParent() {
		return itemParent;
	}
	public void setItemParent(BillItem itemParent) {
		this.itemParent = itemParent;
	}
	public String getSchemeType() {
		return schemeType;
	}
	public void setSchemeType(String schemeType) {
		this.schemeType = schemeType;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public BigDecimal getDiscount() {
		return discount;
	}
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}
	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}
	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}
	public Date getValidTill() {
		return validTill;
	}
	public void setValidTill(Date validTill) {
		this.validTill = validTill;
	}
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	public String getSchemeName() {
		return schemeName;
	}
	public void setSchemeName(String schemeName) {
		this.schemeName = schemeName;
	}
	public String getSchemeCode() {
		return schemeCode;
	}
	public void setSchemeCode(String schemeCode) {
		this.schemeCode = schemeCode;
	}
	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	public BigDecimal getVendorCommission() {
		return vendorCommission;
	}
	public void setVendorCommission(BigDecimal vendorCommission) {
		this.vendorCommission = vendorCommission;
	}
	public String getCommissionPaidType() {
		return commissionPaidType;
	}
	public void setCommissionPaidType(String commissionPaidType) {
		this.commissionPaidType = commissionPaidType;
	}
	public BigDecimal getAmountPaid() {
		return amountPaid;
	}
	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}
	public String getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	public String getCouponCode() {
		return couponCode;
	}
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

}
