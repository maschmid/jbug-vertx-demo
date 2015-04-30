package io.vertx.example;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

/**
 * @author Marek Schmidt
 */
public class LogServerVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {

		EventBus eb = vertx.eventBus();

		vertx.createNetServer().connectHandler(socket -> {

			EntryParser parser = new EntryParser(
					entry -> {
						// System.out.println("XXX: " + entry.encodePrettily());
						eb.publish("logentry", entry);
					});

			socket.handler(buffer -> parser.pushBuffer(buffer));

			socket.endHandler(x -> {
				parser.endOfInput();
			});

		}).listen(8088);
	}
}
