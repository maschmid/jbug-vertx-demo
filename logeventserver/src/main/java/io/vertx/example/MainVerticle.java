package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

	
	private void deployRegexVerticle(String id, String field, String regex) {
		JsonObject config = new JsonObject()
		.put("id", id)
		.put("field", field)
		.put("regex", regex);
	
		DeploymentOptions options = new DeploymentOptions().setConfig(config);
		vertx.deployVerticle("io.vertx.example.RegexRuleVerticle", options);
	}
	
	@Override
	public void start() throws Exception {
		
		//deployRegexVerticle("error", "message", "ERROR ([^ ]*)");
		//deployRegexVerticle("warning", "message", "WARN");
		//deployRegexVerticle("received", "message", "([^ ]*) receive from ([^ ]*) ([^ ]*)");
		//deployRegexVerticle("sent", "message", "([^ ]*) send to ([^ ]*) ([^ ]*)");
		
		//vertx.deployVerticle("io.vertx.example.UnreceivedRuleVerticle");
		
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
