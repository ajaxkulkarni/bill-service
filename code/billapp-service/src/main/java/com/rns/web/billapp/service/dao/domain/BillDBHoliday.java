package com.rns.web.billapp.service.dao.domain;

import static javax.persistence.GenerationType.IDENTITY;

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
@Table(name = "holidays")
public class BillDBHoliday {

	private Integer id;
	private Integer month;
	private Integer day;
	private Integer date;
	private Integer createdDate;
	private String holidayName;
	private BillDBSector sector;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "month")
	public Integer getMonth() {
		return month;
	}
	public void setMonth(Integer month) {
		this.month = month;
	}
	
	@Column(name = "day")
	public Integer getDay() {
		return day;
	}
	public void setDay(Integer day) {
		this.day = day;
	}
	
	@Column(name = "date")
	public Integer getDate() {
		return date;
	}
	public void setDate(Integer date) {
		this.date = date;
	}
	
	@Column(name = "created_date")
	public Integer getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Integer createdDate) {
		this.createdDate = createdDate;
	}
	
	@Column(name = "holiday_name")
	public String getHolidayName() {
		return holidayName;
	}
	public void setHolidayName(String holidayName) {
		this.holidayName = holidayName;
	}
	
	@ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumn(name = "sector")
	public BillDBSector getSector() {
		return sector;
	}
	public void setSector(BillDBSector sector) {
		this.sector = sector;
	}
	
}
