import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

public class Observer extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    EventBus eb = vertx.eventBus();
    eb.consumer("rule.error", message -> {
        System.out.println(message.body());
    });
  }
}
