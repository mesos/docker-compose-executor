package com.paypal.mesos.executor.pluginapi;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import ro.fortsoft.pf4j.ExtensionPoint;

import java.util.List;

/**
 * Introduced to establish a hook at launchTask and shutdown executor callbacks.
 */
public interface ComposeExecutorPlugin extends ExtensionPoint {

    public List<String> launchTask(ExecutorDriver executorDriver, Protos.TaskInfo taskInfo);

    public void shutdown();

}
