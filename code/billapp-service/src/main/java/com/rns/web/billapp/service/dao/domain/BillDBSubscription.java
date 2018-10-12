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
@Table(name = "subscriptions")
public class BillDBSubscription {
	
	private Integer id;
	private BigDecimal serviceCharge;
	private BillDBUserBusiness business;
	private Date createdDate;
	private String status;
	private String name;
	private String email;
	private String phone;
	private String address;
	private BillDBLocation location;
	private Set<BillDBItemSubscription> subscriptions = new HashSet<BillDBItemSubscription>();
	private String showBillDetails;
	
	public BillDBSubscription(Integer id) {
		setId(id);
	}
	
	public BillDBSubscription() {
		
	}
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "service_charge")
	public BigDecimal getServiceCharge() {
		return serviceCharge;
	}
	public void setServiceCharge(BigDecimal serviceCharge) {
		this.serviceCharge = serviceCharge;
	}
	
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "business")
	public BillDBUserBusiness getBusiness() {
		return business;
	}
	public void setBusiness(BillDBUserBusiness business) {
		this.business = business;
	}
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "email")
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Column(name = "phone")
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "area")
	public BillDBLocation getLocation() {
		return location;
	}
	public void setLocation(BillDBLocation location) {
		this.location = location;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	@Column(name = "address")
	public String getAddress() {
		return address;
	}

	@OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
	public Set<BillDBItemSubscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Set<BillDBItemSubscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

	@Column(name = "show_bill_details")
	public String getShowBillDetails() {
		return showBillDetails;
	}

	public void setShowBillDetails(String showBillDetails) {
		this.showBillDetails = showBillDetails;
	}
}
