package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.BodyHandler;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.sockjs.BridgeOptions;
import io.vertx.ext.apex.handler.sockjs.PermittedOptions;
import io.vertx.ext.apex.handler.sockjs.SockJSHandler;

public class UiServerVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {

		String bindAddress = System.getenv("BIND_ADDRESS");

		Router router = Router.router(vertx);

		BridgeOptions opts = new BridgeOptions()
		//.addInboundPermitted(new PermittedOptions().setAddress("chat.to.server"))
		.addOutboundPermitted(new PermittedOptions().setAddressRegex("rule.*"))
		.addOutboundPermitted(new PermittedOptions().setAddress("aggregatedcounts"));

		// Buffer requests to easily manipulate with REST requests
		router.route().handler(BodyHandler.create());

		router.get("/rules").handler(this::ruleListHandler);
		router.post("/rules").handler(this::rulePostHandler);

		// Create the event bus bridge and add it to the router.
		SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
		router.route("/eventbus/*").handler(ebHandler);

		// Create a router endpoint for the static content.
		router.route().handler(StaticHandler.create());

		// Start the web server and tell it to use the router to handle requests.
		vertx.createHttpServer().requestHandler(router::accept).listen(8080, bindAddress);
	}

	private void ruleListHandler(RoutingContext ctx) {
		vertx.eventBus().<JsonArray>send("registry.getRules", null, reply -> {
			if (reply.succeeded()) {

				// System.out.println("XXX listing rules: " + reply.result().body().encode());

				ctx.response().putHeader("content-type", "application/json")
				.end(reply.result().body().encode());
			}
			else {
				ctx.response().setStatusCode(500).end("Error getting rules from registry");
			}
		});
/*
		registryService.getRules(reply -> {
			if (reply.succeeded()) {
				ctx.response().putHeader("content-type", "application/json")
				.end(reply.result().encode());
			}
			else {
				ctx.response().setStatusCode(500).end("Error getting rules from registry");
			}
		});*/
	}

	private void rulePostHandler(RoutingContext ctx) {
		JsonObject rule = ctx.getBodyAsJson();

		//System.out.println("registering rule: " + rule.encodePrettily());

		
		vertx.eventBus().<JsonObject>send("registry.createRule", rule, reply -> {
			if (reply.succeeded()) {
				ctx.response().putHeader("content-type", "application/json").end("{}");
			}
			else {
				System.out.println("" + reply.cause().getMessage());
				ctx.response().setStatusCode(500).end("Error putting rule to the registry");
			}
		});
	}
}
