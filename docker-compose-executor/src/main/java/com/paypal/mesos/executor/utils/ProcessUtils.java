package com.paypal.mesos.executor.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

public class ProcessUtils {

	public static int executeCommand(String command,ExecuteWatchdog watchdog) throws IOException{
		CommandLine cmdLine = CommandLine.parse(command);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(new PumpStreamHandler(null, null, null));
		executor.setExitValues(new int[]{0, 1});
		if(watchdog != null){
			executor.setWatchdog(watchdog);
		}
		int exitValue = executor.execute(cmdLine);
		return exitValue;
	}
	
	public static ExecuteWatchdog createTimeoutWatchdog(TimeUnit timeunit,int timeout){
		ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
		return timeoutWatchdog;
	}
	
}
