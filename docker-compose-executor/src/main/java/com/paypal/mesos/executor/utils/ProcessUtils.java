package com.paypal.mesos.executor.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

public class ProcessUtils {

	private static Logger log = Logger.getLogger(ProcessUtils.class);
	
	public static int executeCommand(String command,ExecuteWatchdog watchdog) {
		CommandLine cmdLine = CommandLine.parse(command);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(new PumpStreamHandler(null, null, null));
		executor.setExitValues(new int[]{0, 1});
		if(watchdog != null){
			executor.setWatchdog(watchdog);
		}
		int exitValue = 0;
		try {
			log.info("command received is:"+command);
			exitValue = executor.execute(cmdLine);
		} catch (IOException e) {
			exitValue = 1;
			log.error("error executing command", e);
		}
		return exitValue;
	}
	
	public static ExecuteWatchdog createTimeoutWatchdog(TimeUnit timeunit,int timeout){
		ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
		return timeoutWatchdog;
	}
	
}
