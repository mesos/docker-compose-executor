package com.paypal.mesos.executor;

import org.apache.mesos.Protos.TaskID;

import com.paypal.mesos.executor.utils.ProcessUtils;

public class LaunchCompose implements Runnable{

	private TaskID taskId;
	private String fileName;
	private DockerComposeExecutor executor;

	public LaunchCompose(DockerComposeExecutor executor,String fileName,TaskID taskId) {
		this.executor = executor;
		this.fileName = fileName;
		this.taskId = taskId;
	}

	@Override
	public void run() {
		int exitCode = 0;
		String pullCommand = CommandBuilder.pullImages(fileName);
		ProcessUtils.executeCommand(pullCommand, null);
		String launchCommand = CommandBuilder.launchTask(fileName);
		exitCode = ProcessUtils.executeCommand(launchCommand, null);
		executor.suicide(taskId,exitCode == 0);
	}


}