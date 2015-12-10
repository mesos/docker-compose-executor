package com.paypal.mesos.executor;

import javax.inject.Singleton;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.paypal.mesos.executor.fetcher.DockerComposeFileFetcher;
import com.paypal.mesos.executor.fetcher.DockerRewriteHelper;
import com.paypal.mesos.executor.fetcher.FileFetcher;

import dagger.Module;
import dagger.Provides;

@Module
public class FileFetcherModule {

	@Provides @Singleton FileFetcher provideComposeFileFetcher(DockerRewriteHelper helper,Yaml yaml){
		return new DockerComposeFileFetcher(helper,yaml);
	}

	@Provides @Singleton DockerRewriteHelper provideRewriteHelper(){
		return new DockerRewriteHelper();
	}

	@Provides @Singleton Yaml provideYaml(){
		DumperOptions options=new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

}
