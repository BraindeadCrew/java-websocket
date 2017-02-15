package fr.braindead.websocket.server;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class SimpleWebSocketServer extends WebSocketServerImpl implements SimpleWebSocketServerHandlers {

    public SimpleWebSocketServer(int port) {
        super(port);
    }

    public SimpleWebSocketServer(String host, int port) {
        super(host, port);
    }
}
