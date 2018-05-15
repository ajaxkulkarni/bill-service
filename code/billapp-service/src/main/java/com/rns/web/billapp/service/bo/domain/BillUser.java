package com.rns.web.billapp.service.bo.domain;

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
public class BillUser {
	
	private Integer id;
	private String name;
	private String email;
	private String phone;
	private Date createdDate;
	private String panDetails;
	private String aadharNumber;
	private String status;
	private String profilePic;
	private BillFile panFile;
	private BillFile aadharFile;
	private BillFinancialDetails financialDetails;
	private List<BillBusiness> businesses;
	private BillBusiness currentBusiness;
	private BillSubscription currentSubscription;
	private String address;
	
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
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getPanDetails() {
		return panDetails;
	}
	public void setPanDetails(String panDetails) {
		this.panDetails = panDetails;
	}
	
	public String getAadharNumber() {
		return aadharNumber;
	}
	public void setAadharNumber(String aadharNumber) {
		this.aadharNumber = aadharNumber;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getProfilePic() {
		return profilePic;
	}
	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}
	public BillFile getPanFile() {
		return panFile;
	}
	public void setPanFile(BillFile panFile) {
		this.panFile = panFile;
	}
	public BillFile getAadharFile() {
		return aadharFile;
	}
	public void setAadharFile(BillFile aadharFile) {
		this.aadharFile = aadharFile;
	}
	public BillFinancialDetails getFinancialDetails() {
		return financialDetails;
	}
	public void setFinancialDetails(BillFinancialDetails financialDetails) {
		this.financialDetails = financialDetails;
	}
	public List<BillBusiness> getBusinesses() {
		return businesses;
	}
	public void setBusinesses(List<BillBusiness> businesses) {
		this.businesses = businesses;
	}
	public BillBusiness getCurrentBusiness() {
		return currentBusiness;
	}
	public void setCurrentBusiness(BillBusiness currentBusiness) {
		this.currentBusiness = currentBusiness;
	}
	public BillSubscription getCurrentSubscription() {
		return currentSubscription;
	}
	public void setCurrentSubscription(BillSubscription currentSubscription) {
		this.currentSubscription = currentSubscription;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
}
