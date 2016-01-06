package com.paypal.mesos.executor.monitoring;

public class ContainerDetails {

	private String containerId;
	
	private boolean isRunning;
	
	private int restartCount;
	
	private int maxAllowedRestartCount;

	private int exitCode;
	
	private int pid;
	
	public ContainerDetails(String containerId,boolean isRunning,int restartCount,int maxAllowedRestartCount,int exitCode,int pid){
		this.containerId = containerId;
		this.isRunning = isRunning;
		this.restartCount = restartCount;
		this.maxAllowedRestartCount = maxAllowedRestartCount;
		this.exitCode = exitCode;
		this.pid = pid;
	}

	public String getContainerId() {
		return containerId;
	}


	public boolean isRunning() {
		return isRunning;
	}

	public int getRestartCount() {
		return restartCount;
	}

	public int getMaxAllowedRestartCount() {
		return maxAllowedRestartCount;
	}
	
	public int getExitCode() {
		return exitCode;
	}

	public int getPid() {
		return pid;
	}
	
	@Override
	public String toString() {
		return "ContainerDetails [containerId=" + containerId + ", isRunning=" + isRunning + ", restartCount="
				+ restartCount + ", maxAllowedRestartCount="
				+ maxAllowedRestartCount + ", exitCode=" + exitCode + "]";
	}
	
}
