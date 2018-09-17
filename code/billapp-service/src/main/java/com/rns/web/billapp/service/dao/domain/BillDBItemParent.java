package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "item_parent")
public class BillDBItemParent {
	
	private Integer id;
	private String name;
	private String description;
	private String frequency;
	private BigDecimal price;
	private String weekDays;
	private String monthDays;
	private Date createdDate;
	private String status;
	private BillDBSector sector;
	private String weeklyPricing;
	private String imagePath;
	private BigDecimal costPrice;
	private String weeklyCostPrice;
	
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
	
	@Column(name = "frequency")
	public String getFrequency() {
		return frequency;
	}
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	
	@Column(name = "price")
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@Column(name = "week_days")
	public String getWeekDays() {
		return weekDays;
	}
	public void setWeekDays(String weekDays) {
		this.weekDays = weekDays;
	}
	
	@Column(name = "month_days")
	public String getMonthDays() {
		return monthDays;
	}
	public void setMonthDays(String monthDays) {
		this.monthDays = monthDays;
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
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "sector")
	public BillDBSector getSector() {
		return sector;
	}
	public void setSector(BillDBSector sector) {
		this.sector = sector;
	}
	
	@Column(name = "weekly_price")
	public String getWeeklyPricing() {
		return weeklyPricing;
	}
	public void setWeeklyPricing(String weeklyPricing) {
		this.weeklyPricing = weeklyPricing;
	}
	
	@Column(name = "img_path")
	public String getImagePath() {
		return imagePath;
	}
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	@Column(name = "cost_price")
	public BigDecimal getCostPrice() {
		return costPrice;
	}
	public void setCostPrice(BigDecimal costPrice) {
		this.costPrice = costPrice;
	}
	
	@Column(name = "weekly_cost_price")
	public String getWeeklyCostPrice() {
		return weeklyCostPrice;
	}
	public void setWeeklyCostPrice(String weeklyCostPrice) {
		this.weeklyCostPrice = weeklyCostPrice;
	}
	
}
