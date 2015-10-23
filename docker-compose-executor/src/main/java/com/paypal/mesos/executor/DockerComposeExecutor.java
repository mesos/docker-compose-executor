package com.paypal.mesos.executor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import rx.subjects.PublishSubject;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.DockerClientBuilder;
import com.paypal.mesos.executor.fetcher.FileFetcher;
import com.paypal.mesos.executor.processbuilder.ProcessBuilderProvider;
import com.paypal.mesos.executor.utils.ProcessUtils;

public class DockerComposeExecutor implements Executor{

	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	
	private ExecutorService executorService = null;

	private FileFetcher fileFetcher;

	private ProcessBuilderProvider processBuilderProvider;

	private String fileName;
	
	private Process process;

	private ExecutorDriver executorDriver;
	
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
			startWatchingDockerEvents(taskId);
			watchProcess(process,taskInfo,executorDriver);
		}catch (Exception e) {
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
			log.warn("exception while launching process"+e.getMessage());
		} 
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_RUNNING);
	}

	private void startWatchingDockerEvents(TaskID taskId){
		System.out.println("start watching for docker events");
		ExecutorService executorService = Executors.newCachedThreadPool();
		PublishSubject<Event> subject = PublishSubject.create();
		DockerClient dockerClient = DockerClientBuilder.getInstance("unix:///var/run/docker.sock").build();
		DockerEventObservable observable = new DockerEventObservable(executorService,dockerClient);
		DockerEventObserver observer = new DockerEventObserver(dockerClient,taskId,this.fileName,this);
		subject.subscribe(observer);
		observable.captureContainerDetails(subject);
		System.out.println("reached end of docker events");
	}
	 
	private void sendTaskStatusUpdate(ExecutorDriver executorDriver,TaskID taskId,TaskState taskState){
		TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskId).setState(taskState).build();
		executorDriver.sendStatusUpdate(taskStatus);
	}

   
	private void watchProcess(final Process process,final TaskInfo taskInfo,final ExecutorDriver executorDriver){
		LaunchCompose compose = new LaunchCompose(executorDriver, taskInfo, process);
		executorService.execute(compose);
	}
	
	class LaunchCompose implements Runnable{

		ExecutorDriver executorDriver;
		TaskInfo taskInfo;
		Process process;
		
	  public LaunchCompose(ExecutorDriver executorDriver,TaskInfo taskInfo,Process process) {
			this.executorDriver = executorDriver;
			this.taskInfo = taskInfo;
			this.process = process;
		}
		
		@Override
		public void run() {
			boolean isInterupted = false;
			int exitCode = 0;
			try {
				//docker-compose pull to get latest images
				ProcessUtils.executeCommand(getImagePullCommand(), null);
				ProcessBuilder processBuilder = processBuilderProvider.getProcessBuilder(TaskStates.LAUNCH_TASK, fileName);
				this.process = processBuilder.start();
				exitCode = process.waitFor();
			} catch (InterruptedException e) {
				isInterupted = true;
				log.warn("interupted while waiting for process to be completed:"+taskInfo.getTaskId().getValue());
			}catch(IOException e){ 
				isInterupted = true;
				log.warn("IO exception while waiting:"+taskInfo.getTaskId().getValue());
			}finally{
				killTask(executorDriver,taskInfo.getTaskId());
				sendUpdateToFramework((exitCode != 0 || isInterupted), taskInfo.getTaskId(), executorDriver);
			}
		}
		
		private String getImagePullCommand(){
			return "docker-compose -f "+fileName+" pull";
		}
	}
	
	
	public void suicide(TaskID taskId){
		killTask(this.executorDriver,taskId);
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
			System.exit(0);
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
		this.executorDriver = executorDriver;
	}

	@Override
	public void reregistered(ExecutorDriver executorDriver, SlaveInfo slaveInfo) {
		log.debug("executor reregistered on slave:"+slaveInfo.getHostname());
		this.executorDriver = executorDriver;
	}

	@Override
	public void shutdown(ExecutorDriver executorDriver) {
		log.debug("shutting down executor");
		if(process != null){
			process.destroy();
		}
		System.exit(0);
	}

}
