package com.paypal.mesos.executor;

import org.apache.log4j.Logger;
import org.apache.mesos.Protos.TaskID;

import rx.Observer;

public class DockerComposeProcessObserver implements Observer<Integer> {

    private static final Logger log = Logger.getLogger(DockerComposeProcessObserver.class);

    private DockerComposeExecutor executor;
    private TaskID taskId;

    public DockerComposeProcessObserver() {

    }

    public void init(DockerComposeExecutor executor, TaskID taskId) {
        this.executor = executor;
        this.taskId = taskId;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        log.error("failed to pull images or bring up docker-compose executor for taskId:" + taskId.getValue(), e);
        executor.suicide(taskId, 1);
    }

    @Override
    public void onNext(Integer t) {
        log.info("executor for taskId:" + taskId.getValue() + " exited with exitCode:" + t);
        executor.suicide(taskId, t);
    }

}
