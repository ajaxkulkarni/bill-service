package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_business")
public class BillDBUserBusiness {
	
	private Integer id;
	private String name;
	private String description;
	private BillDBSector sector;
	private BillDBUser user;
	private Date createdDate;
	private String status;
	private String address;
	private Set<BillDBLocation> locations = new HashSet<BillDBLocation>();
	private String identificationNumber;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "sector")
	public BillDBSector getSector() {
		return sector;
	}
	public void setSector(BillDBSector sector) {
		this.sector = sector;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "user")
	public BillDBUser getUser() {
		return user;
	}
	public void setUser(BillDBUser user) {
		this.user = user;
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
	
	@Column(name = "address")
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	@ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinTable(name = "user_business_locations", joinColumns = { @JoinColumn(name = "business") }, inverseJoinColumns = { @JoinColumn(name = "area") })
	public Set<BillDBLocation> getLocations() {
		return locations;
	}
	public void setLocations(Set<BillDBLocation> locations) {
		this.locations = locations;
	}
	
	@Column(name = "identification_no")
	public String getIdentificationNumber() {
		return identificationNumber;
	}
	public void setIdentificationNumber(String identificationNumber) {
		this.identificationNumber = identificationNumber;
	}
	

}
