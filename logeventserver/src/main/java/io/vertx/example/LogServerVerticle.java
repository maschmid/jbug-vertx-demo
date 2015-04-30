package io.vertx.example;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;

/**
 * @author Marek Schmidt
 */
public class LogServerVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {
		
		//RuleSet ruleSet = new RuleSet();
		//ruleSet.addRule(new RegexRule("error", "ERROR ([^ ]*)"));
		//ruleSet.addRule(new RegexRule("warning", "WARN ([^ ]*)"));
		
		EventBus eb = vertx.eventBus();
		
		vertx.createHttpServer().requestHandler(req -> {

			if (req.method().equals(HttpMethod.POST)) {

				EntryParser parser = new EntryParser(
						entry -> {
							
							// System.out.println("XXX: " + entry.encodePrettily());
							
							eb.publish("logentry", entry);
						});

				req.handler(buffer -> parser.pushBuffer(buffer));

				req.endHandler(x -> {
					parser.endOfInput();
					req.response().end();
				});
			}
			else {
				req.response().end("Hi!");
			}
		}).listen(8088);
	}
}
