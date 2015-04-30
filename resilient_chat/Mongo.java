
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoService;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

import java.util.function.BiConsumer;

public class Mongo extends AbstractVerticle {

  private MongoService proxy = null; 
  private void storeChatMessage(JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    //JsonObject document = new JsonObject().put("user", "foo").put("text", "foo bar har!").put("_id", "123"); 
    final String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(Instant.now()));
    final JsonObject sanitizedDocument = new JsonObject()
        .put("user", document.getString("user"))
        .put("text", document.getString("text"))
        .put("time", timestamp)
        .put("_id", document.getString("_id"));

    proxy.insert("chat", sanitizedDocument, res -> {
        if (res.succeeded()) {
//            System.out.println("Document stored as " + res.result());
            handler.handle(Future.succeededFuture(sanitizedDocument));
        }
        else if (res.cause().getMessage().contains("E11000 duplicate key error")) {
//            System.out.println("Save failed cause: " + res.cause());
            handler.handle(Future.succeededFuture(sanitizedDocument));
        }
        else {
//            System.out.println("Save failed cause: " + res.cause());
            handler.handle(Future.failedFuture(res.cause()));
        }
/*
        JsonObject query = new JsonObject();
        proxy.find("chat", query, qres -> {
            if (qres.succeeded()) {
                for (JsonObject json : qres.result()) {
                  System.out.println(json.encodePrettily());
                }
            }
        }); */
    });
  }

  private void storeChatMessageHandler(Message<JsonObject> message) {
    storeChatMessage(message.body(), res -> {
        if (res.succeeded()) {
            message.reply(res.result());
        }
        else {
            message.fail(1, "MongoDB failed to store the chat entry.");
        }
    });
  }

  private void listChatMessages(Handler<AsyncResult<JsonArray>> handler) {
    proxy.find("chat", new JsonObject(), res -> {
        if (res.succeeded()) {
            JsonArray array = new JsonArray();
            res.result().stream().forEach(x -> array.add(x));

            handler.handle(Future.succeededFuture(array));
        }
    });
  }

  private void listChatMessagesHandler(Message<Void> message) {
    listChatMessages(res -> {
        if (res.succeeded()) {
            JsonObject reply = new JsonObject();
            reply.put("messages", res.result());
            message.reply(reply);
        }
    });
  }

  @Override
  public void start() throws Exception {

    JsonObject config = new JsonObject();
    config.put("db_name", "vertxchat");
    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx.deployVerticle("service:io.vertx.mongo-service", options, res -> {
        if (res.succeeded()) {
            System.out.println("Mongo started");
            proxy = MongoService.createEventBusProxy(vertx, "vertx.mongo");

        } else {
            System.out.println("Error deploying mongo service!!");
        }
    });

    
    EventBus eb = vertx.eventBus();

/*    eb.<JsonObject>consumer("store").handler(storeChatMessageHandler((message, res) -> {
        if (res.succeeded()) {
            message.reply(res.result());
            System.out.println("message stored!");
        }
        else {
            System.out.println("Message not stored! " + res.cause());
        }
    }));*/

    eb.<JsonObject>consumer("store").handler(this::storeChatMessageHandler);

/*    eb.<JsonObject>consumer("store").handler(message -> {
        storeChatMessage(message.body(), res -> {
            if (res.succeeded()) {
                message.reply(res.result());
                System.out.println("message stored!");
            }
            else {
                System.out.println("Message not stored! " + res.cause());
            }
        });
//        System.out.println("storing ... ");
    });*/

    eb.<Void>consumer("list").handler(this::listChatMessagesHandler);
/*    eb.consumer("list").handler(message -> {
        listChatMessages(res -> {
            if (res.succeeded()) {
                JsonObject reply = new JsonObject();
                reply.put("messages", res.result());
                message.reply(reply);
            }
        });
    });*/
    // vertx.setPeriodic(1000, id -> eb.send("store", "something"));
  }
}
