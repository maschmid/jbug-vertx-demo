import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class ExceptionReply extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    EventBus eb = vertx.eventBus();
    eb.consumer("some.address", message -> {
        System.out.println(message.body());
        throw new RuntimeException("Boo!");
    });
  }
}
