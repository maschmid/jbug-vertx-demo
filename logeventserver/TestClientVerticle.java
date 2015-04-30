import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

/**
 * @author Marek Schmidt
 */
public class TestClientVerticle extends AbstractVerticle {

	Random random = new Random();

	private String generateEntry() {

		String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Date.from(Instant.now()));

		int r = random.nextInt();

		if (r % 10 == 0) {
			// every 10th is an error
			return "<entry><timestamp>" + timestamp + "</timestamp><message>ERROR: Some error message</message><level>ERROR</level></entry>";
		}
		else if (r % 10 == 1) {
			// every 10th is a warning
			return "<entry><timestamp>" + timestamp + "</timestamp><message>WARN: Some warning message</message><level>WARN</level></entry>";
		}
		else {
			return "<entry><timestamp>" + timestamp + "</timestamp><message>Some info message</message><level>INFO</level></entry>";
		}
	}

	@Override
	public void start() throws Exception {

		for (int i = 0; i < 256; ++i) {

			NetClient client = vertx.createNetClient();

			client.connect(8088, "localhost", res -> {
				if (res.succeeded()) {
					System.out.println("Connected!");
					NetSocket socket = res.result();

					socket.write("<entries>");
					vertx.setPeriodic(10, id -> {
						socket.write(generateEntry());
					});

				} else {
					System.out.println("Failed to connect: " + res.cause().getMessage());
				}
			});
		}
	}
}
