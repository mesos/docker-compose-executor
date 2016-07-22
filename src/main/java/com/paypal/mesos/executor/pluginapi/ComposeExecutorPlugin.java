package com.paypal.mesos.executor.pluginapi;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import ro.fortsoft.pf4j.ExtensionPoint;

import java.io.IOException;
import java.util.List;

/**
 * Created by kkrishna on 7/18/16.
 */
public interface ComposeExecutorPlugin extends ExtensionPoint {

    public  List<String> launchTask(ExecutorDriver executorDriver,  Protos.TaskInfo taskInfo);

    public  void shutdown();

    public List<String> getComposeFiles();

}
