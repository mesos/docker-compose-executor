package com.paypal.mesos.executor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.log4j.Logger;
import org.apache.mesos.Protos.TaskID;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import rx.Observable;
import rx.Observer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Event;
import com.paypal.mesos.executor.utils.ProcessUtils;

public class DockerEventObserver implements Observer<Event> {

	private Map<String,DockerEvent> map;

	private Set<String> containerNames;

	private DockerClient dockerClient;

	private TaskID taskId;

	private boolean isFileWritten;

	private DockerComposeExecutor executor;

	private static final Logger log = Logger.getLogger(DockerEventObserver.class);

	public DockerEventObserver(DockerClient dockerClient,TaskID taskId,String fileName,DockerComposeExecutor executor) {
		this.dockerClient = dockerClient;
		this.taskId = taskId;
		map = new ConcurrentHashMap<String,DockerEvent>();
		this.executor = executor;
		this.containerNames = getContainerNames(fileName);
	}


	@Override
	public void onCompleted() {
		log.debug("onComplete called");
	}

	@Override
	public void onError(Throwable throwable) {
		log.error("error in processing docker event:"+throwable.getMessage());
	}

	/**
	 * make sure events are filtered
	 * 	1. if event is start
	 * 		add to the map and delete from set
	 *      if set is empty write all the details to a file
	 *  2. if event is kill or die or stop
	 *  	capture exitCode
	 */
	@Override
	public void onNext(Event dockerEvent) {
		String containerId = dockerEvent.getId();
		try{
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
			ContainerConfig config = inspectResponse.getConfig();
			Map<String,String> labels = config.getLabels();
			String taskId = labels.get("taskId");
			if(this.taskId.getValue().equals(taskId)){
				ContainerState state = inspectResponse.getState();
				String name = inspectResponse.getName();
				int exitCode = state.getExitCode();
				String event = dockerEvent.getStatus();
				boolean isValidContainerName = containerNames.contains(name);
				log.info("received event:"+event+"for containerId:"+name+":"+isValidContainerName);
				switch(event){
				case "create":
				case "start":
				case "attach":
					int pid = state.getPid();
					DockerEvent eventDetails = new DockerEvent.Builder().containerId(containerId).containerName(name).exitCode(exitCode).pid(pid).build();
					log.info("added "+name+"to map");
					map.put(containerId, eventDetails);
					if(!this.isFileWritten && map.size() == containerNames.size()){
						log.info("all containers started for task:"+taskId);
						//write to a file
						this.isFileWritten = true;
						writeToFile("/tmp/details_"+taskId, map);
						//start watching for all the pids
						watchPids();
					}
					break;
				case "kill":
				case "die":
				case "stop":
					setExitStatus(containerId, exitCode);
					break;
				default: 
					log.info("received event:"+event+"for containerId:"+containerId);
				};
			}
		}catch(NotFoundException e){
			log.info("tried to inspect invalid docker container id:"+containerId);
		}

	}

	private void setExitStatus(String containerId,int exitCode){
		if(map.containsKey(containerId)){
			DockerEvent existingEvent = map.get(containerId);
			existingEvent.setExitCode(exitCode);
			existingEvent.setExit(true);
		}
	}


	private void sendKillSignal(){
		log.info("sending kill signal......");
		executor.suicide(taskId,true);

	}

	private void watchPids(){
		Observable.interval(10, TimeUnit.SECONDS).subscribe(new Observer<Long>() {
			@Override
			public void onNext(Long t) {
				log.info("watching for pid's");
				for(Entry<String,DockerEvent> entry:map.entrySet()){
					DockerEvent event = entry.getValue();
					if(event.isExit()){
						//unsubscribe to subscription
						log.info("from isExit");
						sendKillSignal();
					}else{
						int pid = event.getPid();
						try {
							boolean isProcessRunning = isProcessRunning(pid, 200, TimeUnit.MILLISECONDS);
							if(!isProcessRunning){
								int failureCount = event.getFailureCount();
								if(failureCount > 0){
									//unsubscribe to subscription
									log.info("process didn't exist"+pid);
									sendKillSignal();
								}else{
									event.setFailureCount(failureCount+1);
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			@Override
			public void onError(Throwable e) {
				log.info("Error while watching for pid's");
			}

			@Override
			public void onCompleted() {
				log.info("completed watching for pid's");
			}
		});
	}

	private  boolean isProcessRunning(int pid, int timeout, TimeUnit timeunit) throws java.io.IOException {
		String line;
		if (OS.isFamilyWindows()) {
			line = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
		}
		else {
			line = "ps -p " + pid;
		}
		ExecuteWatchdog watchdog = ProcessUtils.createTimeoutWatchdog(timeunit, timeout);
		int exitValue = ProcessUtils.executeCommand(line, watchdog);
		return exitValue == 0;
	}

	
	//TODO writing to file shouldn't be here move this to file writer
	private File writeToFile(String fileName,Map<String,DockerEvent> updatedRootYaml) {
		try{
			File file = new File(fileName);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file);
			Yaml yaml = provideYaml();
			yaml.dump(updatedRootYaml,fileWriter);
			fileWriter.flush();
			fileWriter.close();
			return file;
		}catch(FileNotFoundException e){
			log.warn("not able to write to file:"+e);
		}catch(IOException e){
			log.warn("IO Exception:"+e);
		}
		return null;
	}

	//TODO this shouldn't be here
	private Yaml provideYaml(){
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

	private Set<String> getContainerNames(String fileName){
		Map<String,Map<String,Object>> yaml = readFromFile(fileName);
		Set<String> set = new HashSet<String>();
		for(Entry<String, Map<String,Object>> yamlEntry:yaml.entrySet()){
			Object name = yamlEntry.getValue().get("container_name");
			if(name != null){
				log.info("set data:"+name);
				set.add(String.valueOf(name));
			}
		}
		return set;
	}

	private Map<String,Map<String,Object>> readFromFile(String path) {
		try{
			FileReader fileReader = new FileReader(new File(path));
			@SuppressWarnings("unchecked")
			Map<String,Map<String,Object>>yamlMap = (Map<String,Map<String,Object>>)provideYaml().load(fileReader);
			fileReader.close();
			return yamlMap;
		}catch(FileNotFoundException e){
			log.warn("not able to write to file:"+e);
		}catch(IOException e){
			log.warn("IO Exception:"+e);
		}
		return null;
	}



}
