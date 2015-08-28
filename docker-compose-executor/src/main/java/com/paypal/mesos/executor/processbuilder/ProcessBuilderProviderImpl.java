package com.paypal.mesos.executor.processbuilder;

import java.util.ArrayList;
import java.util.List;

import com.paypal.mesos.executor.TaskStates;

public class ProcessBuilderProviderImpl implements ProcessBuilderProvider{

	@Override
	public ProcessBuilder getProcessBuilder(String processName, String fileName) {
		ProcessBuilder processBuilder = null;
		switch (processName) {
		case TaskStates.LAUNCH_TASK:
			processBuilder = launchTask(fileName);
			break;

		case TaskStates.KILL_TASK:
			processBuilder = killTask(fileName);
			break;

		default:
			throw new RuntimeException("Unable to build process builder for processName:"+processName);
		}
		return processBuilder;
	}


	private ProcessBuilder launchTask(String fileName){
		List<String> commandList = new ArrayList<String>();
		commandList.add("docker-compose");
		commandList.add("-f");
		commandList.add(fileName);
		commandList.add("up");
		return createProcessBuilder(commandList);
	}

	private ProcessBuilder killTask(String fileName){
		List<String> commandList = new ArrayList<String>();
		commandList.add("docker-compose");
		commandList.add("-f");
		commandList.add(fileName);
		commandList.add("stop");
		return createProcessBuilder(commandList);
	}


	private ProcessBuilder createProcessBuilder(List<String> commandList){
		ProcessBuilder processBuilder = new ProcessBuilder(commandList);
		return processBuilder.inheritIO();
	}

}
