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

public class DockerComposeExecutor implements Executor {

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

    private volatile boolean isShutDownInProgress = false;

    private boolean pluginEnabled = false;

    @Inject
    public DockerComposeExecutor(ComposeFileList fileFetcher, DockerComposeProcessObserver processObserver,
                                 ComposeMonitor podMonitor, ComposeRewriteHelper helper) {
        this.fileFetcher = fileFetcher;
        this.processObserver = processObserver;
        this.podMonitor = podMonitor;
        this.composeRewriteHelper = helper;
    }

    @Override
    public void launchTask(ExecutorDriver executorDriver, TaskInfo taskInfo) {
        if (log.isDebugEnabled()) {
            log.debug(" ############ launchTask #############");
            log.debug(" taskInfo: " + taskInfo.toString());
            log.debug(" executorDriver: " + executorDriver.toString());
        }
        TaskID taskId = taskInfo.getTaskId();
        processObserver.init(this, taskId);
        sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_STARTING);
        try {
            this.fileNames = executePlugin(executorDriver, taskInfo);
            if (this.fileNames == null || this.fileNames.size() == 0) {
                sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FAILED);
                return;
            }

            this.fileNames = updateYamlFiles(this.fileNames, executorInfo, taskInfo);
            log.debug(" fileNames: " + this.fileNames.toString());
            podMonitor.subscribeToChanges(new Action1<Integer>() {
                @Override
                public void call(Integer exitCode) {
                    suicide(taskId, exitCode);
                }
            });
            podMonitor.startMonitoring(this.fileNames);
            updateImagesAndStartCompose(taskId);
            log.debug("sending TASK_RUNNING status update");
            sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_RUNNING);
        } catch (Exception e) {
            log.error("exception while launching process", e);
            sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FAILED);
            System.exit(1);
        }
    }

    private List<String> executePlugin(ExecutorDriver executorDriver, TaskInfo taskInfo) {
        if (this.pluginManager != null) {
            List<ComposeExecutorPlugin> plugins = this.pluginManager.getExtensions(ComposeExecutorPlugin.class);
            log.debug(String.format("Found %d extensions for extension point '%s'", plugins.size(), ComposeExecutorPlugin.class.getName()));
            //check to ensure that only a single plugin exists..
            if (plugins != null && plugins.size() > 1) {
                log.error(" more than one extension implementation: " + plugins.toString());
                return null;
            }
            return plugins.get(0).launchTask(executorDriver, taskInfo);
        } else {
            log.error(" pluginManager NULL ");
        }
        return null;
    }

    private void shutdownPlugin() {
        if (this.pluginManager != null) {
            List<ComposeExecutorPlugin> plugins = this.pluginManager.getExtensions(ComposeExecutorPlugin.class);
            log.debug(String.format("Found %d extensions for extension point '%s'", plugins.size(), ComposeExecutorPlugin.class.getName()));
            if (plugins != null && plugins.size() > 1) {
                log.error("More than one extension implementation: " + plugins.toString());
                return ;
            }
            plugins.get(0).shutdown();
        }
    }

    private void updateImagesAndStartCompose(TaskID taskId) {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                String pullCommand = CommandBuilder.pullImages(fileNames);
                log.debug(" pullCommand: " + pullCommand);
                int imageUpdateExitCode = ProcessUtils.executeCommand(pullCommand, null);
                if (imageUpdateExitCode != 0) {
                    log.error("unable to pull updated images trying to bring the pod up with existing images");
                }
                String launchCommand = CommandBuilder.launchTask(fileNames);
                log.debug(" launchCommand: " + launchCommand);
                int exitCode = ProcessUtils.executeCommand(launchCommand, null);
                log.debug("updateImagesAndStartCompose exit code: "+exitCode);
                subscriber.onNext(exitCode);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(processObserver);
    }

    public void suicide(TaskID taskId, int exitCode) {

        if (log.isDebugEnabled()) {
            log.debug(" ############## suicide #######");
            if (taskId != null)
                log.debug("taskId: " + taskId.toString() + "  exitCode: " + exitCode);
        }

        if (this.isShutDownInProgress && exitCode == 0) {
            // proceed with finish task
            int stopContainers = cleanUp();
            if (this.pluginManager != null) {
                this.pluginManager.stopPlugins();
            }
            log.debug(" cleanUp exit code: " + stopContainers);
            sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FINISHED);
            System.exit(0);
        } else {
            if (!isShutDownInProgress) {
                int stopContainers = cleanUp();
                if (this.pluginManager != null) {
                    this.pluginManager.stopPlugins();
                }
                sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FAILED);
                System.exit(1);
            } else {
                log.debug(" shutdown already in progress..");
            }

        }
