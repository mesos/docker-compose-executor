package com.paypal.mesos.executor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.paypal.mesos.executor.compose.ComposeRewriteHelper;
import com.paypal.mesos.executor.pluginapi.ComposeExecutorPlugin;
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

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import com.paypal.mesos.executor.compose.ComposeFileList;
import com.paypal.mesos.executor.monitoring.ComposeMonitor;
import com.paypal.mesos.executor.monitoring.ContainerDetails;
import com.paypal.mesos.executor.utils.ProcessUtils;

public class DockerComposeExecutor implements Executor{

	private static final Logger log = Logger.getLogger(DockerComposeExecutor.class);
	private static final String GENERATED_YAML_FILE_NAME = "-generated.yml";


	private ComposeFileList fileFetcher;

	private List<String> fileNames;

	private ExecutorDriver executorDriver;
	
	private DockerComposeProcessObserver processObserver;

	private ExecutorInfo executorInfo;

	private ComposeMonitor podMonitor;

	private ComposeRewriteHelper composeRewriteHelper;

	final PluginManager pluginManager = new DefaultPluginManager();

	private boolean pluginEnabled = false;

	@Inject
	public DockerComposeExecutor(ComposeFileList fileFetcher,DockerComposeProcessObserver processObserver,
								 ComposeMonitor podMonitor, ComposeRewriteHelper helper){
		this.fileFetcher = fileFetcher;
		this.processObserver = processObserver;
		this.podMonitor = podMonitor;
		this.composeRewriteHelper = helper;
		//load/start plugins needed only if plugin is loaded from separate jar
//		this.pluginManager.loadPlugins();
//		this.pluginManager.startPlugins();
	}

	@Override
	public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
		System.out.println(" ############ Launch Task START #############");
		System.out.println(" taskInfo: "+taskInfo.toString());
		System.out.println(" executorDriver: "+executorDriver.toString());
		TaskID taskId = taskInfo.getTaskId();


		System.out.println(" taskId: " + taskId.getValue());

