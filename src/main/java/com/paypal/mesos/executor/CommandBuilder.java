package com.paypal.mesos.executor;

import java.util.List;

import com.paypal.mesos.executor.config.Config;


public class CommandBuilder {

	public static String launchTask(String fileName){
		return "docker-compose -f "+fileName+" up";
	}

	public static String pullImages(String fileName){
		String command = "docker-compose -f "+fileName+" pull";
		if(Config.IGNORE_PULL_FAILURES){
			command = command+"  --ignore-pull-failures";
		}
		return command;
	}

	public static String stopTask(String fileName){
		return "docker-compose -f "+fileName+" stop ";
	}

	public static String getContainerIds(String fileName){
		return "docker-compose -f "+fileName + " ps -q"; 
	}

	public static String linuxKill(List<Integer> pids){
		StringBuilder processIds = new StringBuilder();
		for(int pid:pids){
			processIds.append(pid).append(" ");
		}
		return "sudo kill -9 "+processIds.toString();
	}

	public static String getContainerDetails(String containerId){
		return "docker inspect --format='{{.State.Pid}},{{.State.ExitCode}},{{.State.Running}},{{.RestartCount}},{{.HostConfig.RestartPolicy.MaximumRetryCount}}' "+containerId;
	}

}
