package com.paypal.mesos.executor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.SlaveInfo;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.Paths;

public class DockerComposeExecutor implements Executor{

	private ExecutorService executorService = null;
	
	private List<String> containerNames = null;
	
	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	
	public void disconnected(ExecutorDriver arg0) {
		// TODO Auto-generated method stub
		
	}

	public void error(ExecutorDriver arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void frameworkMessage(ExecutorDriver arg0, byte[] arg1) {
		// TODO Auto-generated method stub
		
	}

	public void killTask(ExecutorDriver executorDriver, TaskID taskId) {
		if(containerNames == null || containerNames.isEmpty()){
			return;
		}

		List<String> dockerKillParams = new ArrayList<String>();
		dockerKillParams.add("docker");
		dockerKillParams.add("kill");
		dockerKillParams.addAll(containerNames);

		ProcessBuilder processBuilder = new ProcessBuilder(dockerKillParams);
		try {
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if(exitCode == 0){
				log.info("Process cleanly exited");
			}else{
				log.info("Process didn't exit cleanly");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(InterruptedException interruptedException){

		}
	}

	public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
		log.info("launching task with taskId:"+taskInfo.getTaskId().getValue());
	    sendTaskStatusUpdate(taskInfo, executorDriver, TaskState.TASK_STARTING);
		launchTask(taskInfo,executorDriver);
		sendTaskStatusUpdate(taskInfo, executorDriver, TaskState.TASK_RUNNING);
	}

	private void sendTaskStatusUpdate(TaskInfo taskInfo,ExecutorDriver executorDriver,TaskState taskState){
		TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskInfo.getTaskId()).setState(taskState).build();
		executorDriver.sendStatusUpdate(taskStatus);
	}
	
	/**
	 * @param taskInfo
	 */
	private void launchTask(TaskInfo taskInfo,ExecutorDriver executorDriver){
		//get file name for URI
		
		//change yaml file names
		String composeFileName = "docker-compose-example/docker-compose.yml";
		String updatedComposeName = "docker-compose-example/docker-compose_new.yml";
		this.containerNames = editYaml(composeFileName,updatedComposeName, taskInfo.getTaskId().getValue());
		
		//launch docker compose
		launchDockerCompose(taskInfo,executorDriver,updatedComposeName);
		
		//push logs to corresponding files
	}
	
	
	private void launchDockerCompose(final TaskInfo taskInfo,final ExecutorDriver executorDriver,final String fileName){
	  executorService.submit(new Callable<Integer>() {
			public Integer call() {
				int exitCode = 0;
				ProcessBuilder processBuilder = createDockerComposeProcessBuilder(fileName);
				Process process;
				try {
					process = processBuilder.start();
				    exitCode = process.waitFor();
				} catch (IOException ioException) {
					log.warn("encountered IOException while running task:"+taskInfo.getTaskId().getValue());
					exitCode = -1; 
				} catch(InterruptedException interruptedException){
					log.warn("encountered Interrupted while running task:"+taskInfo.getTaskId().getValue());
					exitCode = -1;
				}finally{
					TaskState taskState = TaskState.TASK_FINISHED;
					if(exitCode != 0){
						taskState = TaskState.TASK_FAILED;
					}
					sendTaskStatusUpdate(taskInfo, executorDriver, taskState);
				}
				return exitCode;
			}
		});
	}
	
	
	private ProcessBuilder createDockerComposeProcessBuilder(
			String fileName) {
		ProcessBuilder processBuilder = new ProcessBuilder("docker-compose","-f",fileName,"up");
		processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return processBuilder;
	}
	
	public List<String> editYaml(String fileName,String newFileName,String taskId){

		Map<String,Map<String,Object>> yamlMap=readDockerCompose(fileName);
        List<String> containerNames = new ArrayList<String>();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		
		String containerId = getContainerId();
                String parentCgroup = "/sys/fs/cgroup/cpu/mesos/" + containerId;
                makeWritable(parentCgroup);

		for(Map.Entry<String, Map<String,Object>> mapEntry:yamlMap.entrySet()){
			String key = mapEntry.getKey();
			Map<String,Object> yamlValue = mapEntry.getValue();
			yamlValue.put("cgroup_parent", parentCgroup);
			containerNames = replaceContainerNames(taskId, key, yamlValue);
			resultMap.put(key,yamlValue);
		}
		
		writeFile(newFileName, resultMap);
        return containerNames;
	}

	private void writeFile(String fileName,Map<String,Object> ymlFile){
		Yaml writerYaml = createYaml();
		try{
			FileWriter writer = new FileWriter(fileName);
			writerYaml.dump(ymlFile,writer);
			writer.flush();
			writer.close();
		}catch(FileNotFoundException e){

		}catch(IOException io){

		}
	}

	private Yaml createYaml(){
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}


	@SuppressWarnings("unchecked")
	private Map<String,Map<String,Object>> readDockerCompose(String fileName){
		FileReader fileReader;
		Map<String,Map<String,Object>> yamlMap = null;
		try {
			fileReader = new FileReader(new File(fileName));
			Yaml yaml = new Yaml();
			yamlMap = (Map<String,Map<String,Object>>)yaml.load(fileReader);
			fileReader.close();
		} catch (FileNotFoundException e) {

		} catch(IOException io){

		}
		return yamlMap;
	}

	private List<String> replaceContainerNames(String taskId,String key,Map<String,Object> yamlValue){
		String CONTAINER_NAME = "container_name";
		String NETWORK = "net";

		List<String> containerNames = new ArrayList<String>();
		
		Object containerValue = taskId+"_"+key;
		if(yamlValue.containsKey(CONTAINER_NAME)){
			containerValue = taskId+"_"+String.valueOf(yamlValue.get(CONTAINER_NAME));
			containerNames.add((String)containerValue);
		}
		yamlValue.put(CONTAINER_NAME, containerValue);

		Object networkValue = yamlValue.get(NETWORK);
		if(networkValue !=null && (String.valueOf(networkValue).contains("container"))){
			String networkValueString = String.valueOf(yamlValue.get(NETWORK));
			String [] split = networkValueString.split(":");
			String containerName = split[split.length-1];
			yamlValue.put(NETWORK, "container:"+taskId+"_"+containerName);	
		}
		
		return containerNames;
	}



	public void registered(ExecutorDriver arg0, ExecutorInfo arg1,
			FrameworkInfo arg2, SlaveInfo arg3) {
		// TODO Auto-generated method stub
		
	}

	public void reregistered(ExecutorDriver arg0, SlaveInfo arg1) {
		// TODO Auto-generated method stub
		
	}

	public void shutdown(ExecutorDriver arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public String getContainerId() {
	        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        	String[] split = cwd.split("/");
        	return split[split.length - 1];
    	}

	private void makeWritable(String path) {
    	    File file = new File(path);
        	if (!file.setWritable(true, false)) {
            		System.out.println(path + " is not writable");
        	}
    	}

	public static void main(String[] args) {
		DockerComposeExecutor dockerExecutor = new DockerComposeExecutor();
		dockerExecutor.executorService = Executors.newCachedThreadPool();
		new MesosExecutorDriver(dockerExecutor).run();
	}

}
