package fr.braindead.websocket.server;

import fr.braindead.websocket.WebSocket;

import java.util.Set;

/**
 *
 * Created by leiko on 18/06/15.
 */
public interface WebSocketServer extends WebSocketServerHandlers {

    void start();

    void stop();

    Set<WebSocket> getClients();
}
