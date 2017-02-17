package fr.braindead.websocket;

import fr.braindead.websocket.client.WebSocketClientHandlers;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * Created by leiko on 2/14/17.
 */
public interface WebSocket extends WebSocketClientHandlers {

    void send(String text);
    void send(String text, Callback callback);

    void close() throws IOException;
    void close(int code) throws IOException;
    void close(int code, String reason) throws IOException;
    void close(int code, String reason, Callback callback) throws IOException;

    boolean isOpen();
    boolean isClosed();

    String getUrl();
    InetSocketAddress getDestinationAddress();
    InetSocketAddress getSourceAddress();
}
