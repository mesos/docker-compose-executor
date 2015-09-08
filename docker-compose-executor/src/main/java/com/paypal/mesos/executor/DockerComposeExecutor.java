package com.paypal.mesos.executor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.SlaveInfo;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;

import com.paypal.mesos.executor.fetcher.FileFetcher;
import com.paypal.mesos.executor.processbuilder.ProcessBuilderProvider;

public class DockerComposeExecutor implements Executor{

	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	
	private ExecutorService executorService = null;

	private FileFetcher fileFetcher;

	private ProcessBuilderProvider processBuilderProvider;

	private String fileName;
	
	private Process process;

	@Inject
	public DockerComposeExecutor(FileFetcher fileFetcher,ProcessBuilderProvider processBuilder,ExecutorService executorService){
		this.fileFetcher = fileFetcher;
		this.processBuilderProvider = processBuilder;
		this.executorService = executorService;
	}


	@Override
	public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
		System.out.println("launch task called:"+taskInfo);
		TaskID taskId = taskInfo.getTaskId();
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_STARTING);
		try {
			File file = fileFetcher.getFile(taskInfo);
			this.fileName = file.getAbsolutePath();
			ProcessBuilder processBuilder = processBuilderProvider.getProcessBuilder(TaskStates.LAUNCH_TASK, fileName);
			this.process = processBuilder.start();
			watchProcess(process,taskInfo,executorDriver);
		}catch (Exception e) {
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
			log.warn("exception while launching process"+e.getMessage());
		} 
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_RUNNING);
	}

	 
	private void sendTaskStatusUpdate(ExecutorDriver executorDriver,TaskID taskId,TaskState taskState){
		TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskId).setState(taskState).build();
		executorDriver.sendStatusUpdate(taskStatus);
	}


	private void watchProcess(final Process process,final TaskInfo taskInfo,final ExecutorDriver executorDriver){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				boolean isInterupted = false;
				int exitCode = 0;
				try {
					exitCode = process.waitFor();
				} catch (InterruptedException e) {
					isInterupted = true;
					log.warn("interupted while waiting for process to be completed:"+taskInfo.getTaskId().getValue());
				} finally{
					killTask(executorDriver,taskInfo.getTaskId());
					sendUpdateToFramework((exitCode != 0 || isInterupted), taskInfo.getTaskId(), executorDriver);
				}
			}
		});
	}
	
	@Override
	public void killTask(ExecutorDriver executorDriver, TaskID taskId) {
		System.out.println("kill task called");
		Process killProcess = null,removeProcess = null;
		int exitStatus = 0,removeExitStatus = 0;
		try {
			executorService.shutdown();
			killProcess = processBuilderProvider.getProcessBuilder(TaskStates.KILL_TASK, fileName).start();
		    exitStatus = killProcess.waitFor();
		    removeProcess = processBuilderProvider.getProcessBuilder(TaskStates.REMOVE_TASK, fileName).start();
		    removeExitStatus = removeProcess.waitFor();
		    log.info("killing Task is completed and exit code is:"+exitStatus);
		} catch (InterruptedException e) {
			log.info("interrupted while waiting to kill process");
		} catch (IOException e) {
			log.info("IOException while trying to kill process");
		}finally{
			if(killProcess != null){
				killProcess.destroy();
			}
			if(removeProcess != null){
				removeProcess.destroy();
			}
			if(process != null){
				process.destroy();
			}
			sendUpdateToFramework(((exitStatus != 0 || exitStatus != 137) && (removeExitStatus !=0)), taskId, executorDriver);
		}
	}
	
	private void sendUpdateToFramework(boolean hasTaskFailed,TaskID taskId,ExecutorDriver executorDriver){
		if(hasTaskFailed){
			sendTaskStatusUpdate(executorDriver,taskId, TaskState.TASK_FAILED);
		}else{
			sendTaskStatusUpdate(executorDriver,taskId, TaskState.TASK_FINISHED);
		}
	}
	
	@Override
	public void disconnected(ExecutorDriver executorDriver) {
		log.debug("executor disconnected");
	}

	@Override
	public void error(ExecutorDriver executorDriver, String errorMessage) {
		log.warn("executor received an error message:"+errorMessage);
	}

	@Override
	public void frameworkMessage(ExecutorDriver arg0, byte[] arg1) {
		log.debug("received framework message");
	}
	
	@Override
	public void registered(ExecutorDriver executorDriver, ExecutorInfo executorInfo,
			FrameworkInfo frameworkInfo, SlaveInfo slaveInfo) {
		log.debug("executor registered with framework:"+frameworkInfo.getName()+":on slave:"+slaveInfo.getHostname());
	}

	@Override
	public void reregistered(ExecutorDriver executorDriver, SlaveInfo slaveInfo) {
		log.debug("executor reregistered on slave:"+slaveInfo.getHostname());
	}

	@Override
	public void shutdown(ExecutorDriver executorDriver) {
		log.debug("shutting down executor");
		if(process != null){
			process.destroy();
		}
	}

}
