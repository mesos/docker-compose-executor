package com.paypal.mesos.executor;

import javax.inject.Singleton;

import org.apache.mesos.Executor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.paypal.mesos.executor.config.Config;
import com.paypal.mesos.executor.fetcher.FileFetcher;

import dagger.Module;
import dagger.Provides;

@Module
public class ExecutorModule {

	@Provides Executor provideDockerComposeExecutor(FileFetcher fileFetcher,DockerEventStreamListener streamListener,
			DockerEventObserver eventObserver,DockerComposeProcessObserver processObserver){
		return new DockerComposeExecutor(fileFetcher,streamListener,eventObserver,processObserver);
	}
	
	@Provides @Singleton DockerEventStreamListener provideDockerEventStreamListener(DockerClient dockerClient){
		return new DockerEventStreamListener(dockerClient);
	}
	
	@Provides @Singleton DockerEventObserver provideDockerEventObserver(DockerClient dockerClient){
		return new DockerEventObserver(dockerClient);
	}
	
	@Provides @Singleton DockerComposeProcessObserver provideDockerComposeProcessObserver(){
		return new DockerComposeProcessObserver();
	}
	
	@Provides @Singleton DockerClient provideDockerClient(){
		return  DockerClientBuilder.getInstance(Config.DOCKER_SERVER_URL).build();
	}
	

}
