package io.vertx.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Scheduler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class RuleCountAggregatorVerticle extends AbstractVerticle {

	boolean hasAggregatorLock = false;
	boolean lockRequested = false;

	private Collection<String> rules = new LinkedList<String>();

	@Override
	public void start() throws Exception {
		
		Observable<Long> timer = vertx.periodicStream(1000).toObservable();

		vertx.eventBus().<JsonObject>consumer("registry.ruleCreated").handler(msg -> {
			rules.add(msg.body().getString("id"));
		});

		// periodically attempt to get the aggregator lock
		vertx.setPeriodic(1000L, n -> {
						
			// Don't make more than one lock requests at once, as that just clogs the eventbus.. 
			if (!hasAggregatorLock && !lockRequested) {
				lockRequested = true;
				vertx.sharedData().getLock("rulecountaggregator", lockAcquired -> {
					
					lockRequested = false;
					
					if (lockAcquired.succeeded()) {
						hasAggregatorLock = true;
					}
					else {
					}
				});
			}
		});

		Observable<JsonObject> jsonRule = vertx.eventBus().<JsonObject>consumer("rule")
				.bodyStream().toObservable();

		Observable<List<JsonObject>> bufferObs = jsonRule.buffer(timer);
		
		Observable<Map<String, Long>> mapObs = bufferObs.map(samples -> 
						samples.
						stream().
						collect(Collectors.groupingBy(ruleEvent -> ruleEvent.getString("rule"),
													  Collectors.counting())));
		
		mapObs.subscribe(map -> {
			
			System.out.println("XXX subscribe thread: " + Thread.currentThread().getName() + " hasLock: " + hasAggregatorLock);
			
			if (hasAggregatorLock) {
				
				JsonObject stats = new JsonObject();
				rules.forEach(ruleid -> {
					stats.put(ruleid, 0L);
				});

				map.entrySet().forEach(entry -> {
					stats.put(entry.getKey(), entry.getValue());
				});

				System.out.println("publishing aggregatedcounts!: " + stats.encode());
				vertx.eventBus().publish("aggregatedcounts", stats);
			}
		}, error -> {
			error.printStackTrace();
		});

		/*
		Observable<Observable<JsonObject>> windowObs = jsonRule.window(timer);

		Observable<ConcurrentHashMap<String, LongAdder>> reducedObs = windowObs.flatMap(obs -> 
				obs.reduce(new ConcurrentHashMap<String, LongAdder>(), 
						(map, json) -> {
							map.computeIfAbsent(json.getString("rule"), x -> new LongAdder()).increment();
							return map;
						}));

		reducedObs.subscribe(map -> {
			JsonObject stats = new JsonObject();
			rules.forEach(ruleid -> {
				stats.put(ruleid, 0L);
			});

			map.entrySet().forEach(entry -> {
				stats.put(entry.getKey(), entry.getValue());
			});

			vertx.eventBus().publish("aggregatedcounts", stats);
		});*/
	}
}
