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
@Table(name = "customer_coupons")
public class BillDBCustomerCoupons {
	
	private Integer id;
	private BillDBSubscription subscription;
	private BillDBUserBusiness business;
	private BillDBSchemes scheme;
	private BigDecimal amountPaid;
	private String paymentStatus;
	private String paymentId;
	private String paymentType;
	private String phone;
	private String email;
	private String name;
	private String address;
	private Date acceptedDate;
	private Date validFrom;
	private Date validTill;
	private String couponCode;
	private String status;
	private BillDBInvoice invoice;
	
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
	@JoinColumn(name = "subscription")
	public BillDBSubscription getSubscription() {
		return subscription;
	}
	public void setSubscription(BillDBSubscription subscription) {
		this.subscription = subscription;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor")
	public BillDBUserBusiness getBusiness() {
		return business;
	}
	public void setBusiness(BillDBUserBusiness business) {
		this.business = business;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme")
	public BillDBSchemes getScheme() {
		return scheme;
	}
	public void setScheme(BillDBSchemes scheme) {
		this.scheme = scheme;
	}
	
	@Column(name = "amount_paid")
	public BigDecimal getAmountPaid() {
		return amountPaid;
	}
	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}
	
	@Column(name = "payment_status")
	public String getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	
	@Column(name = "payment_id")
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	
	@Column(name = "payment_type")
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	
	@Column(name = "phone")
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	@Column(name = "email")
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "address")
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	@Column(name = "accepted_date")
	public Date getAcceptedDate() {
		return acceptedDate;
	}
	public void setAcceptedDate(Date acceptedDate) {
		this.acceptedDate = acceptedDate;
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
	
	@Column(name = "coupon_code")
	public String getCouponCode() {
		return couponCode;
	}
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice")
	public BillDBInvoice getInvoice() {
		return invoice;
	}
	public void setInvoice(BillDBInvoice invoice) {
		this.invoice = invoice;
	}
	
	
}
