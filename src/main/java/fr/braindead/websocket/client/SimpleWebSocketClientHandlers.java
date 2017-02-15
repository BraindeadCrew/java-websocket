package fr.braindead.websocket.client;

/**
 *
 * Created by leiko on 2/14/17.
 */
public interface SimpleWebSocketClientHandlers extends WebSocketClientHandlers {

    @Override
    default void onOpen() {}

    @Override
    default void onMessage(String msg) {}

    @Override
    default void onClose(int code, String reason, boolean remote) {}

    @Override
    default void onError(Throwable e) {}
}
