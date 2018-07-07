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
@Table(name = "invoices")
public class BillDBInvoice {
	
	private Integer id;
	private BillDBSubscription subscription;
	private BigDecimal amount;
	private Integer month;
	private Integer year;
	private BigDecimal pendingBalance;
	private BigDecimal creditBalance;
	private BigDecimal discount;
	private String comments;
	private String status;
	private Date paidDate;
	private Date createdDate;
	private String paymentId;
	private String paymentRequestId;
	private String paymentType;
	private Set<BillDBItemInvoice> items = new HashSet<BillDBItemInvoice>();
	private BigDecimal paidAmount;
	private BigDecimal serviceCharge;
	private String paymentMedium;
	private String paymentMode;
	
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
	
	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
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
	
	@Column(name = "pending_balance")
	public BigDecimal getPendingBalance() {
		return pendingBalance;
	}
	public void setPendingBalance(BigDecimal pendingBalance) {
		this.pendingBalance = pendingBalance;
	}
	
	@Column(name = "credit_balance")
	public BigDecimal getCreditBalance() {
		return creditBalance;
	}
	public void setCreditBalance(BigDecimal creditBalance) {
		this.creditBalance = creditBalance;
	}
	
	@Column(name = "discount")
	public BigDecimal getDiscount() {
		return discount;
	}
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}
	
	@Column(name = "comments")
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "paid_date")
	public Date getPaidDate() {
		return paidDate;
	}
	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
	}
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
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
	
	@OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
	public Set<BillDBItemInvoice> getItems() {
		return items;
	}
	public void setItems(Set<BillDBItemInvoice> items) {
		this.items = items;
	}
	
	@Column(name = "payment_request_id")
	public String getPaymentRequestId() {
		return paymentRequestId;
	}
	public void setPaymentRequestId(String paymentRequestId) {
		this.paymentRequestId = paymentRequestId;
	}
	
	@Column(name = "paid_amount")
	public BigDecimal getPaidAmount() {
		return paidAmount;
	}
	public void setPaidAmount(BigDecimal paidAmount) {
		this.paidAmount = paidAmount;
	}
	
	@Column(name = "service_charge")
	public BigDecimal getServiceCharge() {
		return serviceCharge;
	}
	public void setServiceCharge(BigDecimal serviceCharge) {
		this.serviceCharge = serviceCharge;
	}
	
	@Column(name = "payment_partner")
	public String getPaymentMedium() {
		return paymentMedium;
	}
	public void setPaymentMedium(String paymentMedium) {
		this.paymentMedium = paymentMedium;
	}
	
	@Column(name = "payment_mode")
	public String getPaymentMode() {
		return paymentMode;
	}
	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}
	

}
