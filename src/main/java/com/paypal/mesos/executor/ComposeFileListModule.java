package com.paypal.mesos.executor;

import javax.inject.Singleton;

import com.paypal.mesos.executor.compose.ComposeFileListImpl;
import com.paypal.mesos.executor.compose.ComposeFileList;

import dagger.Module;
import dagger.Provides;

@Module
public class ComposeFileListModule {

	@Provides @Singleton ComposeFileList provideComposeFileList(){
		return new ComposeFileListImpl();
	}

}
