package com.tatesuke.ktmsaver.dto;

public class Response {

	public enum Result {
		SUCCESS, ERROR, CANCEL
	}
	
	public Result result;
	public String message;
	public String filePath;
	
}
