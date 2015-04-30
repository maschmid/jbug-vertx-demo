package io.vertx.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RegexRuleVerticle extends AbstractVerticle {
	@Override
	public void start() throws Exception {
		String id = context.config().getString("id");
		String field = context.config().getString("field");
		String regex = context.config().getString("regex");
		
		Pattern pattern = Pattern.compile(regex);
		
		vertx.eventBus().<JsonObject>localConsumer("logentry").handler(msg -> {
			JsonObject entry = msg.body();
			Matcher m = pattern.matcher(entry.getString(field));
			
			if (m.find()) {
				JsonObject match = new JsonObject();
				match.put("rule", id);
				
				JsonArray groups = new JsonArray();
				for (int i = 0; i <= m.groupCount(); ++i) {
					groups.add(m.group(i));
				}
				
				match.put("groups", groups);
				match.put("entry", entry);
				
				vertx.eventBus().publish("rule." + id, match);
				vertx.eventBus().publish("rule", match);
			}
		});
	}
}
