package com.paypal.mesos.executor;

import javax.inject.Singleton;

import com.paypal.mesos.executor.compose.ComposeRewriteHelper;
import org.apache.mesos.Executor;

import com.paypal.mesos.executor.compose.ComposeFileList;
import com.paypal.mesos.executor.monitoring.ComposeMonitor;

import dagger.Module;
import dagger.Provides;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Module
public class ExecutorModule {

	@Provides Executor provideDockerComposeExecutor(ComposeFileList fileFetcher,DockerComposeProcessObserver processObserver,
													ComposeMonitor composeMonitor, ComposeRewriteHelper helper){
		return new DockerComposeExecutor(fileFetcher,processObserver,composeMonitor, helper);
	}
	
	@Provides @Singleton ComposeMonitor provideComposeMonitor(){
		return new ComposeMonitor();
	}
	
	@Provides @Singleton DockerComposeProcessObserver provideDockerComposeProcessObserver(){
		return new DockerComposeProcessObserver();
	}

	@Provides @Singleton
	ComposeRewriteHelper provideComposeRewriteHelper() {
		return new ComposeRewriteHelper();
	}

	@Provides @Singleton Yaml provideYaml(){
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

}
