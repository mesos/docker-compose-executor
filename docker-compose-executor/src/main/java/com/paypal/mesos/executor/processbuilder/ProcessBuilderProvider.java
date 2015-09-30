package com.paypal.mesos.executor.processbuilder;

public interface ProcessBuilderProvider {

	ProcessBuilder getProcessBuilder(String processName,String fileName);
	
}
