package com.paypal.mesos.executor;

import java.util.List;


public class CommandBuilder {

	public static String launchTask(String fileName){
		return "docker-compose -f "+fileName+" up";
	}
	
	public static String pullImages(String fileName){
		return "docker-compose -f "+fileName+" pull";
	}

	public static String stopTask(String fileName){
		return "docker-compose -f "+fileName+" stop ";
	}
	

	public static String linuxKill(List<Integer> pids){
		StringBuilder processIds = new StringBuilder();
		for(int pid:pids){
			processIds.append(pid).append(" ");
		}
		return "sudo kill -9 "+processIds.toString();
	}

}
