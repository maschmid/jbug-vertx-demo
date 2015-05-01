package io.vertx.example;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;

public class RuleRegistryVerticle extends AbstractVerticle {
	
	private Map<String, String> ruleId2VerticleId = new HashMap<String, String> ();
	private JsonArray ruleCache = new JsonArray();

	@Override
	public void start() throws Exception {
		EventBus eb = vertx.eventBus();

		eb.<JsonObject>localConsumer("registry.createRule").handler(this::createRuleHandler);
		eb.<JsonObject>localConsumer("registry.deleteRule").handler(this::deleteRuleHandler);

		eb.<JsonObject>consumer("registry.ruleCreated").handler(this::deployRule);
		eb.<String>consumer("registry.ruleDeleted").handler(this::undeployRule);
		
		eb.<Void>localConsumer("registry.getRules").handler(this::getRules);
	}
	
	private void getRules(Message<Void> msg) {
		msg.reply(ruleCache);
	}
	
	private void deployRule(Message<JsonObject> msg) {
		
		JsonObject rule = msg.body();
		
		ruleCache.add(rule);

		String ruleId = rule.getString("id");
		String verticleClassName = null;
		switch(rule.getString("type")) {
		case "regex":
			verticleClassName = "io.vertx.example.RegexRuleVerticle";
			break;
		case "unreceived":
			verticleClassName = "io.vertx.example.UnreceivedRuleVerticle";
			break;
		}
		
		DeploymentOptions options = new DeploymentOptions().setConfig(rule);
		vertx.deployVerticle(verticleClassName, options, (AsyncResult<String> asyncCompletion) -> {
			if (asyncCompletion.succeeded()) {
				String deploymentId = asyncCompletion.result();
				ruleId2VerticleId.put(ruleId, deploymentId);
			}
		});
	}
	
	private void undeployRule(Message<String> msg) {
		String deploymentId = ruleId2VerticleId.get(msg.body());
		if (deploymentId != null) {
			vertx.undeploy(deploymentId);
			ruleId2VerticleId.remove(msg.body());
		}
	}
	
	private void createRuleHandler(Message<JsonObject> msg) {
		SharedData sd = vertx.sharedData();
		EventBus eb = vertx.eventBus();
		
		JsonObject obj = msg.body();
		String ruleId = obj.getString("id");
		
		System.out.println("XXX Create rule handler");
		
		sd.<String, String>getClusterWideMap("rules", res -> {
			if (res.succeeded()) {
				System.out.println("XXX getClusterWideMap succeeded");
				AsyncMap<String, String> map = res.result();
				
				map.get("rules", asyncGetRuleRes -> {
					 if (asyncGetRuleRes.succeeded()) {
						 System.out.println("XXX asyncGetRuleRes succeeded");
						 String rulesString = asyncGetRuleRes.result();
						 
						 JsonArray array = null;
						 if (rulesString == null) {
							 array = new JsonArray();
						 }
						 else {
							 array = new JsonArray(rulesString);
						 }
						 for (int i = 0; i < array.size(); i++) {
							 JsonObject iRule = array.getJsonObject(i);
							 if (ruleId.equals(iRule.getString("id"))) {
								 msg.fail(2, "Rule with such id already exists.");
								 break;
							 }
						 }
						 
						 array.add(obj);
						 
						 // TODO: store in a safe way
						 map.put("rules", array.encode(), completed -> {
							 if (completed.succeeded()) {
								 eb.publish("registry.ruleCreated", obj);
								 msg.reply(obj);
							 }
							 else {
								 msg.fail(3, "Fail to store new rules to the cluster map");
							 }
						 });
					 }
					 else {
						 System.out.println("XXX asyncGetRuleRes failed");
						 msg.fail(4, "Failed to get rule from AsyncMap");
					 }
				});
			} else {
				System.out.println("XXX getClusterWideMap failed");
				msg.fail(1, "Failed to get the cluster map");
			}
		});
	}

	private void deleteRuleHandler(Message<JsonObject> msg) {
//TBD
	}
}
