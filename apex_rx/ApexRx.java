import rx.Observable;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.apex.Router;
import io.vertx.rxjava.ext.apex.RoutingContext;
import io.vertx.rxjava.ext.apex.handler.BodyHandler;

public class ApexRx extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        
        router.route().handler(BodyHandler.create());
        
        // simulates some async service
        vertx.eventBus().<String>consumer("foo").handler(msg -> msg.reply("re: " + msg.body()));
        
        ObservableHandler<RoutingContext> ctxObs = RxHelper.observableHandler();
        router.post("/foo").handler(ctxObs.toHandler());
        
        Observable<RoutingContextAndReply<String>> ctxAndReplyObs = ctxObs.flatMap(routingContext -> 
            Observable.zip(
                Observable.just(routingContext),
                vertx.eventBus().<String>sendObservable("foo", routingContext.getBodyAsString()),
                (ctx, reply) -> new RoutingContextAndReply<String>(ctx, reply.body())));
            
        ctxAndReplyObs.subscribe(
            ctxAndReply -> ctxAndReply.getRoutingContext().response().setChunked(true).write(ctxAndReply.getReply()).end(),
            error -> error.printStackTrace());
    
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }
    
    private static class RoutingContextAndReply<T> {
        private RoutingContext routingContext;
        private T reply;
        
        public RoutingContextAndReply(RoutingContext routingContext, T reply) {
            this.routingContext = routingContext;
            this.reply = reply;
        }
        
        public RoutingContext getRoutingContext() {
            return routingContext;
        }
        public T getReply() {
            return reply;
        }
    }
}
