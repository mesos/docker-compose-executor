package com.paypal.mesos.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import com.paypal.mesos.executor.fetcher.FileFetcher;
import com.paypal.mesos.executor.monitoring.ComposeMonitor;
import com.paypal.mesos.executor.monitoring.ContainerDetails;
import com.paypal.mesos.executor.utils.ProcessUtils;

public class DockerComposeExecutor implements Executor{

	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	

	private FileFetcher fileFetcher;

	private String fileName;

	private ExecutorDriver executorDriver;
	
	private DockerComposeProcessObserver processObserver;
	
	
	private ExecutorInfo executorInfo;
	
	private ComposeMonitor podMonitor;
	
	@Inject
	public DockerComposeExecutor(FileFetcher fileFetcher,DockerComposeProcessObserver processObserver,ComposeMonitor podMonitor){
		this.fileFetcher = fileFetcher;
		this.processObserver = processObserver;
		this.podMonitor = podMonitor;
	}

	@Override
	public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
		TaskID taskId = taskInfo.getTaskId();
		processObserver.init(this, taskId);
		sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_STARTING);
		try {
			File file = fileFetcher.getFile(executorInfo,taskInfo);
			this.fileName = file.getAbsolutePath();
			podMonitor.subscribeToChanges(new Action1<Integer>() {
				@Override
				public void call(Integer exitCode) {
					suicide(taskId, exitCode);
				}
			});
			podMonitor.startMonitoring(fileName);
			updateImagesAndStartCompose(taskId);
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_RUNNING);
		}catch (Exception e) {
			log.error("exception while launching process",e);
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
		} 
	}

	private void updateImagesAndStartCompose(TaskID taskId){
		Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				String pullCommand = CommandBuilder.pullImages(fileName);
				int imageUpdateExitCode = ProcessUtils.executeCommand(pullCommand, null);
				if(imageUpdateExitCode != 0){
					log.error("unable to pull updated images trying to bring the pod up with existing images");
				}
				String launchCommand = CommandBuilder.launchTask(fileName);
				int exitCode = ProcessUtils.executeCommand(launchCommand, null);
				subscriber.onNext(exitCode);
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(processObserver);
	}
	
	public void suicide(TaskID taskId,int exitCode){
		int stopContainers = cleanUp();
		if(exitCode == 0 && stopContainers == 0){
			sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FINISHED);
			System.exit(0);
		}else{
			sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FAILED);
			System.exit(1);
		}
	}
	
	
	/**
	 * stop docker-compose
	 * force remove docker images
	 * @return killing compose and removing all images are successful
	 */
	private int cleanUp(){
		String killTask = CommandBuilder.stopTask(fileName);
		int exitCode = ProcessUtils.executeCommand(killTask,null);
		if(exitCode != 0 ){
			exitCode = linuxKill(fileName);
		}
		return exitCode;
	}
	
	private int linuxKill(String fileName){
		List<String> containerIds = podMonitor.getContainerIds(fileName);
		List<Integer> pids = new ArrayList<Integer>();
		for(String containerId:containerIds){
			ContainerDetails details = podMonitor.getContainerDetails(containerId);
			int pid = details.getPid();
			if(pid != 0){
				pids.add(pid);
			}
		}
		int exitCode = 1;
		if(pids.size() > 0){
			String command = CommandBuilder.linuxKill(pids);
		    exitCode = ProcessUtils.executeCommand(command, null);
		}
		return exitCode;
	}
	
	private void sendTaskStatusUpdate(ExecutorDriver executorDriver,TaskID taskId,TaskState taskState){
		TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskId).setState(taskState).build();
		executorDriver.sendStatusUpdate(taskStatus);
	}
	
	@Override
	public void killTask(ExecutorDriver executorDriver, TaskID taskId) {
		log.info("kill task called for taskId:"+taskId.getValue());
		suicide(taskId,0);
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
		this.executorInfo = executorInfo;
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
		suicide(null, 0);
	}

}
