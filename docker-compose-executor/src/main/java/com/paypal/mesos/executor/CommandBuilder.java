package com.paypal.mesos.executor;


public class CommandBuilder {

	public static String launchTask(String fileName){
		return "docker-compose -f "+fileName+" up";
	}
	
	public static String pullImages(String fileName){
		return "docker-compose -f "+fileName+" pull";
	}

	public static String killTask(String fileName){
		return "docker-compose -f "+fileName+" stop";
	}
	
	public static String removeTask(String fileName){
		return "docker-compose -f "+fileName+" rm --force";
	}

}
