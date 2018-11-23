package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;

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
	

}
