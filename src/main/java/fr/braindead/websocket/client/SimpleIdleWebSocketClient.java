package fr.braindead.websocket.client;

import java.net.URI;

/**
 *
 * Created by leiko on 2/14/17.
 */
public class SimpleIdleWebSocketClient extends IdleWebSocketClient implements SimpleWebSocketClientHandlers {

    public SimpleIdleWebSocketClient(URI uri) {
        super(uri);
    }

    public SimpleIdleWebSocketClient(URI uri, long idleTimeout) {
        super(uri, idleTimeout);
    }
}
