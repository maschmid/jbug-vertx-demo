package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
	
	@Override
	public void start() throws Exception {
		
		vertx.deployVerticle("io.vertx.example.LogServerVerticle");
		
		vertx.deployVerticle("io.vertx.example.UiServerVerticle");
		
		
		
		vertx.deployVerticle("io.vertx.example.RuleCountAggregatorVerticle");
		
		//RuleRegistryService.create(vertx);
		vertx.deployVerticle("io.vertx.example.RuleRegistryVerticle");
		
		vertx.eventBus().<JsonObject>consumer("aggregatedcounts").handler(msg -> {
			System.out.println("XXX received: " + msg.body().encode());
		});
	}
}
