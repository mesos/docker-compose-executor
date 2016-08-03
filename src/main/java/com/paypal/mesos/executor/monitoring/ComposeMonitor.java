package com.paypal.mesos.executor.monitoring;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

import com.paypal.mesos.executor.CommandBuilder;
import com.paypal.mesos.executor.config.Config;
import com.paypal.mesos.executor.utils.ProcessUtils;

//TODO remode
public class ComposeMonitor {

    private static final Logger log = Logger.getLogger(ComposeMonitor.class);

    public final BehaviorSubject<Integer> monitor;

    public ComposeMonitor() {
        monitor = BehaviorSubject.create();
    }

    public Subscription subscribeToChanges(Action1<Integer> action) {
        return monitor.subscribe(action);
    }

    public void startMonitoring(final List<String> fileNames) {
        log.info("start montioring is called:" + fileNames);
        Observable.interval(Config.POD_MONITOR_INTERVAL, TimeUnit.MILLISECONDS).subscribe(new Observer<Long>() {

            @Override
            public void onCompleted() {
                log.info("monitor thread on completed :completed monitoring compose for file:" + fileNames);
            }

            @Override
            public void onError(Throwable e) {
                log.error("monitor thread on error: encountred an error monitoring:" + fileNames, e);
                monitor.onNext(1);
            }

            @Override
            public void onNext(Long t) {
                List<String> containerIds = getContainerIds(fileNames);
                if (log.isDebugEnabled())
                    log.debug(Arrays.toString(containerIds.toArray()));
                if (containerIds != null) {
                    for (String containerId : containerIds) {
                        ContainerDetails details = getContainerDetails(containerId);
                        int exitCode = details.getExitCode();
                        int restartCount = details.getRestartCount();
                        if (details.isRunning() == false && exitCode != 0 && restartCount == details.getMaxAllowedRestartCount()) {
                            RestartPolicyException exception = new RestartPolicyException(containerId, exitCode, restartCount);
                            onError(exception);
                        }
                    }
                }
            }
        });

    }

    public List<String> getContainerIds(List<String> fileNames) {
        List<String> containerIds = new ArrayList<String>();
        String listConatinerIdsCommand = CommandBuilder.getContainerIds(fileNames);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int exitCode = ProcessUtils.executeCommand(listConatinerIdsCommand, null, outputStream, null, null);
        if (exitCode == 0) {
            String commandOutput = outputStream.toString();
            containerIds = parseListCommandOutput(commandOutput);
        }
        return containerIds;
    }

    private List<String> parseListCommandOutput(String output) {
        List<String> containersIds = new ArrayList<String>();
        if (output != null) {
            StringTokenizer tokenizer = new StringTokenizer(output, "\n");
            while (tokenizer.hasMoreTokens()) {
                containersIds.add(tokenizer.nextToken().trim());
            }
        }
        return containersIds;
    }

    public ContainerDetails getContainerDetails(String containerId) {
        String detailsCommand = CommandBuilder.getContainerDetails(containerId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int exitCode = ProcessUtils.executeCommand(detailsCommand, null, outputStream, null, null);
        if (exitCode == 0) {
            String details = outputStream.toString();
            ContainerDetails containerDetails = parseDetails(containerId, details);
            return containerDetails;
        } else {
            throw new ContainerDetailRetrievalException(containerId);
        }
    }

    public ContainerDetails parseDetails(String containerId, String details) {
        StringTokenizer tokenizer = new StringTokenizer(details, ",");
        if (tokenizer.countTokens() == 5) {
            try {
                int pid = Integer.parseInt(tokenizer.nextToken());
                int exitCode = Integer.parseInt(tokenizer.nextToken());
                boolean isRunning = Boolean.parseBoolean(tokenizer.nextToken());
                int restartCount = Integer.parseInt(tokenizer.nextToken());
                int maxRestartCount = Integer.parseInt(tokenizer.nextToken().trim());
                return new ContainerDetails(containerId, isRunning, restartCount, maxRestartCount, exitCode, pid);
            } catch (Exception exception) {
                log.error("problem while parsing container details for containerId:" + containerId + ":details string is:" + details, exception);
                throw new ContainerDetailRetrievalException(containerId);
            }
        } else {
            throw new ContainerDetailRetrievalException(containerId);
        }
    }


}
