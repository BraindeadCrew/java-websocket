package fr.braindead.websocket.client;

import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.XNIOException;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/15/17.
 */
public interface WebSocketClient extends WebSocket, WebSocketClientHandlers {

    void connect() throws XNIOException;

    boolean connectBlocking() throws IOException, InterruptedException;
    boolean connectBlocking(long timeout) throws IOException, InterruptedException;
}
