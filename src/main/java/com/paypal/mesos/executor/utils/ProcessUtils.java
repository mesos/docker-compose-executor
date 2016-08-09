package com.paypal.mesos.executor.utils;

import org.apache.commons.exec.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

    private static Logger log = Logger.getLogger(ProcessUtils.class);

    public static int executeCommand(String command, ExecuteWatchdog watchdog) {
        return executeCommand(command, watchdog, null, null, null);
    }

    public static int executeCommand(String command, ExecuteWatchdog watchdog, OutputStream outputStream, OutputStream errorStream, InputStream inputStream) {
        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        if (outputStream == null) {
            outputStream = System.out;
        }
        if (errorStream == null) {
            errorStream = System.err;
        }
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream, inputStream));
        executor.setExitValues(new int[]{0, 1});
        if (watchdog != null) {
            executor.setWatchdog(watchdog);
        }
        int exitValue = 0;
        try {
            exitValue = executor.execute(cmdLine);
        } catch (IOException e) {
            exitValue = 1;
            log.error("error executing command", e);
        }

        return exitValue;
    }

    public static ExecuteWatchdog createTimeoutWatchdog(TimeUnit timeunit, int timeout) {
        ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
        return timeoutWatchdog;
    }

    public static boolean isProcessRunning(int pid) {
        String line;
        if (OS.isFamilyWindows()) {
            line = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
        } else {
            line = "ps -p " + pid;
        }
        int exitValue = ProcessUtils.executeCommand(line, null);
        return exitValue == 0;
    }

}
