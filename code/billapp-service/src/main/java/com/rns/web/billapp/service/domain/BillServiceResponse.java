package com.rns.web.billapp.service.domain;

import com.rns.web.billapp.service.util.BillConstants;

public class BillServiceResponse {
	
	private Integer status;
	private String response;
	
	public BillServiceResponse() {
		setStatus(BillConstants.STATUS_OK);
		setResponse(BillConstants.RESPONSE_OK);
	}
	
	public void setResponse(Integer status, String response) {
		setStatus(status);
		setResponse(response);
	}
	
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}
	

}
