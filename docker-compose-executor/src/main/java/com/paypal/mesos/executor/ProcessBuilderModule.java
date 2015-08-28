package com.paypal.mesos.executor;

import javax.inject.Singleton;

import com.paypal.mesos.executor.processbuilder.ProcessBuilderProvider;
import com.paypal.mesos.executor.processbuilder.ProcessBuilderProviderImpl;

import dagger.Module;
import dagger.Provides;

@Module
public class ProcessBuilderModule {
	
	@Provides @Singleton ProcessBuilderProvider provideProcessBuilder(){
		return new ProcessBuilderProviderImpl();
	}
	
}
