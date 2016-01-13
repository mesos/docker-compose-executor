package com.paypal.mesos.executor;

import javax.inject.Singleton;

import org.apache.mesos.Executor;

import com.paypal.mesos.executor.fetcher.FileFetcher;
import com.paypal.mesos.executor.monitoring.ComposeMonitor;

import dagger.Module;
import dagger.Provides;

@Module
public class ExecutorModule {

	@Provides Executor provideDockerComposeExecutor(FileFetcher fileFetcher,DockerComposeProcessObserver processObserver,ComposeMonitor composeMonitor){
		return new DockerComposeExecutor(fileFetcher,processObserver,composeMonitor);
	}
	
	@Provides @Singleton ComposeMonitor provideComposeMonitor(){
		return new ComposeMonitor();
	}
	
	@Provides @Singleton DockerComposeProcessObserver provideDockerComposeProcessObserver(){
		return new DockerComposeProcessObserver();
	}

}
