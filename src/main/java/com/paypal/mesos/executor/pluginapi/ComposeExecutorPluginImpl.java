package com.paypal.mesos.executor.pluginapi;

import com.paypal.mesos.executor.compose.ComposeFileList;
import com.paypal.mesos.executor.compose.ComposeFileListImpl;
import org.apache.log4j.Logger;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import ro.fortsoft.pf4j.Extension;

import java.io.IOException;
import java.util.List;

/*
 * Introduced to establish a hook at launchTask and shutdown executor callbacks.
 * Default implementation for launchTask, retrieves yaml files from TaskInfo object. This method is to be over-ridden as needed on case by case basis.
 * launchTask method should return list of yaml files path.
 */
@Extension
public class ComposeExecutorPluginImpl implements ComposeExecutorPlugin {

    private static final Logger log = Logger.getLogger(ComposeExecutorPluginImpl.class);

    @Override
    public List<String> launchTask(ExecutorDriver executorDriver, Protos.TaskInfo taskInfo) {

        if (log.isDebugEnabled()) {
            log.debug(" ######### ComposeExecutorPlugin launchTask #############");
            log.debug("  ExecutorDriver: " + executorDriver.toString());
            log.debug("  taskInfo: " + taskInfo.toString());
        }

        ComposeFileList composeFiles = new ComposeFileListImpl();
        Protos.TaskID taskId = taskInfo.getTaskId();

        try {
            return composeFiles.getFile(taskInfo);
        } catch (IOException e) {
            e.printStackTrace();
            Protos.TaskStatus taskStatus = Protos.TaskStatus.newBuilder().setTaskId(taskId).setState(Protos.TaskState.TASK_FAILED).build();
            executorDriver.sendStatusUpdate(taskStatus);
        }

        return null;
    }

    @Override
    public void shutdown() {
        log.debug(" ############## ComposeExecutorPlugin Shutdown ############");
    }

}