//
//        int stopContainers = cleanUp();
//
//        if (this.pluginManager != null) {
//            this.pluginManager.stopPlugins();
//        }
//
//        if (exitCode == 0 && stopContainers == 0) {
//            sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FINISHED);
//            System.exit(0);
//        } else {
//            sendTaskStatusUpdate(executorDriver, taskId, TaskState.TASK_FAILED);
//            System.exit(1);
//        }
    }


    /**
     * stop docker-compose
     * force remove docker images
     *
     * @return killing compose and removing all images are successful
     */
    private int cleanUp() {
        if (log.isDebugEnabled()) {
            log.debug(" ##############  cleanUp ###########,   fileName: " + fileNames);
        }
        String killTask = CommandBuilder.stopTask(fileNames);
        log.debug(" killTask: " + killTask);
        int exitCode = ProcessUtils.executeCommand(killTask, null);
        log.debug(" cleanUp killTask exitCode: " + exitCode);

        if (exitCode != 0) {
            exitCode = linuxKill(fileNames);
        }
        return exitCode;
    }

    private int linuxKill(List<String> files) {
        log.debug("########### linuxKill ##########, fileName: " + files);
        List<String> containerIds = podMonitor.getContainerIds(files);
        List<Integer> pids = new ArrayList<Integer>();
        for (String containerId : containerIds) {
            log.debug(" containerId: " + containerId);
            ContainerDetails details = podMonitor.getContainerDetails(containerId);
            log.debug(" details: " + details);
            int pid = details.getPid();
            if (pid != 0) {
                pids.add(pid);
            }
        }
        int exitCode = 1;
        if (pids.size() > 0) {
            log.debug("pids: " + pids.toString());
            String command = CommandBuilder.linuxKill(pids);
            log.debug(" command: " + command);
            exitCode = ProcessUtils.executeCommand(command, null);
        }
        log.debug(" linuxKill exitCode: "+exitCode);
        return exitCode;
    }

    private void sendTaskStatusUpdate(ExecutorDriver executorDriver, TaskID taskId, TaskState taskState) {
        if (taskId != null) {
            TaskStatus taskStatus = TaskStatus.newBuilder().setTaskId(taskId).setState(taskState).build();
            executorDriver.sendStatusUpdate(taskStatus);
        } else {
            log.error("taskId is null");
        }
    }

    @Override
    public void killTask(ExecutorDriver executorDriver, TaskID taskId) {
        log.info("killTask, taskId:" + taskId.getValue());
        this.isShutDownInProgress = true;
        suicide(taskId, 0);
    }

    @Override
    public void disconnected(ExecutorDriver executorDriver) {
        log.debug("executor disconnected");
    }

    @Override
    public void error(ExecutorDriver executorDriver, String errorMessage) {
        log.error("executor received an error message:" + errorMessage);
    }

    @Override
    public void frameworkMessage(ExecutorDriver arg0, byte[] arg1) {
        log.debug("received framework message");
    }

    @Override
    public void registered(ExecutorDriver executorDriver, ExecutorInfo executorInfo,
                           FrameworkInfo frameworkInfo, SlaveInfo slaveInfo) {
        log.debug("executor registered with framework:" + frameworkInfo.getName() + ":on slave:" + slaveInfo.getHostname());
        this.executorInfo = executorInfo;
        this.executorDriver = executorDriver;
    }

    @Override
    public void reregistered(ExecutorDriver executorDriver, SlaveInfo slaveInfo) {
        log.debug("executor reregistered on slave:" + slaveInfo.getHostname());
        this.executorDriver = executorDriver;
    }

    @Override
    public void shutdown(ExecutorDriver executorDriver) {
        log.debug("shutting down executor");
        this.isShutDownInProgress = true;
        shutdownPlugin();
        suicide(null, 0);
    }


    public List<String> updateYamlFiles(List<String> paths, ExecutorInfo executorInfo, TaskInfo taskInfo) throws IOException {
        List<String> newPath = new ArrayList<>();
        for (String file : paths) {
            Map<String, Map<String, Map<String, Object>>> rootYaml = readFromFile(file);
            if (log.isDebugEnabled())
                log.debug(" rootYaml dump: " + rootYaml.toString());
            Map<String, Map<String, Map<String, Object>>> updatedYaml = composeRewriteHelper.updateYaml(rootYaml, taskInfo, executorInfo);
            if (log.isDebugEnabled())
                log.debug(" updatedYaml dump: " + updatedYaml.toString());
            String outputFileName = getOutputFileName(file);
            log.debug("outputFileName: " + outputFileName);
            writeToFile(outputFileName, updatedYaml);
            newPath.add(outputFileName);
        }
        log.debug("updated Paths: " + newPath.toString());
        return newPath;
    }

    private String getOutputFileName(String path) {
        if (path != null) {
            StringBuffer buf = new StringBuffer(path.trim());
            buf.append(GENERATED_YAML_FILE_NAME);
            return buf.toString();
        }
        return null;
    }

    private File writeToFile(String fileName, Map<String, Map<String, Map<String, Object>>> updatedRootYaml) throws IOException, FileNotFoundException {
        File file = new File(fileName);
        FileWriter fileWriter = new FileWriter(file);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        yaml.dump(updatedRootYaml, fileWriter);
        fileWriter.flush();
        fileWriter.close();
        return file;
    }

    private Map<String, Map<String, Map<String, Object>>> readFromFile(String path) throws FileNotFoundException, IOException {
        FileReader fileReader = new FileReader(new File(path));
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Object>>> yamlMap = (Map<String, Map<String, Map<String, Object>>>) yaml.load(fileReader);
        fileReader.close();
        return yamlMap;
    }

}
