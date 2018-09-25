package com.rns.web.billapp.service.bo.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rns.web.billapp.service.domain.BillFile;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillBusiness implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5131782007420176838L;
	private Integer id;
	private String name;
	private String description;
	private BillSector businessSector;
	private BillUser owner;
	private Date createdDate;
	private String address;
	private List<BillLocation> businessLocations;
	private List<BillItem> items;
	private String identificationNumber;
	private BillFile logo;
	private String mapLocation;
	private String type;

	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BillSector getBusinessSector() {
		return businessSector;
	}
	public void setBusinessSector(BillSector businessSector) {
		this.businessSector = businessSector;
	}
	public BillUser getOwner() {
		return owner;
	}
	public void setOwner(BillUser owner) {
		this.owner = owner;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public List<BillLocation> getBusinessLocations() {
		return businessLocations;
	}
	public void setBusinessLocations(List<BillLocation> businessLocations) {
		this.businessLocations = businessLocations;
	}
	public List<BillItem> getItems() {
		return items;
	}
	public void setItems(List<BillItem> items) {
		this.items = items;
	}
	public String getIdentificationNumber() {
		return identificationNumber;
	}
	public void setIdentificationNumber(String identificationNumber) {
		this.identificationNumber = identificationNumber;
	}
	public BillFile getLogo() {
		return logo;
	}
	public void setLogo(BillFile logo) {
		this.logo = logo;
	}
	public String getMapLocation() {
		return mapLocation;
	}
	public void setMapLocation(String mapLocation) {
		this.mapLocation = mapLocation;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}
