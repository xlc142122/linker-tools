package com.linker.tools.common;

public class ParamException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String paramName;
	private String paramValue;
	
	public ParamException(String paramName, String paramValue){
		this.paramName = paramName;
		this.paramValue = paramValue;
	}

	@Override
	public String toString() {
		return "param error： " + this.paramName + " = " + this.paramValue == null ? "null" : this.paramValue;
	}

	public String getParamName() {
		return paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamValue() {
		return paramValue;
	}

	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}
	
	

}
