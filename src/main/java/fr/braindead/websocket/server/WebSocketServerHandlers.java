package fr.braindead.websocket.server;

import fr.braindead.websocket.WebSocket;

/**
 *
 * Created by leiko on 2/14/17.
 */
public interface WebSocketServerHandlers {

    void onOpen(WebSocket client);

    void onMessage(WebSocket client, String message);

    void onClose(WebSocket client, int code, String reason, boolean remote);

    void onError(WebSocket client, Throwable ex);
}
