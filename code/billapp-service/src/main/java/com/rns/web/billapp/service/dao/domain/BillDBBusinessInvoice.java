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

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "business_invoices")
@DynamicUpdate
public class BillDBBusinessInvoice {
	
	private Integer id;
	private BillDBUserBusiness toBusiness;
	private BillDBUserBusiness fromBusiness;
	private BigDecimal amount;
	private BigDecimal pendingBalance;
	private String comments;
	private String status;
	private Date paidDate;
	private Date createdDate;
	private String paymentId;
	private String paymentType;
	private Set<BillDBItemBusinessInvoice> items = new HashSet<BillDBItemBusinessInvoice>();
	private BigDecimal paidAmount;
	private String paymentMedium;
	private String paymentMode;
	private Integer paymentAttempt;
	private Date invoiceDate;
	
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
	
	@Column(name = "pending_balance")
	public BigDecimal getPendingBalance() {
		return pendingBalance;
	}
	public void setPendingBalance(BigDecimal pendingBalance) {
		this.pendingBalance = pendingBalance;
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
	public Set<BillDBItemBusinessInvoice> getItems() {
		return items;
	}
	public void setItems(Set<BillDBItemBusinessInvoice> items) {
		this.items = items;
	}
	
	@Column(name = "paid_amount")
	public BigDecimal getPaidAmount() {
		return paidAmount;
	}
	public void setPaidAmount(BigDecimal paidAmount) {
		this.paidAmount = paidAmount;
	}
	
	@Column(name = "payment_medium")
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
	
	@Column(name = "payment_attempts")
	public Integer getPaymentAttempt() {
		return paymentAttempt;
	}
	public void setPaymentAttempt(Integer paymentAttempt) {
		this.paymentAttempt = paymentAttempt;
	}
	
	@Column(name = "invoice_date")
	public Date getInvoiceDate() {
		return invoiceDate;
	}
	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
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
	@JoinColumn(name = "from_business")
	public BillDBUserBusiness getFromBusiness() {
		return fromBusiness;
	}
	public void setFromBusiness(BillDBUserBusiness fromBusiness) {
		this.fromBusiness = fromBusiness;
	}
	

}
