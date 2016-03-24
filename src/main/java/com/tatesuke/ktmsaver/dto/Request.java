package com.tatesuke.ktmsaver.dto;

public class Request {

	public String action;
	public String fileDir;
	public String fileName;
	public String content;
	public boolean backupEnabled;
	public String backupDir;
	public int backupGeneration;

	@Override
	public String toString() {
		return "{\"action\":\"" + action + "\",\"fileDir\":\"" + fileDir
				+ "\",\"fileName\":\"" + fileName + "\",\"content\":" + "***"
				+ ",\"backupEnabled\":" + backupEnabled + ",\"backupDir\":\""
				+ backupDir + "\",\"backupGeneration\":" + backupGeneration + "}";
	}
}
