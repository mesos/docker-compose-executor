package com.paypal.mesos.executor;

import com.paypal.mesos.executor.compose.ComposeFileList;
import com.paypal.mesos.executor.compose.ComposeFileListImpl;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class ComposeFileListModule {

    @Provides
    @Singleton
    ComposeFileList provideComposeFileList() {
        return new ComposeFileListImpl();
    }

}
