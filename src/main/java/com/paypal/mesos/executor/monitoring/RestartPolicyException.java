package com.paypal.mesos.executor.monitoring;

public class RestartPolicyException extends RuntimeException{

	private static final long serialVersionUID = 6529482430311761551L;
	
	private String containerId;
	private int exitCode;
	private int restartCount;

	public RestartPolicyException(String containerId,int exitCode,int restartCount){
		super();
		this.containerId = containerId;
		this.exitCode = exitCode;
		this.restartCount = restartCount;
	}

	public String getContainerId() {
		return containerId;
	}


	public int getExitCode() {
		return exitCode;
	}


	public int getRestartCount() {
		return restartCount;
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("containerId:").append(containerId)
		.append(":exitCode:").append(exitCode)
		.append(":restartCount:").append(restartCount);
		return builder.toString();
	}

}
