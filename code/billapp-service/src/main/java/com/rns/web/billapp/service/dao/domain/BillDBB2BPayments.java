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
@Table(name = "b2b_payments")
public class BillDBB2BPayments {
	
	private Integer id;
	private BillDBUserBusiness fromBusiness;
	private BillDBUserBusiness toBusiness;
	private BillDBSchemes scheme;
	private BillDBCustomerCoupons coupon;
	private BigDecimal amountPaid;
	private String paymentStatus;
	private String paymentType;
	private Date paidDate;
	private String paymentId;
	private Integer month;
	private Integer year;
	private Date date;
	
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
	@JoinColumn(name = "from_business")
	public BillDBUserBusiness getFromBusiness() {
		return fromBusiness;
	}
	public void setFromBusiness(BillDBUserBusiness fromBusiness) {
		this.fromBusiness = fromBusiness;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "to_business")
	public BillDBUserBusiness getToBusiness() {
		return toBusiness;
	}
	public void setToBusiness(BillDBUserBusiness toBusiness) {
		this.toBusiness = toBusiness;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme")
	public BillDBSchemes getScheme() {
		return scheme;
	}
	public void setScheme(BillDBSchemes scheme) {
		this.scheme = scheme;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "coupon")
	public BillDBCustomerCoupons getCoupon() {
		return coupon;
	}
	public void setCoupon(BillDBCustomerCoupons coupon) {
		this.coupon = coupon;
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
	
	@Column(name = "payment_type")
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	
	@Column(name = "paid_date")
	public Date getPaidDate() {
		return paidDate;
	}
	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
	}
	
	@Column(name = "payment_id")
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	
	@Column(name = "month")
	public Integer getMonth() {
		return month;
	}
	public void setMonth(Integer month) {
		this.month = month;
	}
	
	@Column(name = "year")
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	
	@Column(name = "date")
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

}
