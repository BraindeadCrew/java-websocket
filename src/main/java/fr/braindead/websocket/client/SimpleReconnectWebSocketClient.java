package fr.braindead.websocket.client;

import java.net.URI;

/**
 *
 * Created by leiko on 2/14/17.
 */
public class SimpleReconnectWebSocketClient extends ReconnectWebSocketClient implements SimpleWebSocketClientHandlers {

    public SimpleReconnectWebSocketClient(URI uri) {
        super(uri);
    }

    public SimpleReconnectWebSocketClient(URI uri, long reconnectDelay) {
        super(uri, reconnectDelay);
    }
}
