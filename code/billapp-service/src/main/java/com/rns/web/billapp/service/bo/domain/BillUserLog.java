package com.rns.web.billapp.service.bo.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class BillUserLog implements Comparable<BillUserLog> {
	
	private Integer id;
	private Date fromDate;
	private Date toDate;
	private String changeType;
	private BigDecimal priceChange;
	private BigDecimal quantityChange;
	private Date createdDate;
	private Integer subscriptionId;
	private Integer businessItemId;
	private Integer businessId;
	private Integer parentItemId;
	private String weeklyPricing;
	private BillItem item;
	
	public BillUserLog() {
		
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getId() {
		return id;
	}
	public Date getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public String getChangeType() {
		return changeType;
	}
	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}
	public BigDecimal getPriceChange() {
		return priceChange;
	}
	public void setPriceChange(BigDecimal priceChange) {
		this.priceChange = priceChange;
	}
	public BigDecimal getQuantityChange() {
		return quantityChange;
	}
	public void setQuantityChange(BigDecimal quantityChange) {
		this.quantityChange = quantityChange;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Integer getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(Integer subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public Integer getBusinessItemId() {
		return businessItemId;
	}

	public void setBusinessItemId(Integer businessItemId) {
		this.businessItemId = businessItemId;
	}

	public Integer getBusinessId() {
		return businessId;
	}

	public void setBusinessId(Integer businessId) {
		this.businessId = businessId;
	}

	public Integer getParentItemId() {
		return parentItemId;
	}

	public void setParentItemId(Integer parentItemId) {
		this.parentItemId = parentItemId;
	}

	public String getWeeklyPricing() {
		return weeklyPricing;
	}

	public void setWeeklyPricing(String weeklyPricing) {
		this.weeklyPricing = weeklyPricing;
	}

	
	public int compareTo(BillUserLog o) {
		if(o == null || o.getId() == null) {
			return 1;
		}
		if(this.getId() == null) {
			return -1;
		}
		if(o.getId().intValue() == this.getId().intValue()) {
			return 0;
		}
		return 1;
	}
	
	@Override
	public boolean equals(Object obj) {
		BillUserLog o = (BillUserLog) obj;
		if(o == null || o.getId() == null) {
			return false;
		}
		if(this.getId() == null) {
			return false;
		}
		if(o.getId().intValue() == this.getId().intValue()) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if(id == null) {
			return super.hashCode();
		}
		return id;
	}

	public BillItem getItem() {
		return item;
	}

	public void setItem(BillItem item) {
		this.item = item;
	}
	
}
