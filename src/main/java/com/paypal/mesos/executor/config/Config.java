package com.paypal.mesos.executor.config;

public interface Config {

	public static final String DOCKER_SERVER_URL = "unix:///var/run/docker.sock";
	
	//time in milliseconds
	public static final int PROCESS_WATCH_INTERVAL = 5000;
	
	//this will be in sandbox directory and executorId will be added to this prefix
	public static final String DOCKER_DETAILS_FILE_PREFIX = "details_";
	
	//executor id will be appended to this. Mesos hook will use this file to perform cleanup
	public static final String DOCKER_FILE_PREFIX = "/tmp/file_";
}
