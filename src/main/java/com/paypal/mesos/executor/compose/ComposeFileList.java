package com.paypal.mesos.executor.compose;

import org.apache.mesos.Protos.TaskInfo;

import java.io.IOException;
import java.util.List;

public interface ComposeFileList {

    List<String> getFile(TaskInfo taskInfo) throws IOException;

}
