package com.paypal.mesos.executor.processbuilder;

public interface ProcessBuilderProvider {

	public ProcessBuilder getProcessBuilder(String processName,String fileName);
	
}
