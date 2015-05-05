package io.vertx.example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;

public class Main {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
				new DropwizardMetricsOptions().setEnabled(true).setJmxEnabled(true)
				.addMonitoredEventBusHandler(
					new Match().setValue("queue"))
				));
		
		vertx.deployVerticle("io.vertx.example.SenderVerticle");
		DeploymentOptions options = new DeploymentOptions().setWorker(true);
		vertx.deployVerticle("io.vertx.example.WorkerVerticle", options);
		
		vertx.deployVerticle("io.vertx.example.MetricsVerticle");
	}

}
