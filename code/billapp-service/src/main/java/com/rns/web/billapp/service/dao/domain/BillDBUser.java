package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class BillDBUser {

	private Integer id;
	private String name;
	private String email;
	private String phone;
	private Date createdDate;
	private String panDetails;
	private String panFilePath;
	private String aadharNumber;
	private String aadharFilePath;
	private String status;
	private String profilePic;
	private String instaId;
	private String refreshToken;
	private String accessToken;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "full_name")
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
	
	@Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@Column(name = "pan_number")
	public String getPanDetails() {
		return panDetails;
	}
	
	public void setPanDetails(String panDetails) {
		this.panDetails = panDetails;
	}
	
	@Column(name = "pan_image_path")
	public String getPanFilePath() {
		return panFilePath;
	}
	public void setPanFilePath(String panFilePath) {
		this.panFilePath = panFilePath;
	}
	
	@Column(name = "aadhar_number")
	public String getAadharNumber() {
		return aadharNumber;
	}
	public void setAadharNumber(String aadharNumber) {
		this.aadharNumber = aadharNumber;
	}
	
	@Column(name = "aadhar_image_path")
	public String getAadharFilePath() {
		return aadharFilePath;
	}
	public void setAadharFilePath(String aadharFilePath) {
		this.aadharFilePath = aadharFilePath;
	}
	
	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Column(name = "profile_pic_path")
	public String getProfilePic() {
		return profilePic;
	}
	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}
	
	@Column(name = "insta_id")
	public String getInstaId() {
		return instaId;
	}
	public void setInstaId(String instaId) {
		this.instaId = instaId;
	}
	
	@Column(name = "refresh_token")
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
	@Column(name = "access_token")
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
}
