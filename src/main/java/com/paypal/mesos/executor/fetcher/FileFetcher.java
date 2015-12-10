package com.paypal.mesos.executor.fetcher;

import java.io.File;
import java.io.IOException;

import org.apache.mesos.Protos.TaskInfo;

public interface FileFetcher {

	File getFile(TaskInfo taskInfo) throws IOException;
	
}
