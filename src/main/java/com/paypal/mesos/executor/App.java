package com.paypal.mesos.executor;

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;

public class App {
    
	public static void main(String[] args) {
		ExecutorComponent executorComponent = DaggerExecutorComponent.builder().build();
		Executor executor = executorComponent.getExecutor();
		ExecutorDriver driver= new MesosExecutorDriver(executor);
		driver.run();
	}
	
}
