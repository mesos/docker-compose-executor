package com.paypal.mesos.executor;

import org.apache.log4j.Logger;
import org.apache.mesos.Executor;
import org.apache.mesos.MesosExecutorDriver;

public class App {

	private static final Logger log = Logger.getLogger(App.class);
	
	public static void main(String[] args) {
		log.warn("Main method invoked");
		ExecutorComponent executorComponent = DaggerExecutorComponent.builder().build();
		Executor executor = executorComponent.getExecutor();
		log.warn("executor is null:"+(executor == null));
		new MesosExecutorDriver(executor).run();
	}
	
}
