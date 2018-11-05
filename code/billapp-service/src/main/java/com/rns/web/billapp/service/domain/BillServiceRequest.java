package com.rns.web.billapp.service.domain;

import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rns.web.billapp.service.bo.domain.BillBusiness;
import com.rns.web.billapp.service.bo.domain.BillInvoice;
import com.rns.web.billapp.service.bo.domain.BillItem;
import com.rns.web.billapp.service.bo.domain.BillLocation;
import com.rns.web.billapp.service.bo.domain.BillNotification;
import com.rns.web.billapp.service.bo.domain.BillScheme;
import com.rns.web.billapp.service.bo.domain.BillSector;
import com.rns.web.billapp.service.bo.domain.BillUser;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillServiceRequest {
	
	private BillUser user;
	private BillBusiness business;
	private BillItem item;
	private BillInvoice invoice;
	private BillSector sector;
	private String requestType;
	private Date requestedDate;
	private List<BillItem> items;
	private BillFile file;
	private BillScheme scheme;
	private List<BillUser> users;
	private BillLocation location;
	private BillNotification notification;
	
	public BillUser getUser() {
		return user;
	}

	public void setUser(BillUser user) {
		this.user = user;
	}

	public BillBusiness getBusiness() {
		return business;
	}

	public void setBusiness(BillBusiness business) {
		this.business = business;
	}

	public BillItem getItem() {
		return item;
	}

	public void setItem(BillItem item) {
		this.item = item;
	}

	public BillInvoice getInvoice() {
		return invoice;
	}

	public void setInvoice(BillInvoice invoice) {
		this.invoice = invoice;
	}

	public BillSector getSector() {
		return sector;
	}

	public void setSector(BillSector sector) {
		this.sector = sector;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public Date getRequestedDate() {
		return requestedDate;
	}

	public void setRequestedDate(Date requestedDate) {
		this.requestedDate = requestedDate;
	}

	public List<BillItem> getItems() {
		return items;
	}

	public void setItems(List<BillItem> items) {
		this.items = items;
	}

	public BillFile getFile() {
		return file;
	}

	public void setFile(BillFile file) {
		this.file = file;
	}

	public BillScheme getScheme() {
		return scheme;
	}

	public void setScheme(BillScheme scheme) {
		this.scheme = scheme;
	}

	public List<BillUser> getUsers() {
		return users;
	}

	public void setUsers(List<BillUser> users) {
		this.users = users;
	}

	public BillLocation getLocation() {
		return location;
	}

	public void setLocation(BillLocation location) {
		this.location = location;
	}

	public BillNotification getNotification() {
		return notification;
	}

	public void setNotification(BillNotification notification) {
		this.notification = notification;
	}

}