		processObserver.init(this, taskId);
		sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_STARTING);
		try {
			//this.fileNames = fileFetcher.getFile(taskInfo);
			this.fileNames = executePlugin(executorDriver, taskInfo);
			if (this.fileNames == null || this.fileNames.size() == 0) {
				sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
				return;
			}

			this.fileNames = updateYamlFiles(this.fileNames, executorInfo, taskInfo);
			//this.fileNames = file.getAbsolutePath();
			System.out.println(" fileName: "+this.fileNames);
			podMonitor.subscribeToChanges(new Action1<Integer>() {
				@Override
				public void call(Integer exitCode) {
					suicide(taskId, exitCode);
				}
			});
			podMonitor.startMonitoring(this.fileNames);
			updateImagesAndStartCompose(taskId);
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_RUNNING);
		}catch (Exception e) {
			log.error("exception while launching process",e);
			sendTaskStatusUpdate(executorDriver,taskId,TaskState.TASK_FAILED);
		} 
	}

	private List<String> executePlugin(ExecutorDriver executorDriver, TaskInfo taskInfo)  {
		if (this.pluginManager != null) {
			List<ComposeExecutorPlugin> plugins = this.pluginManager.getExtensions(ComposeExecutorPlugin.class);
			System.out.println(String.format("Found %d extensions for extension point '%s'", plugins.size(), ComposeExecutorPlugin.class.getName()));
			if (plugins.size() > 1)  {
				log.error(" more than one extension implementation: "+plugins.toString());
				return null;
			}
			//check to ensure that only a single plugin exists..
			for (ComposeExecutorPlugin plugin : plugins) {
				return plugin.launchTask(executorDriver, taskInfo);
			}
		}else {
			log.error(" pluginManager NULL ");
		}
		return null;
	}

	private void shutdownPlugin() {
		if (this.pluginManager != null) {
			List<ComposeExecutorPlugin> plugins = this.pluginManager.getExtensions(ComposeExecutorPlugin.class);
			System.out.println(String.format("Found %d extensions for extension point '%s'", plugins.size(), ComposeExecutorPlugin.class.getName()));
			for (ComposeExecutorPlugin plugin : plugins) {
				plugin.shutdown();
			}
		}
	}

	private void updateImagesAndStartCompose(TaskID taskId){
		Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				String pullCommand = CommandBuilder.pullImages(fileNames);
				System.out.println(" pullCommand: "+pullCommand);
				int imageUpdateExitCode = ProcessUtils.executeCommand(pullCommand, null);
				if(imageUpdateExitCode != 0){
					log.error("unable to pull updated images trying to bring the pod up with existing images");
				}
				String launchCommand = CommandBuilder.launchTask(fileNames);
				System.out.println(" launchCommand: "+ launchCommand);
				int exitCode = ProcessUtils.executeCommand(launchCommand, null);
				subscriber.onNext(exitCode);
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(processObserver);
	}

	public void suicide(TaskID taskId,int exitCode){
		System.out.println(" ############## in suicide #######");
		System.out.println("taskId: "+taskId.toString() + "  exitCode: "+exitCode);
		int stopContainers = cleanUp();

		if (this.pluginManager != null) {
			this.pluginManager.stopPlugins();
		}

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
		System.out.println(" ############## Inside cleanUp ###########,   fileName: "+fileNames);
		String killTask = CommandBuilder.stopTask(fileNames);
		System.out.println(" killTask: "+killTask);
		int exitCode = ProcessUtils.executeCommand(killTask,null);
		if(exitCode != 0 ){
			exitCode = linuxKill(fileNames);
		}
		return exitCode;
	}
	
	private int linuxKill(List<String> files){
		System.out.println("########### linuxKill ##########, fileName: "+files);
		List<String> containerIds = podMonitor.getContainerIds(files);
		List<Integer> pids = new ArrayList<Integer>();
		for(String containerId:containerIds){
			System.out.println(" containerId: "+containerId);
			ContainerDetails details = podMonitor.getContainerDetails(containerId);
			System.out.println(" details: "+details);
			int pid = details.getPid();
			if(pid != 0){
				pids.add(pid);
			}
		}
		int exitCode = 1;
		if(pids.size() > 0){
			System.out.println("pids: "+pids.toString());
			String command = CommandBuilder.linuxKill(pids);
			System.out.println(" command: "+command);
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
		shutdownPlugin();
		suicide(null, 0);
	}


	private List<String> updateYamlFiles(List<String> paths, ExecutorInfo executorInfo,TaskInfo taskInfo) throws IOException {
		List<String> newPath = new ArrayList<>();
		for (String file: paths) {
			Map<String,Map<String,Map<String,Object>>> rootYaml = readFromFile(file);
			System.out.println(" rootYaml dump: " + rootYaml.toString());
			Map<String,Map<String,Map<String,Object>>> updatedYaml = composeRewriteHelper.updateYaml(rootYaml, taskInfo, executorInfo);
			System.out.println(" updatedYaml dump: "+updatedYaml.toString());
			String outputFileName = getOutputFileName(file);
			System.out.println("outputFileName: "+outputFileName);
			writeToFile(outputFileName, updatedYaml);
			newPath.add(outputFileName);
		}
		System.out.println("updated Paths: "+newPath.toString());
		return newPath;
	}

	private String getOutputFileName(String path){
		if(path != null ){
			StringBuffer buf = new StringBuffer(path.trim());
			buf.append(GENERATED_YAML_FILE_NAME);
			return  buf.toString();
		}
		return null;
	}

	private File writeToFile(String fileName,Map<String,Map<String,Map<String,Object>>> updatedRootYaml) throws IOException,FileNotFoundException {
		File file = new File(fileName);
		FileWriter fileWriter = new FileWriter(file);
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		yaml.dump(updatedRootYaml,fileWriter);
		fileWriter.flush();
		fileWriter.close();
		return file;
	}

	private Map<String,Map<String,Map<String,Object>>> readFromFile(String path) throws FileNotFoundException,IOException{
		FileReader fileReader = new FileReader(new File(path));
		@SuppressWarnings("unchecked")
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		Map<String,Map<String,Map<String,Object>>> yamlMap = (Map<String,Map<String,Map<String,Object>>>)yaml.load(fileReader);
		fileReader.close();
		return yamlMap;
	}

}
