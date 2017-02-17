package fr.braindead.websocket;

import fr.braindead.websocket.client.SimpleWebSocketClient;
import fr.braindead.websocket.client.WebSocketClient;
import fr.braindead.websocket.server.SimpleWebSocketServer;
import fr.braindead.websocket.server.WebSocketServer;

import java.net.URI;

/**
 *
 * Created by leiko on 2/15/17.
 */
public abstract class TestConfig {

    protected static final int DEFAULT_PORT = 9000;
    protected static final URI DEFAULT_URI = URI.create("ws://localhost:" + DEFAULT_PORT);

    protected WebSocketClient createClient() {
        return new SimpleWebSocketClient(DEFAULT_URI);
    }

    protected WebSocketServer createServer() {
        return new SimpleWebSocketServer(DEFAULT_PORT);
    }
}
