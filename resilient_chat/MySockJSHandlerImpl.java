import io.vertx.core.Vertx;
import io.vertx.ext.apex.handler.sockjs.BridgeOptions;
import io.vertx.ext.apex.handler.sockjs.EventBusBridgeHook;
import io.vertx.ext.apex.handler.sockjs.SockJSHandler;
import io.vertx.ext.apex.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.apex.handler.sockjs.impl.EventBusBridgeImpl;
import io.vertx.ext.apex.handler.sockjs.impl.SockJSHandlerImpl;

public class MySockJSHandlerImpl extends SockJSHandlerImpl {

	private EventBusBridgeHook hook;
	private Vertx vertx;
	
	public MySockJSHandlerImpl(Vertx vertx, SockJSHandlerOptions options) {
		super(vertx, options);
		this.vertx = vertx;
	}
	
	@Override
	public SockJSHandlerImpl setHook(EventBusBridgeHook hook) {
		super.setHook(hook);
		this.hook = hook;
		return this;
	}

	@Override
	public SockJSHandler bridge(BridgeOptions bridgeOptions) {
		EventBusBridgeImpl busBridge = new EventBusBridgeImpl(vertx, bridgeOptions);
		if (hook != null) {
			busBridge.setHook(hook);
		}
	    socketHandler(busBridge);
	    return this;
	}
}
