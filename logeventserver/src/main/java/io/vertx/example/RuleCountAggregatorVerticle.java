package io.vertx.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import rx.Observable;
import rx.Scheduler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
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
		
		/*
		Observable<JsonObject> jsonUnreceived = vertx.eventBus().<JsonObject>consumer("rule.unreceived")
				.toObservable()
				.map(x -> x.body());
		
		Observable<JsonObject> jsonError = vertx.eventBus().<JsonObject>consumer("rule.error")
				.toObservable()
				.map(x -> x.body());
		
		Observable<JsonObject> jsonWarning = vertx.eventBus().<JsonObject>consumer("rule.warning")
				.toObservable()
				.map(x -> x.body());*/

		
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
	
	/*
	private void reCreateObservables() {
		if (consumers != null) {
			stopConsumers();
		}
		
		consumers = new LinkedList<MessageConsumer<JsonObject>> ();
		
		rules.forEach(ruleid -> {
			MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer("rule." + ruleid);
			consumers.add(consumer);
		});
		
		Stream<Observable<JsonObject>> stream = consumers.stream().<Observable<JsonObject>>map(consumer -> consumer.toObservable().map(x -> x.body()));
		Observable<JsonObject> mergedObs = Observable.<JsonObject>merge((Iterable<Observable<JsonObject>>)stream::iterator);
		
		mergedObs.window(timer)
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
			
			//stats.put("unreceived", 0L);
			//stats.put("error", 0L);
			//stats.put("warning", 0L);
			
			map.entrySet().forEach(entry -> {
				stats.put(entry.getKey(), entry.getValue());
			});
			
			System.out.println("XXX: " + stats.encodePrettily());
			
			vertx.eventBus().publish("aggregatedcounts", stats);
		});
	}

	private void stopConsumers() {
		
		consumers.forEach(consumer -> {
			consumer.unregister();
		});
		
		consumers = null;
	}*/
}
