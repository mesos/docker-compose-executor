package com.paypal.mesos.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.TaskID;

import rx.Observable;
import rx.Observer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Event;
import com.paypal.mesos.executor.config.Config;
import com.paypal.mesos.executor.utils.FileUtils;
import com.paypal.mesos.executor.utils.ProcessUtils;

/**
 * @author tgadiraju
 * Listens to all incoming docker events
 *   1. filters events to check if they belong to this task
 *   2. Inspects docker to get required details
 *   3. Writes events to a files
 *   4. Monitors pids
 *   5. Send's kill signal to executor if any non-clean exit is detected
 */
public class DockerEventObserver implements Observer<Event> {

	private Map<String,DockerEvent> map;

	private DockerClient dockerClient;

	private TaskID taskId;

	private DockerComposeExecutor executor;

	private String outputFile;

	private static final Logger log = Logger.getLogger(DockerEventObserver.class);
	
	private static final String PID = "pid";
	private static final String TASK_ID = "taskId";

	public DockerEventObserver(DockerClient dockerClient) {
		this.dockerClient = dockerClient;
		map = new ConcurrentHashMap<String,DockerEvent>();
		monitorPids();
	}
	
	public void init(TaskID taskId,String outputFileName,DockerComposeExecutor executor){
		this.taskId = taskId;
		this.executor = executor;
		this.outputFile = outputFileName;
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
	 * 	1. append to file once we have a pid
	 *  2. once we have containerId in map, check if its exit code changes
	 */
	@Override
	public void onNext(Event dockerEvent) {
		String containerId = dockerEvent.getId();
		DockerEvent eventDetails = getDockerEvent(containerId);
		if(eventDetails != null){

			if(eventDetails.getPid() != 0 && !map.containsKey(containerId)){
				 appendToFile(getCleanUpInfo(containerId, eventDetails.getPid()));
				map.put(containerId, eventDetails);
			}
			
			if(eventDetails.getExitCode() != 0 && map.containsKey(containerId)){
				DockerEvent details = map.get(containerId);
				details.setExitCode(eventDetails.getExitCode());
				map.put(containerId, details);
			}
		}
	}
    

	private Map<String,Map<String,Integer>> getCleanUpInfo(String containerId,int pid){
		 Map<String,Integer> cleanUpEntry = new HashMap<String, Integer>();
		 cleanUpEntry.put(PID, pid);
		 Map<String,Map<String,Integer>> cleanUpMap = new HashMap<String,Map<String,Integer>>();
		 cleanUpMap.put(containerId, cleanUpEntry);
		 return cleanUpMap;
	}
	
	private void appendToFile(Map<String,Map<String,Integer>> cleanupInfo){
    	FileUtils.writeToFile(outputFile, cleanupInfo);
    }
	
	private DockerEvent getDockerEvent(String containerId){
		DockerEvent result = null;
		try{
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
			ContainerConfig config = inspectResponse.getConfig();
			Map<String,String> labels = config.getLabels();
			String taskId = labels.get(TASK_ID);
			if(this.taskId.getValue().equals(taskId)){
				ContainerState state = inspectResponse.getState();
				String name = inspectResponse.getName();
				int exitCode = state.getExitCode();
				int pid = state.getPid();
				result= new DockerEvent(containerId, name, pid, exitCode);
			}
		}catch(NotFoundException exception){
			log.debug("tried to debug non-existent containerId",exception);
		}
		return result;
	}
	
	/**
	 * Monitor for pid's
	 * 1. if pid is not found
	 *       do a docker inspect and find exitCode for that pid
	 *       update failure count
	 * 2. if failure count of a pid reaches a threshold and exit is not clean send a terminate signal       
	 */
	private void monitorPids(){
		Observable.interval(Config.PROCESS_WATCH_INTERVAL,TimeUnit.MILLISECONDS).subscribe(new Observer<Long>() {
			@Override
			public void onNext(Long t) {
				for(Entry<String,DockerEvent> entry:map.entrySet()){
					DockerEvent event = entry.getValue();
					monitorPid(event);
				}
			}

			//TODO rethink if there is a better way to do this
			private void monitorPid(DockerEvent event){
				int pid = event.getPid();
				boolean isProcessRunning = ProcessUtils.isProcessRunning(pid);
				if(!isProcessRunning){
					boolean killExecutor = (event.getExitCode() != 0);
					if(killExecutor == false){
						DockerEvent result = getDockerEvent(event.getContainerId());
						boolean updatedResult = (result != null && result.getExitCode() != 0);
						killExecutor = killExecutor || updatedResult;
					}
					
					if(killExecutor){
						executor.suicide(taskId, 1);
					}
				}
			}

			@Override
			public void onError(Throwable e) {
				log.error("Error while watching for pid's",e);
			}

			@Override
			public void onCompleted() {
				log.debug("completed watching for pid's");
			}
		});
	}


}
