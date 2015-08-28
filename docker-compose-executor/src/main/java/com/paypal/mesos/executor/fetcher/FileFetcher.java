package com.paypal.mesos.executor.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.mesos.Protos.TaskInfo;

public interface FileFetcher {

	public File getFile(TaskInfo taskInfo) throws IOException,FileNotFoundException;
	
}
