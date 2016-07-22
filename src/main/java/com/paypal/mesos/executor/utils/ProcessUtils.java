package com.paypal.mesos.executor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

public class ProcessUtils {

	private static Logger log = Logger.getLogger(ProcessUtils.class);
	
	public static int executeCommand(String command,ExecuteWatchdog watchdog) {
		return executeCommand(command, watchdog,null,null,null);
	}
	
	public static int executeCommand(String command,ExecuteWatchdog watchdog,OutputStream outputStream,OutputStream errorStream,InputStream inputStream){
		System.out.println(" command: "+command);
		CommandLine cmdLine = CommandLine.parse(command);
		DefaultExecutor executor = new DefaultExecutor();
		if(outputStream == null){
			outputStream = System.out;
		}
		if(errorStream == null){
			errorStream = System.err;
		}
		executor.setStreamHandler(new PumpStreamHandler(outputStream,errorStream, inputStream));
		executor.setExitValues(new int[]{0, 1});
		if(watchdog != null){
			executor.setWatchdog(watchdog);
		}
		int exitValue = 0;
		try {
			exitValue = executor.execute(cmdLine);
		} catch (IOException e) {
			exitValue = 1;
			log.error("error executing command", e);
		}

		System.out.println(" output: "+outputStream.toString());
		return exitValue;
	}
	
	public static ExecuteWatchdog createTimeoutWatchdog(TimeUnit timeunit,int timeout){
		ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
		return timeoutWatchdog;
	}
	
	public static  boolean isProcessRunning(int pid) {
		String line;
		if (OS.isFamilyWindows()) {
			line = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
		}
		else {
			line = "ps -p " + pid;
		}
		int exitValue = ProcessUtils.executeCommand(line, null);
		return exitValue == 0;
	}
	
}
