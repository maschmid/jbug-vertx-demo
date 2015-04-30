import io.vertx.core.AbstractVerticle;

import java.math.BigDecimal;
import java.util.UUID;


public class Server extends AbstractVerticle {

  public static String serverId = UUID.randomUUID().toString();

  private LRUCache<Integer, BigDecimal> lruCache = new LRUCache<Integer, BigDecimal>(256);

  static BigDecimal fac(BigDecimal n, BigDecimal acc) {
    if (n.equals(BigDecimal.ONE)) {
      return acc;
    }

    BigDecimal lessOne = n.subtract(BigDecimal.ONE);
    return fac(lessOne, acc.multiply(lessOne));
  }

  private BigDecimal cachedFac(int n) {
    if (lruCache.containsKey(n)) {
        return lruCache.get(n);
    }

    BigDecimal bdn = BigDecimal.valueOf(n);

    BigDecimal ret = fac(bdn, bdn);
    lruCache.put(n, ret);

    return ret;
  }

  @Override
  public void start() throws Exception {
    vertx.createHttpServer().requestHandler(req -> {

      String query = req.getParam("n");

      if (query == null) {
        req.response()
          .putHeader("content-type", "text/html")
          .end("<html><body><h1>Hello from " + serverId +
          "</h1></body></html>");
      }
      else {
        int i = Integer.parseInt(query);

        BigDecimal ret = cachedFac(i);

        req.response()
          .putHeader("content-type", "text/html")
          .end("<html><body><h1>Hello from " + serverId + "</h1><p>" + ret + "</p></body></html>");
      }
      
    }).listen(8080);
  }
}

