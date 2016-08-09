package com.paypal.mesos.executor;

import com.paypal.mesos.executor.config.Config;

import java.util.List;


public class CommandBuilder {

    public static String launchTask(List<String> files) {
        StringBuffer buf = new StringBuffer("docker-compose ");
        for (String file : files) {
            buf.append(" -f " + file);
        }
        buf.append(" up");
        return buf.toString();
        //return "docker-compose -f "+fileName+" up";
    }

    public static String pullImages(List<String> files) {
        StringBuffer buf = new StringBuffer("docker-compose ");
        for (String file : files) {
            buf.append(" -f " + file);
        }
        buf.append(" pull");

        if (Config.IGNORE_PULL_FAILURES) {
            buf.append("  --ignore-pull-failures");
        }
        return buf.toString();

//		String command = "docker-compose -f "+fileName+" pull";
//		if(Config.IGNORE_PULL_FAILURES){
//			command = command+"  --ignore-pull-failures";
//		}
//		return command;
    }

    public static String stopTask(List<String> files) {
        StringBuffer buf = new StringBuffer("docker-compose ");
        for (String file : files) {
            buf.append(" -f " + file);
        }
        buf.append(" stop");
        return buf.toString();
        //return "docker-compose -f "+fileName+" stop ";
    }

    public static String getContainerIds(List<String> files) {
        StringBuffer buf = new StringBuffer("docker-compose ");
        for (String file : files) {
            buf.append(" -f " + file);
        }
        buf.append(" ps -q");
        return buf.toString();
        //return "docker-compose -f "+fileName + " ps -q";
    }

    public static String linuxKill(List<Integer> pids) {
        StringBuilder processIds = new StringBuilder();
        for (int pid : pids) {
            processIds.append(pid).append(" ");
        }
        return "sudo kill -9 " + processIds.toString();
    }

    public static String getContainerDetails(String containerId) {
        return "docker inspect --format='{{.State.Pid}},{{.State.ExitCode}},{{.State.Running}},{{.RestartCount}},{{.HostConfig.RestartPolicy.MaximumRetryCount}}' " + containerId;
    }

}
