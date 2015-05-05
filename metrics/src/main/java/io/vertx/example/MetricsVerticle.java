package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;

public class MetricsVerticle extends AbstractVerticle {
	public void start() {
		MetricsService metricsService = MetricsService.create(vertx);
		
		vertx.setPeriodic(1000, tid -> {
			JsonObject metrics = metricsService.getMetricsSnapshot(vertx);
			JsonObject pendingCounter = metrics.getJsonObject("vertx.eventbus.messages.pending");
			System.out.println("vertx.eventbus.messages.pending metric: " + pendingCounter.getLong("count"));
		});
	}
}
