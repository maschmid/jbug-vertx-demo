import io.vertx.core.AbstractVerticle;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.Route;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.core.http.HttpServerResponse;

public class ApexDemo extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    Route route1 = router.route("/some/path/").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.setChunked(true);
        response.write("route1\n");
        routingContext.vertx().setTimer(5000, tid -> routingContext.next());
    });

    Route route2 = router.route("/some/path/").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.write("route2\n");
        response.end();
    });

    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
  }
}
