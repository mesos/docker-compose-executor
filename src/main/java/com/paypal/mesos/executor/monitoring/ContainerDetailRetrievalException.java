package com.paypal.mesos.executor.monitoring;

public class ContainerDetailRetrievalException extends RuntimeException {

	private static final long serialVersionUID = -5387591922249919523L;
	
	private String containerId;
	
	public ContainerDetailRetrievalException(String containerId){
		super();
		this.containerId = containerId;
	}
	
	public String getContainerId() {
		return containerId;
	}
	
	@Override
	public String toString() {
		return "exception in getting details for containerId:"+containerId;
	}
}
