package com.rns.web.billapp.service.domain;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rns.web.billapp.service.bo.domain.BillAdminDashboard;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillOrder;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillUser;
import com.rns.web.billapp.service.bo.domain.BillUserLog;
import com.rns.web.billapp.service.util.BillConstants;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillServiceResponse {
	
	private Integer status;
	private String response;
	private Integer warningCode;
	private String warningText;
	private BillUser user;
	private List<BillLocation> locations;
	private List<BillItem> items;
	private List<BillUser> users;
	private List<BillInvoice> invoices;
	private BillInvoice invoice;
	private List<BillUserLog> logs;
	private List<BillOrder> orders; 
	private BillAdminDashboard dashboard;
	private List<BillBusiness> businesses;
	private BillFile file;
	private List<BillScheme> schemes;
	private BillScheme scheme;
	private List<BillSector> sectors;
	
	public BillServiceResponse() {
		setStatus(BillConstants.STATUS_OK);
		setResponse(BillConstants.RESPONSE_OK);
	}
	
	public void setResponse(Integer status, String response) {
		setStatus(status);
		setResponse(response);
	}
	
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}

	public Integer getWarningCode() {
		return warningCode;
	}

	public void setWarningCode(Integer warningCode) {
		this.warningCode = warningCode;
	}

	public String getWarningText() {
		return warningText;
	}

	public void setWarningText(String warningText) {
		this.warningText = warningText;
	}

	public BillUser getUser() {
		return user;
	}

	public void setUser(BillUser user) {
		this.user = user;
	}

	public List<BillLocation> getLocations() {
		return locations;
	}

	public void setLocations(List<BillLocation> locations) {
		this.locations = locations;
	}

	public List<BillItem> getItems() {
		return items;
	}

	public void setItems(List<BillItem> items) {
		this.items = items;
	}

	public List<BillUser> getUsers() {
		return users;
	}

	public void setUsers(List<BillUser> users) {
		this.users = users;
	}

	public List<BillInvoice> getInvoices() {
		return invoices;
	}

	public void setInvoices(List<BillInvoice> invoices) {
		this.invoices = invoices;
	}

	public BillInvoice getInvoice() {
		return invoice;
	}

	public void setInvoice(BillInvoice invoice) {
		this.invoice = invoice;
	}

	public List<BillUserLog> getLogs() {
		return logs;
	}

	public void setLogs(List<BillUserLog> logs) {
		this.logs = logs;
	}

	public List<BillOrder> getOrders() {
		return orders;
	}

	public void setOrders(List<BillOrder> orders) {
		this.orders = orders;
	}

	public BillAdminDashboard getDashboard() {
		return dashboard;
	}

	public void setDashboard(BillAdminDashboard dashboard) {
		this.dashboard = dashboard;
	}

	public List<BillBusiness> getBusinesses() {
		return businesses;
	}

	public void setBusinesses(List<BillBusiness> businesses) {
		this.businesses = businesses;
	}

	public BillFile getFile() {
		return file;
	}

	public void setFile(BillFile file) {
		this.file = file;
	}

	public List<BillScheme> getSchemes() {
		return schemes;
	}

	public void setSchemes(List<BillScheme> schemes) {
		this.schemes = schemes;
	}

	public BillScheme getScheme() {
		return scheme;
	}

	public void setScheme(BillScheme scheme) {
		this.scheme = scheme;
	}

	public List<BillSector> getSectors() {
		return sectors;
	}

	public void setSectors(List<BillSector> sectors) {
		this.sectors = sectors;
	}
	
}
