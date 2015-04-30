import io.vertx.core.AbstractVerticle;

import java.math.BigDecimal;
import java.util.UUID;


public class Server extends AbstractVerticle {

  public static String serverId = UUID.randomUUID().toString();

  static BigDecimal fac(BigDecimal n, BigDecimal acc) {
    if (n.equals(BigDecimal.ONE)) {
      return acc;
    }

    BigDecimal lessOne = n.subtract(BigDecimal.ONE);
    return fac(lessOne, acc.multiply(lessOne));
  } 

  @Override
  public void start() throws Exception {

    System.out.println("starting verticle in thread: " + Thread.currentThread());

    vertx.createHttpServer().requestHandler(req -> {

      String query = req.getParam("n");

      if (query == null) {
        req.response()
          .putHeader("content-type", "text/html")
          .end("<html><body><h1>Hello from " + serverId +
          "</h1></body></html>");
      }
      else {
        BigDecimal bd = BigDecimal.valueOf(Integer.parseInt(query));

        BigDecimal ret = fac(bd, bd);

        req.response()
          .putHeader("content-type", "text/html")
          .end("<html><body><h1>Hello from " + serverId + "</h1><p>" + ret + "</p></body></html>");
      }
      
    }).listen(8080);
  }
}

