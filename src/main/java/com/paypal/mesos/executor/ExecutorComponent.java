package com.paypal.mesos.executor;

import dagger.Component;
import org.apache.mesos.Executor;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ComposeFileListModule.class, ExecutorModule.class})
public interface ExecutorComponent {

    Executor getExecutor();

}
