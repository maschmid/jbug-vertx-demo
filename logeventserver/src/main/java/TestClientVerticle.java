
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

/**
 * @author Marek Schmidt
 */
public class TestClientVerticle extends AbstractVerticle {

	Random random = new Random();

	NetSocket socket1 = null;
	NetSocket socket2 = null;

	private void generateEntry() {

		String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Date.from(Instant.now()));

		int r = random.nextInt();

		if (r % 10 == 0) {
			// every 10th is an error for server1
			// return "<entry><timestamp>" + timestamp + "</timestamp><message>ERROR: Some error message</message><level>ERROR</level></entry>";
			socket1.write("<entry><timestamp>" + timestamp + "</timestamp><message>ERROR: Some error message</message><level>ERROR</level></entry>");
		}
		else if (r % 10 == 2) {
			// every 10th is a warning for server2
			socket2.write("<entry><timestamp>" + timestamp + "</timestamp><message>WARN: Some warning message</message><level>WARN</level></entry>");
			// return "<entry><timestamp>" + timestamp + "</timestamp><message>WARN: Some warning message</message><level>WARN</level></entry>";
		}
		else if (r % 10 == 3) {
			// message send
			
			String messageId = UUID.randomUUID().toString();
			
			if (r % 100 == 3) {
				// 
				socket1.write("<entry><timestamp>" + timestamp + "</timestamp><message>Server1 send to Server2 " + messageId + "</message><level>INFO</level></entry>");
			}
			else {
				socket1.write("<entry><timestamp>" + timestamp + "</timestamp><message>Server1 send to Server2 " + messageId + "</message><level>INFO</level></entry>");
				vertx.setTimer(2000L, x -> socket2.write("<entry><timestamp>" + timestamp + "</timestamp><message>Server2 receive from Server1 " + messageId + "</message><level>INFO</level></entry>"));
			}
		}
		else {
			socket1.write("<entry><timestamp>" + timestamp + "</timestamp><message>Some info message</message><level>INFO</level></entry>");
			socket2.write("<entry><timestamp>" + timestamp + "</timestamp><message>Some info message</message><level>INFO</level></entry>");
		}
	}

	@Override
	public void start() throws Exception {

		vertx.setPeriodic(5, id -> {
			generateEntry();
		});

		NetClient client = vertx.createNetClient();

		client.connect(8088, "127.0.1.1", res -> {
			if (res.succeeded()) {
				socket1 = res.result();
				socket1.write("<entries>");
			} else {
				System.out.println("Failed to connect to 127.0.1.1: " + res.cause().getMessage());
			}
		});
		
		client.connect(8088, "127.0.2.1", res -> {
			if (res.succeeded()) {
				socket2 = res.result();
				socket2.write("<entries>");
			} else {
				System.out.println("Failed to connect to 127.0.2.1: " + res.cause().getMessage());
			}
		});
	}
}
