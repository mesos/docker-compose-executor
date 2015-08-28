package com.paypal.mesos.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mesos.Executor;

import com.paypal.mesos.executor.fetcher.FileFetcher;
import com.paypal.mesos.executor.processbuilder.ProcessBuilderProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class ExecutorModule {

	@Provides Executor provideDockerComposeExecutor(FileFetcher fileFetcher,ProcessBuilderProvider processBuilder,ExecutorService executorService){
		return new DockerComposeExecutor(fileFetcher, processBuilder, executorService);
	}

	@Provides ExecutorService provideExecutorService(){
		return  Executors.newCachedThreadPool();
	}
}
