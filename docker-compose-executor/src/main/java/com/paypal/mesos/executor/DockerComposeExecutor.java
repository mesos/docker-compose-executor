package com.paypal.mesos.executor;

import java.io.File;
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
import com.paypal.mesos.executor.utils.ProcessUtils;

public class DockerComposeExecutor implements Executor{

	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	
	private ExecutorService executorService = null;

	private FileFetcher fileFetcher;

	private String fileName;

	private ExecutorDriver executorDriver;
	
	@Inject
	public DockerComposeExecutor(FileFetcher fileFetcher,ExecutorService executorService){
		this.fileFetcher = fileFetcher;
		this.executorService = executorService;
	}


	@Override
	public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
		TaskID taskId = taskInfo.getTaskId();
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_STARTING);
		try {
			File file = fileFetcher.getFile(taskInfo);
			this.fileName = file.getAbsolutePath();
			startWatchingDockerEvents(taskId);
			startProcess(taskId);
		}catch (Exception e) {
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
			log.warn("exception while launching process"+e.getMessage());
		} 
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_RUNNING);
	}

	//TODO instead of a seperate thread inside of observable,schedule observer on a differenct thread
	private void startWatchingDockerEvents(TaskID taskId){
		ExecutorService executorService = Executors.newCachedThreadPool();
		PublishSubject<Event> subject = PublishSubject.create();
		DockerClient dockerClient = DockerClientBuilder.getInstance("unix:///var/run/docker.sock").build();
		DockerEventObservable observable = new DockerEventObservable(executorService,dockerClient);
		DockerEventObserver observer = new DockerEventObserver(dockerClient,taskId,this.fileName,this);
		subject.subscribe(observer);
		observable.captureContainerDetails(subject);
	}
	 
	private void startProcess(TaskID taskId){
		LaunchCompose compose = new LaunchCompose(this,fileName,taskId);
		executorService.execute(compose);
	}
	
	public void suicide(TaskID taskId,boolean hasTaskFailed){
		boolean result = cleanUp();
		sendUpdateToFramework(hasTaskFailed || result, taskId, executorDriver);
		System.exit(0);
	}
	
	/**
	 * shutdown executor service
	 * stop docker-compose
	 * force remove docker images
	 * @return killing compose and removing all images are successful
	 */
	private boolean cleanUp(){
		executorService.shutdown();
		String killTask = CommandBuilder.killTask(fileName);
		int killResult = ProcessUtils.executeCommand(killTask, null);
		String removeTask = CommandBuilder.removeTask(fileName);
		int removeTaskResult = ProcessUtils.executeCommand(removeTask, null);
		return (killResult == 0 && removeTaskResult == 0);
	}
	
	private void sendUpdateToFramework(boolean hasTaskFailed,TaskID taskId,ExecutorDriver executorDriver){
		if(hasTaskFailed){
			sendTaskStatusUpdate(executorDriver,taskId, TaskState.TASK_FAILED);
		}else{
			sendTaskStatusUpdate(executorDriver,taskId, TaskState.TASK_FINISHED);
		}
	}
	
	private void sendTaskStatusUpdate(ExecutorDriver executorDriver,TaskID taskId,TaskState taskState){
		TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskId).setState(taskState).build();
		executorDriver.sendStatusUpdate(taskStatus);
	}
	
	@Override
	public void killTask(ExecutorDriver executorDriver, TaskID taskId) {
		suicide(taskId,false);
	}
	
	@Override
	public void disconnected(ExecutorDriver executorDriver) {
		log.debug("executor disconnected");
	}

	@Override
	public void error(ExecutorDriver executorDriver, String errorMessage) {
		log.error("executor received an error message:"+errorMessage);
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
		suicide(null, false);
	}

}
