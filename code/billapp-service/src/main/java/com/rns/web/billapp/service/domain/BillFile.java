package com.rns.web.billapp.service.domain;

import java.io.InputStream;
import java.math.BigDecimal;

public class BillFile {
	
	private String fileName;
	private String filePath;
	private InputStream fileData;
	private String fileType;
	private BigDecimal fileSize;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public InputStream getFileData() {
		return fileData;
	}
	public void setFileData(InputStream fileData) {
		this.fileData = fileData;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public BigDecimal getFileSize() {
		return fileSize;
	}
	public void setFileSize(BigDecimal fileSize) {
		this.fileSize = fileSize;
	}
	
	

}
