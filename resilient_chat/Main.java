
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.sockjs.BridgeOptions;
import io.vertx.ext.apex.handler.sockjs.PermittedOptions;
import io.vertx.ext.apex.handler.sockjs.SockJSHandler;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

public class Main extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.deployVerticle("Mongo.java", mongoRes -> {
        if (mongoRes.succeeded()) {
          vertx.deployVerticle("Server.java", serverRes -> {
            if (serverRes.succeeded()) {
                System.out.println("App deployed!");
            }
            else {
                System.out.println("Error starting Server " + serverRes.cause());
            }
          });
        }
        else {
            System.out.println("Error starting Mongo " + mongoRes.cause());
        }
    });
  }
}
