package com.paypal.mesos.executor.pluginapi;

import com.paypal.mesos.executor.compose.ComposeFileList;
import com.paypal.mesos.executor.compose.ComposeFileListImpl;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import ro.fortsoft.pf4j.Extension;

import java.io.IOException;
import java.util.List;

/**
 * Created by kkrishna on 7/18/16.
 */
@Extension
public class ComposeExecutorPluginImpl implements ComposeExecutorPlugin {
    @Override
    public List<String> launchTask(ExecutorDriver executorDriver, Protos.TaskInfo taskInfo)  {
        System.out.println(" ######### ComposeExecutorPlugin launchTask #############");
        System.out.println("  ExecutorDriver: " + executorDriver.toString());
        System.out.println("  taskInfo: "+ taskInfo.toString());
        ComposeFileList composeFiles = new ComposeFileListImpl();
        Protos.TaskID taskId = taskInfo.getTaskId();

        try {
            return  composeFiles.getFile(taskInfo);
        } catch (IOException e) {
            e.printStackTrace();
            Protos.TaskStatus taskStatus = Protos.TaskStatus.newBuilder().setTaskId(taskId).setState(Protos.TaskState.TASK_FAILED).build();
            executorDriver.sendStatusUpdate(taskStatus);
        }

//        Protos.Labels  labels = taskInfo.getLabels();
//        System.out.println("taskInfo labels: " + labels.toString());
//        labels.toBuilder().addLabels(Protos.Label.newBuilder().setKey("testkey").setValue("testvalue")).build();
//        taskInfo.toBuilder().setLabels(labels).build();
//
//        System.out.println("  taskInfo: " + taskInfo.toString());
        return  null;
    }

    @Override
    public void shutdown() {
        System.out.println(" ############## ComposeExecutorPlugin Shutdown ############");
    }

    @Override
    public List<String> getComposeFiles() {
        return null;
    }
}
