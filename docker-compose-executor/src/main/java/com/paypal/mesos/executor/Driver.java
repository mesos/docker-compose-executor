package com.paypal.mesos.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mesos.MesosExecutorDriver;

public class Driver{
   public static void main(String...args) {
      DockerComposeExecutor dockerExecutor = new DockerComposeExecutor();
      dockerExecutor.setExecutorService(Executors.newCachedThreadPool());
      new MesosExecutorDriver(dockerExecutor).run();
   }
}
