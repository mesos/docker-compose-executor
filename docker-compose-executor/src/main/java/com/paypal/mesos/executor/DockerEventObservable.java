package com.paypal.mesos.executor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.EventsResultCallback;

public class DockerEventObservable {

	 private ExecutorService executorService;
	 private DockerClient dockerClient;
	 
	 public DockerEventObservable(ExecutorService executorService,DockerClient dockerClient){
		 this.executorService = executorService;
		 this.dockerClient = dockerClient;
	 }
	 
	 public void captureContainerDetails(PublishSubject<Event> subject){
		 
	    	
	    	EventsResultCallback callback = new EventsResultCallback() {
			    @Override
			    public void onNext(Event event) {
			       super.onNext(event);
			       subject.onNext(event);
			      // System.out.println("onNext called observable:"+event);
			    }
			    
			    @Override
			    public void onError(Throwable throwable) {
			    	super.onError(throwable);
			    	subject.onError(throwable);
			    	System.out.println("onError called observable"+throwable.getMessage());
			    	throwable.printStackTrace(System.out);
			    }
			    
			    @Override
			    public void onComplete() {
			    	super.onComplete();
			    	subject.onCompleted();
			    	System.out.println("onCompleted called observable");
			    }
			};
			
			executorService.execute(new Runnable() {
				@Override
				public void run() {
						//Filters filters = new Filters();
						//String [] events = {"start","kill","die","stop","destroy"};
						//filters = filters.withFilter("event", events);
						try {
							dockerClient.eventsCmd().exec(callback).awaitCompletion();
						} catch (InterruptedException e) {
							System.out.println("event command exception");
							e.printStackTrace(System.out);
						};
				}
			});
			
	    }
}
