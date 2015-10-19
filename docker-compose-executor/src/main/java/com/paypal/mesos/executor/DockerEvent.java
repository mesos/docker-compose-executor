package com.paypal.mesos.executor;

public class DockerEvent {

	private String containerId;
	
	private String containerName;
	
	private int pid;
	
	private int exitCode;
	
	private boolean isExit;
	
	private int failureCount;
	
	public DockerEvent(Builder builder){
		this.containerId = builder.containerId;
		this.containerName = builder.containerName;
		this.pid = builder.pid;
		this.exitCode = builder.exitCode;
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

	

	public boolean isExit() {
		return isExit;
	}


	public void setExit(boolean isExit) {
		this.isExit = isExit;
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

	public static class Builder{
		
		private String containerId;
		
		private String containerName;
		
		private int pid;
		
		private int exitCode;
		
		public Builder containerId(String containerId){
			this.containerId = containerId;
			return this;
		}
		
		public Builder containerName(String containerName){
			this.containerName = containerName;
			return this;
		}
		
		public Builder pid(int pid){
			this.pid = pid;
			return this;
		}
		
		public Builder exitCode(int exitCode){
			this.exitCode = exitCode;
			return this;
		}
		
		public DockerEvent build(){
			return new DockerEvent(this);
		}
	}
}
