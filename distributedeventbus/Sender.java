import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import java.util.concurrent.atomic.AtomicInteger;

public class Sender extends AbstractVerticle {

  private int i = 0;

  @Override
  public void start() throws Exception {
    EventBus eb = vertx.eventBus();
    vertx.setPeriodic(1000, id -> {
      final int seq = i++;
      eb.send("some.address", "Hello #" + seq, res -> {
        if (res.succeeded()) {
            System.out.println("Message #" + seq + " response: " + res.result().body());
        }
        else {
            if (res.cause() instanceof ReplyException) {
                ReplyFailure failureType = ((ReplyException)res.cause()).failureType();
                if (ReplyFailure.NO_HANDLERS == failureType) {
                    System.out.println("Message #" + seq + " failed because there are no handlers!");
                }
                else if (ReplyFailure.RECIPIENT_FAILURE == failureType) {
                    System.out.println("Message #" + seq + " failed because of recipient failure!");
                }
                else if (ReplyFailure.TIMEOUT == failureType) {
                    System.out.println("Message #" + seq + " failed because of timeout!");
                }
            }
            else {
                System.out.println("Message #" + seq + " response not received!!! Casue: " + res.cause());
            }
        }
      });
    });
  }
}
