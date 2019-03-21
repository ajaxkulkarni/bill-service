package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillInvoice {
	
	private Integer id;
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
	private List<BillItem> invoiceItems;
	private BigDecimal serviceCharge;
	private BigDecimal payable;
	private BigDecimal internetFees;
	private String paymentUrl;
	private BigDecimal paidAmount;
	private String hdfcPaymentUrl;
	private String hdfcRequest;
	private String hdfcAccessCode;
	private String paymentMedium;
	private String paymentMode;
	private String paymentMessage;
	private String atomPaymentUrl;
	private String cashfreeSignature;
	private String cashFreeRedirectUrl;
	private String cashFreeAppId;
	private String cashFreePaymentUrl;
	private String cashFreeTxId;
	private BigDecimal outstandingBalance;
	private String paymentResponse;
	private String txTime;
	private Date invoiceDate;
	private BigDecimal transactionCharges;
	private String paytmRedirectUrl;
	private String paytmChecksum;
	private String paytmMid;
	private String paytmWebsite;
	private String paytmChannel;
	private String paytmUrl;
	private String shortUrl;
	private Integer noOfReminders;
	private Integer paymentAttempt;
	private String extraParams;
	private BigDecimal soldAmount;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public Integer getMonth() {
		return month;
	}
	public void setMonth(Integer month) {
		this.month = month;
	}
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	public BigDecimal getPendingBalance() {
		return pendingBalance;
	}
	public void setPendingBalance(BigDecimal pendingBalance) {
		this.pendingBalance = pendingBalance;
	}
	public BigDecimal getCreditBalance() {
		return creditBalance;
	}
	public void setCreditBalance(BigDecimal creditBalance) {
		this.creditBalance = creditBalance;
	}
	public BigDecimal getDiscount() {
		return discount;
	}
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getPaidDate() {
		return paidDate;
	}
	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	public String getPaymentType() {
		return paymentType;
	}
	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}
	public List<BillItem> getInvoiceItems() {
		return invoiceItems;
	}
	public void setInvoiceItems(List<BillItem> invoiceItems) {
		this.invoiceItems = invoiceItems;
	}
	public BigDecimal getServiceCharge() {
		return serviceCharge;
	}
	public void setServiceCharge(BigDecimal serviceCharge) {
		this.serviceCharge = serviceCharge;
	}
	public BigDecimal getPayable() {
		return payable;
	}
	public void setPayable(BigDecimal payable) {
		this.payable = payable;
	}
	public BigDecimal getInternetFees() {
		return internetFees;
	}
	public void setInternetFees(BigDecimal internetFees) {
		this.internetFees = internetFees;
	}
	public String getPaymentUrl() {
		return paymentUrl;
	}
	public void setPaymentUrl(String paymentUrl) {
		this.paymentUrl = paymentUrl;
	}
	public String getPaymentRequestId() {
		return paymentRequestId;
	}
	public void setPaymentRequestId(String paymentRequestId) {
		this.paymentRequestId = paymentRequestId;
	}
	public BigDecimal getPaidAmount() {
		return paidAmount;
	}
	public void setPaidAmount(BigDecimal paidAmount) {
		this.paidAmount = paidAmount;
	}
	public String getHdfcPaymentUrl() {
		return hdfcPaymentUrl;
	}
	public void setHdfcPaymentUrl(String hdfcPaymentUrl) {
		this.hdfcPaymentUrl = hdfcPaymentUrl;
	}
	public String getHdfcRequest() {
		return hdfcRequest;
	}
	public void setHdfcRequest(String hdfcRequest) {
		this.hdfcRequest = hdfcRequest;
	}
	public String getHdfcAccessCode() {
		return hdfcAccessCode;
	}
	public void setHdfcAccessCode(String hdfcAccessCode) {
		this.hdfcAccessCode = hdfcAccessCode;
	}
	public String getPaymentMedium() {
		return paymentMedium;
	}
	public void setPaymentMedium(String paymentMedium) {
		this.paymentMedium = paymentMedium;
	}
	public String getPaymentMode() {
		return paymentMode;
	}
	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}
	public String getPaymentMessage() {
		return paymentMessage;
	}
	public void setPaymentMessage(String paymentMessage) {
		this.paymentMessage = paymentMessage;
	}
	public String getAtomPaymentUrl() {
		return atomPaymentUrl;
	}
	public void setAtomPaymentUrl(String atomPaymentUrl) {
		this.atomPaymentUrl = atomPaymentUrl;
	}
	public String getCashfreeSignature() {
		return cashfreeSignature;
	}
	public void setCashfreeSignature(String cashfreeSignature) {
		this.cashfreeSignature = cashfreeSignature;
	}
	public String getCashFreeRedirectUrl() {
		return cashFreeRedirectUrl;
	}
	public void setCashFreeRedirectUrl(String cashFreeRedirectUrl) {
		this.cashFreeRedirectUrl = cashFreeRedirectUrl;
	}
	public String getCashFreeAppId() {
		return cashFreeAppId;
	}
	public void setCashFreeAppId(String cashFreeAppId) {
		this.cashFreeAppId = cashFreeAppId;
	}
	public String getCashFreePaymentUrl() {
		return cashFreePaymentUrl;
	}
	public void setCashFreePaymentUrl(String cashFreePaymentUrl) {
		this.cashFreePaymentUrl = cashFreePaymentUrl;
	}
	public String getCashFreeTxId() {
		return cashFreeTxId;
	}
	public void setCashFreeTxId(String cashFreeTxId) {
		this.cashFreeTxId = cashFreeTxId;
	}
	public BigDecimal getOutstandingBalance() {
		return outstandingBalance;
	}
	public void setOutstandingBalance(BigDecimal outstandingBalance) {
		this.outstandingBalance = outstandingBalance;
	}
	public String getPaymentResponse() {
		return paymentResponse;
	}
	public void setPaymentResponse(String paymentResponse) {
		this.paymentResponse = paymentResponse;
	}
	public String getTxTime() {
		return txTime;
	}
	public void setTxTime(String txTime) {
		this.txTime = txTime;
	}
	public Date getInvoiceDate() {
		return invoiceDate;
	}
	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}
	public BigDecimal getTransactionCharges() {
		return transactionCharges;
	}
	public void setTransactionCharges(BigDecimal transactionCharges) {
		this.transactionCharges = transactionCharges;
	}
	public String getPaytmRedirectUrl() {
		return paytmRedirectUrl;
	}
	public void setPaytmRedirectUrl(String paytmRedirectUrl) {
		this.paytmRedirectUrl = paytmRedirectUrl;
	}
	public String getPaytmChecksum() {
		return paytmChecksum;
	}
	public void setPaytmChecksum(String paytmChecksum) {
		this.paytmChecksum = paytmChecksum;
	}
	public String getPaytmMid() {
		return paytmMid;
	}
	public void setPaytmMid(String paytmMid) {
		this.paytmMid = paytmMid;
	}
	public String getPaytmWebsite() {
		return paytmWebsite;
	}
	public void setPaytmWebsite(String paytmWebsite) {
		this.paytmWebsite = paytmWebsite;
	}
	public String getPaytmChannel() {
		return paytmChannel;
	}
	public void setPaytmChannel(String paytmChannel) {
		this.paytmChannel = paytmChannel;
	}
	public String getPaytmUrl() {
		return paytmUrl;
	}
	public void setPaytmUrl(String paytmUrl) {
		this.paytmUrl = paytmUrl;
	}
	public String getShortUrl() {
		return shortUrl;
	}
	public void setShortUrl(String shortUrl) {
		this.shortUrl = shortUrl;
	}
	public Integer getNoOfReminders() {
		return noOfReminders;
	}
	public void setNoOfReminders(Integer noOfReminders) {
		this.noOfReminders = noOfReminders;
	}
	public Integer getPaymentAttempt() {
		return paymentAttempt;
	}
	public void setPaymentAttempt(Integer paymentAttempt) {
		this.paymentAttempt = paymentAttempt;
	}
	public String getExtraParams() {
		return extraParams;
	}
	public void setExtraParams(String extraParams) {
		this.extraParams = extraParams;
	}
	public BigDecimal getSoldAmount() {
		return soldAmount;
	}
	public void setSoldAmount(BigDecimal soldAmount) {
		this.soldAmount = soldAmount;
	}
	
}
