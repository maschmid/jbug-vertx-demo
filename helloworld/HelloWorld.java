import io.vertx.core.AbstractVerticle;

public class HelloWorld extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/html")
        .end("<html><body><h1>Hello from vert.x!" +
          "</h1></body></html>");
    }).listen(8080);
  }
}
