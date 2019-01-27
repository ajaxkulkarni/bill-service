package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillAdminDashboard {
	
	private BigDecimal totalPaid;
	private BigDecimal totalGenerated;
	private Long totalInvoices;
	private Long paidInvoices;
	private Long totalCustomers;
	private Long totalBusinesses;
	private Long pendingApprovals;
	private BigDecimal pendingSettlements;
	private BigDecimal completedSettlements;
	private Long pendingInvoices;
	private BigDecimal pendingAmount;
	private Long onlineInvoices;
	private BigDecimal onlinePaid;
	private Long offlineInvoices;
	private BigDecimal offlinePaid;
	private List<BillBusinessSummary> summary;
	
	public Long getPendingInvoices() {
		return pendingInvoices;
	}
	public void setPendingInvoices(Long pendingInvoices) {
		this.pendingInvoices = pendingInvoices;
	}
	public BigDecimal getPendingAmount() {
		return pendingAmount;
	}
	public void setPendingAmount(BigDecimal pendingAmount) {
		this.pendingAmount = pendingAmount;
	}
	public Long getOnlineInvoices() {
		return onlineInvoices;
	}
	public void setOnlineInvoices(Long onlineInvoices) {
		this.onlineInvoices = onlineInvoices;
	}
	public BigDecimal getOnlinePaid() {
		return onlinePaid;
	}
	public void setOnlinePaid(BigDecimal onlinePaid) {
		this.onlinePaid = onlinePaid;
	}
	public Long getOfflineInvoices() {
		return offlineInvoices;
	}
	public void setOfflineInvoices(Long offlineInvoices) {
		this.offlineInvoices = offlineInvoices;
	}
	public BigDecimal getOfflinePaid() {
		return offlinePaid;
	}
	public void setOfflinePaid(BigDecimal offlinePaid) {
		this.offlinePaid = offlinePaid;
	}
	public BigDecimal getTotalPaid() {
		return totalPaid;
	}
	public void setTotalPaid(BigDecimal totalPaid) {
		this.totalPaid = totalPaid;
	}
	public BigDecimal getTotalGenerated() {
		return totalGenerated;
	}
	public void setTotalGenerated(BigDecimal totalGenerated) {
		this.totalGenerated = totalGenerated;
	}
	public Long getTotalInvoices() {
		return totalInvoices;
	}
	public void setTotalInvoices(Long totalInvoices) {
		this.totalInvoices = totalInvoices;
	}
	public Long getPaidInvoices() {
		return paidInvoices;
	}
	public void setPaidInvoices(Long paidInvoices) {
		this.paidInvoices = paidInvoices;
	}
	public Long getTotalCustomers() {
		return totalCustomers;
	}
	public void setTotalCustomers(Long totalCustomers) {
		this.totalCustomers = totalCustomers;
	}
	public Long getTotalBusinesses() {
		return totalBusinesses;
	}
	public void setTotalBusinesses(Long totalBusinesses) {
		this.totalBusinesses = totalBusinesses;
	}
	public Long getPendingApprovals() {
		return pendingApprovals;
	}
	public void setPendingApprovals(Long pendingApprovals) {
		this.pendingApprovals = pendingApprovals;
	}
	public BigDecimal getPendingSettlements() {
		return pendingSettlements;
	}
	public void setPendingSettlements(BigDecimal pendingSettlements) {
		this.pendingSettlements = pendingSettlements;
	}
	public BigDecimal getCompletedSettlements() {
		return completedSettlements;
	}
	public void setCompletedSettlements(BigDecimal completedSettlements) {
		this.completedSettlements = completedSettlements;
	}
	public List<BillBusinessSummary> getSummary() {
		return summary;
	}
	public void setSummary(List<BillBusinessSummary> summary) {
		this.summary = summary;
	}
	

}
