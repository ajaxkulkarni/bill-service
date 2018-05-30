package com.rns.web.billapp.service.bo.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.rns.web.billapp.service.domain.BillFile;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6620229710257490921L;
	private Integer id;
	private String name;
	private String description;
	private String frequency;
	private BigDecimal price;
	private String weekDays;
	private String monthDays;
	private Date createdDate;
	private String status;
	private BillItem parentItem;
	private BigDecimal quantity;
	private BillUserLog changeLog;
	private BillSector itemSector;
	private String weeklyPricing;
	private Integer parentItemId;
	private BillFile image;
	
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
	public String getFrequency() {
		return frequency;
	}
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public String getWeekDays() {
		return weekDays;
	}
	public void setWeekDays(String weekDays) {
		this.weekDays = weekDays;
	}
	public String getMonthDays() {
		return monthDays;
	}
	public void setMonthDays(String monthDays) {
		this.monthDays = monthDays;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public BillItem getParentItem() {
		return parentItem;
	}
	public void setParentItem(BillItem parentItem) {
		this.parentItem = parentItem;
	}
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	public BillUserLog getChangeLog() {
		return changeLog;
	}
	public void setChangeLog(BillUserLog changeLog) {
		this.changeLog = changeLog;
	}
	public BillSector getItemSector() {
		return itemSector;
	}
	public void setItemSector(BillSector itemSector) {
		this.itemSector = itemSector;
	}
	public String getWeeklyPricing() {
		return weeklyPricing;
	}
	public void setWeeklyPricing(String weeklyPricing) {
		this.weeklyPricing = weeklyPricing;
	}
	public Integer getParentItemId() {
		return parentItemId;
	}
	public void setParentItemId(Integer parentItemId) {
		this.parentItemId = parentItemId;
	}
	public BillFile getImage() {
		return image;
	}
	public void setImage(BillFile image) {
		this.image = image;
	}
	
}
