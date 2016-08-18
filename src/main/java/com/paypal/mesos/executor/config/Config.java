package com.paypal.mesos.executor.config;

public interface Config {

	// checks pods health every PROCESS_WATCH_INTERVAL to see if containers are running
	// kills pod if there is a non-zero exit and restart policy is violated
	public static final boolean MONITOR_POD = true;
	
	//time in milliseconds
	public static final int POD_MONITOR_INTERVAL = 2000;
	
	public static final boolean IGNORE_PULL_FAILURES = false;
}
