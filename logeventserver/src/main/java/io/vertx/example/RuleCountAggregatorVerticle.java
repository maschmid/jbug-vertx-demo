package io.vertx.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import rx.Observable;
import rx.Scheduler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class RuleCountAggregatorVerticle extends AbstractVerticle {
	
	
	private Collection<String> rules = new LinkedList<String>();
	
	@Override
	public void start() throws Exception {

		Scheduler scheduler = io.vertx.rxjava.core.RxHelper.scheduler(vertx);

		// Create a periodic event stream using Vertx scheduler
		Observable<Long> timer = Observable.
				timer(0, 1000, TimeUnit.MILLISECONDS, scheduler);
		
		vertx.eventBus().<JsonObject>consumer("registry.ruleCreated").handler(msg -> {
			rules.add(msg.body().getString("id"));
		});
		
		Observable<JsonObject> jsonRule = vertx.eventBus().<JsonObject>consumer("rule")
				.toObservable()
				.map(x -> x.body());
				
		jsonRule.window(timer)
			.map(obs -> {
				return obs.reduce(new ConcurrentHashMap<String, LongAdder>(), 
						(map, json) -> {
							map.computeIfAbsent(json.getString("rule"), x -> new LongAdder()).increment();
							return map;
						});
			})
			.flatMap(x -> x)
			.subscribe(map -> {
				JsonObject stats = new JsonObject();
				rules.forEach(ruleid -> {
					stats.put(ruleid, 0L);
				});
				
				map.entrySet().forEach(entry -> {
					stats.put(entry.getKey(), entry.getValue());
				});
								
				vertx.eventBus().publish("aggregatedcounts", stats);
			}); 
	}
}
