
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.handler.BodyHandler;
import io.vertx.ext.apex.handler.CookieHandler;
import io.vertx.ext.apex.handler.FormLoginHandler;
import io.vertx.ext.apex.handler.RedirectAuthHandler;
import io.vertx.ext.apex.handler.SessionHandler;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.sockjs.BridgeOptions;
import io.vertx.ext.apex.handler.sockjs.EventBusBridgeHook;
import io.vertx.ext.apex.handler.sockjs.PermittedOptions;
import io.vertx.ext.apex.handler.sockjs.SockJSHandler;
import io.vertx.ext.apex.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.apex.handler.sockjs.SockJSSocket;
import io.vertx.ext.apex.handler.sockjs.impl.SockJSHandlerImpl;
import io.vertx.ext.apex.sstore.LocalSessionStore;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.AuthService;
import io.vertx.ext.auth.shiro.ShiroAuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.auth.shiro.ShiroAuthService;


import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * A {@link io.vertx.core.Verticle} which implements a simple, realtime,
 * multiuser chat. Anyone can connect to the chat application on port
 * 8000 and type messages. The messages will be rebroadcast to all
 * connected users via the @{link EventBus} Websocket bridge.
 *
 * @author <a href="https://github.com/InfoSec812">Deven Phillips</a>
 */
public class Server extends AbstractVerticle {

  @Override
  public void start() throws Exception {

    Router router = Router.router(vertx);

    // Allow events for the designated addresses in/out of the event bus bridge
    BridgeOptions opts = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("chat.to.server"))
      .addOutboundPermitted(new PermittedOptions().setAddress("chat.to.client"));

    // Create the event bus bridge and add it to the router.
    // SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
    MySockJSHandlerImpl ebHandler = new MySockJSHandlerImpl(vertx, new SockJSHandlerOptions());

    ebHandler.setHook(new EventBusBridgeHook() {

        @Override
        public boolean handleSocketCreated(SockJSSocket sock) {
            return true;
        }

        @Override
        public void handleSocketClosed(SockJSSocket sock) {
        }

        @Override
        public boolean handleSendOrPub(SockJSSocket sock, boolean send,
                JsonObject msg, String address) {
            // System.out.println("XXX here, msg: " + msg.toString() + ", principal: " + sock.apexSession().getPrincipal().toString());

            JsonObject authMessage = new JsonObject();
            authMessage.put("principal", sock.apexSession().getPrincipal());
            authMessage.put("body", msg.getValue("body"));

            msg.put("body", authMessage);

            return true;
        }

        @Override
        public boolean handlePreRegister(SockJSSocket sock, String address) {
            return true;
        }

        @Override
        public void handlePostRegister(SockJSSocket sock, String address) {
        }

        @Override
        public boolean handleUnregister(SockJSSocket sock, String address) {
            return true;
        }

        @Override
        public boolean handleAuthorise(JsonObject message, String sessionID,
                Handler<AsyncResult<Boolean>> handler) {
            return true;
        }
    });


    ebHandler.bridge(opts);

    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(BodyHandler.create());

    router.route("/eventbus/*").handler(ebHandler);

        AuthProvider authProvider = ShiroAuthProvider.create(vertx, ShiroAuthRealmType.PROPERTIES, new JsonObject());
    router.route("/private/*").handler(RedirectAuthHandler.create(authProvider, "/loginpage.html"));

    // Serve the static private pages from directory 'private'
    router.route("/private/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("private"));
    
    // Handles the actual login
    router.route("/loginhandler").handler(FormLoginHandler.create(authProvider));
    // Implement logout
    
    router.route("/logout").handler(context -> {
        context.session().logout();
       // Redirect back to the index page
        context.response().putHeader("location", "/").setStatusCode(302).end();
    });

    // Create a router endpoint for the static content.
    router.route().handler(StaticHandler.create());


    // Start the web server and tell it to use the router to handle requests.
    vertx.createHttpServer().requestHandler(router::accept).listen(8080);

    EventBus eb = vertx.eventBus();

    // Register to listen for messages coming IN to the server
    eb.<JsonObject>consumer("chat.to.server").handler(authMessage -> {

      //message.headers().entries().forEach(entry -> {
      //  System.out.println("header " + entry.getKey() + ": " + entry.getValue());
      //});
      
      JsonObject body = authMessage.body().getJsonObject("body");
      body.put("user", authMessage.body().getJsonObject("principal").getString("username"));

      // System.out.println("XXX " + message.body());

      JsonObject entry = new JsonObject();
      entry.put("user", body.getString("user"));
      entry.put("text", body.getString("text"));
      entry.put("_id", body.getString("id"));
      
      eb.send("store", entry, res -> {
        if (res.succeeded()) {
            authMessage.reply(res.result().body());
            eb.publish("chat.to.client", res.result().body());
        }
        else {
            authMessage.fail(2, res.cause().getMessage());
        }
      });

      // Create a timestamp string
      //String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
      // Send the message back out to all clients with the timestamp prepended.
      //eb.publish("chat.to.client", timestamp + ": " + message.body());
    });

  }
}
