package com.paypal.mesos.executor;

public class DockerEvent {

	private String containerId;
	
	private String containerName;
	
	private int pid;
	
	private int exitCode;
	
	private int failureCount;
	
	public DockerEvent(String containerId,String containerName,int pid,int exitCode){
		this.containerId = containerId;
		this.containerName = containerName;
		this.pid = pid;
		this.exitCode = exitCode;
	}
	
	
	public String getContainerId() {
		return containerId;
	}


	public String getContainerName() {
		return containerName;
	}


	public int getPid() {
		return pid;
	}


	public int getExitCode() {
		return exitCode;
	}


	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	
	
	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}
	
	public int getFailureCount() {
		return failureCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("id:").append(containerId).append(":name:").append(containerName).append(":pid:").append(pid).append(":exitCode:").append(exitCode);
		return builder.toString();
	}

}
