package com.paypal.mesos.executor;

import org.apache.log4j.Logger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.command.EventsResultCallback;

/**
 * @author tgadiraju
 * connects to docker bus and listens for events
 */
//TODO add filtering by taskId label when docker supports it.
public class DockerEventStreamListener {

	private static final Logger log = Logger.getLogger(DockerEventStreamListener.class);

	private DockerClient dockerClient;

	public DockerEventStreamListener(DockerClient dockerClient){
		this.dockerClient = dockerClient;
	}

	public Observable<Event> getDockerEvents(){
		return Observable.create(new OnSubscribe<Event>() {
			@Override
			public void call(Subscriber<? super Event> subscriber) {
				DockerRxAdapter adapter = new DockerRxAdapter(subscriber);
				try {
					dockerClient.eventsCmd().exec(adapter).awaitCompletion();
				} catch (InterruptedException e) {
					log.error("interrupted while waiting for docker events",e);
					subscriber.onError(e);
				}
			}
		});
	}

	class DockerRxAdapter extends EventsResultCallback{

		Subscriber<? super Event> subscriber;

		public DockerRxAdapter(Subscriber<? super Event> subscriber){
			this.subscriber = subscriber;
		}

		@Override
		public void onComplete() {
			super.onComplete();
			subscriber.onCompleted();
		}

		@Override
		public void onNext(Event item) {
			super.onNext(item);
			subscriber.onNext(item);
		}

		@Override
		public void onError(Throwable throwable) {
			super.onError(throwable);
			subscriber.onError(throwable);
		}

	}
}
