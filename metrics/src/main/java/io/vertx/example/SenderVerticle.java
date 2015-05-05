package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class SenderVerticle extends AbstractVerticle {
	
	long count = 0;
	
	public void start() {
		EventBus eb = vertx.eventBus();
		
		vertx.setPeriodic(50, tid -> {
			eb.send("queue", count++, replyHandler -> {
				if (replyHandler.succeeded()) {
					// System.out.println("reply: " + replyHandler.result().body());
				}
				else {
					System.out.println("reply error!");
				}
			});
		});
	}
}
