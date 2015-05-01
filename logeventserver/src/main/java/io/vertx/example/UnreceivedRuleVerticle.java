package io.vertx.example;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class UnreceivedRuleVerticle extends AbstractVerticle {
	@Override
	public void start() throws Exception {

		Scheduler scheduler = io.vertx.rxjava.core.RxHelper.scheduler(vertx);

		// Create a periodic event stream using Vertx scheduler
		Observable<Long> timer = Observable.
				timer(0, 1000, TimeUnit.MILLISECONDS, scheduler);

		Observable<JsonObject> jsonTimer = timer.map(x -> new JsonObject().put("timer", x));

		Observable<JsonObject> jsonReceived = vertx.eventBus().<JsonObject>consumer("rule.received")
				.toObservable()
				.map(x -> x.body());

		Observable<JsonObject> jsonSent = vertx.eventBus().<JsonObject>localConsumer("rule.sent")
				.toObservable()
				.map(x -> x.body());

		Observable<JsonObject> timerOrReceivedObs = Observable.merge(jsonTimer, jsonReceived);


		Observable<Tuple2<JsonObject, Observable<JsonObject>>> timerOrReceivedAfterSendObs = jsonSent.<JsonObject,Long,Long,Tuple2<JsonObject, Observable<JsonObject>>> groupJoin(timerOrReceivedObs,
				x -> Observable.timer(5000L, TimeUnit.MILLISECONDS, scheduler),
				y -> Observable.timer(5000L, TimeUnit.MILLISECONDS, scheduler), 
				(sent,timerOrReceived) -> {
										
					return new Tuple2<JsonObject, Observable<JsonObject>>(sent, timerOrReceived);
				});


		Observable<Object> lonelySendEvents = timerOrReceivedAfterSendObs.map(x -> {
			JsonObject sendEvent = x.getFirst();
			String messageId = sendEvent.getJsonArray("groups").getString(3);
						
			Observable<JsonObject> timerOrReceived = x.getSecond();

			return timerOrReceived.filter(tr -> { // filter out received messages for different messageIds.
				if ("received".equals(tr.getString("rule"))) {
					String receivedMessageId = tr.getJsonArray("groups").getString(3);
					if (receivedMessageId.equals(messageId)) {
						return true;
					}
					else {
						return false;
					}
				}
				return true;
			})
			.reduce((tr1, tr2) -> { // attempt to get the only 1 proper received message, failing that return 1 timer message
				if ("received".equals(tr1.getString("rule"))) {
					return tr1;
				}
				return tr2;
			})
			.filter(tr -> { // filter out proper received messages, keep only timer message indicating no proper reply received 
				if ("received".equals(tr.getString("rule"))) {
					// ignore, message received
					return false;
				}

				return true;
			})
			.map(tr -> { // format our sendEvent
				// this can only be a timer message, or nothing is emitted
				return sendEvent;
			});
		}).flatMap(lonelySendEventObs -> {
			return lonelySendEventObs;
		});

		lonelySendEvents.subscribe(x -> {
			JsonObject sendEvent = (JsonObject)x;

			System.out.println("XXX: " + sendEvent.encodePrettily());

			JsonObject unreceivedEvent = new JsonObject();
			unreceivedEvent.put("rule", "unreceived");
			unreceivedEvent.put("entry", sendEvent.getJsonObject("entry"));
			
			vertx.eventBus().publish("rule.unreceived", unreceivedEvent);
			vertx.eventBus().publish("rule", unreceivedEvent);
		}, x -> {
			x.printStackTrace();
		});
		
	}

	private static class Tuple2<X,Y> {
		private X first;
		private Y second;

		public Tuple2(X first, Y second) {
			this.first = first;
			this.second = second;
		}

		public X getFirst() {
			return first;
		}

		public Y getSecond() {
			return second;
		}
	}
}
