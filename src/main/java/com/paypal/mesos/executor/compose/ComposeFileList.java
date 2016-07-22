package com.paypal.mesos.executor.compose;

import java.io.IOException;
import java.util.List;

import org.apache.mesos.Protos.TaskInfo;

public interface ComposeFileList {

	List<String> getFile(TaskInfo taskInfo) throws IOException;
	
}
