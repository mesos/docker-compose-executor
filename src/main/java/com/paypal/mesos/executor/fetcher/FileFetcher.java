package com.paypal.mesos.executor.fetcher;

import java.io.File;
import java.io.IOException;

import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.TaskInfo;

public interface FileFetcher {

	File getFile(ExecutorInfo executorInfo,TaskInfo taskInfo) throws IOException;
	
}
