package com.paypal.mesos.executor;

import javax.inject.Singleton;

import org.apache.mesos.Executor;

import dagger.Component;

@Singleton
@Component(modules = { FileFetcherModule.class,ProcessBuilderModule.class,ExecutorModule.class})
public interface ExecutorComponent {

	Executor getExecutor();
	
}
