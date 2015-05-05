package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class WorkerVerticle extends AbstractVerticle {
	public void start() {
		EventBus eb = vertx.eventBus();
		
		eb.<Long>consumer("queue").handler(msg -> {
			// System.out.println("Worker: " + msg.body());
			
			try {
				Thread.sleep(1000);
				
				msg.reply(msg.body());
			}
			catch(Exception x) {
				
			}
		});
	}
}
