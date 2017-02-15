package fr.braindead.websocket.client;

import io.undertow.websockets.core.WebSocketChannel;

import java.net.URI;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class SimpleWebSocketClient extends WebSocketClientImpl implements SimpleWebSocketClientHandlers {

    public SimpleWebSocketClient(WebSocketChannel channel) {
        super(channel);
    }

    public SimpleWebSocketClient(URI uri) {
        super(uri);
    }
}
