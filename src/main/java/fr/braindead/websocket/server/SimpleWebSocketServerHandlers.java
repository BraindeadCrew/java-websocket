package fr.braindead.websocket.server;

import fr.braindead.websocket.WebSocket;

/**
 *
 * Created by leiko on 2/14/17.
 */
public interface SimpleWebSocketServerHandlers extends WebSocketServerHandlers {

    @Override
    default void onOpen(WebSocket client) {}

    @Override
    default void onMessage(WebSocket client, String msg) {}

    @Override
    default void onClose(WebSocket client, int code, String reason, boolean remote) {}

    @Override
    default void onError(WebSocket client, Throwable e) {}
}
