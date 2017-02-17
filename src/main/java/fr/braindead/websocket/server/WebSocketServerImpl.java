package fr.braindead.websocket.server;

import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.client.SimpleWebSocketClient;
import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static io.undertow.Handlers.websocket;

/**
 *
 * Created by leiko on 18/06/15.
 */
public abstract class WebSocketServerImpl implements WebSocketServer, WebSocketConnectionCallback {

    private WebSocketProtocolHandshakeHandler wsHandler;
    private Undertow server;

    /**
     * Creates a WebSocketServer bound to 0.0.0.0:port
     * @param port port to listen to
     */
    public WebSocketServerImpl(int port) {
        this("0.0.0.0", port);
    }

    /**
     * Creates a WebSocketServer bound to host:port
     * @param host host to listen to
     * @param port port to listen to
     */
    public WebSocketServerImpl(String host, int port) {
        this.wsHandler = websocket(this);
        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(this.wsHandler)
                .build();
    }

    @Override
    public void start() {
        if (this.server != null) {
            this.server.start();
        }
    }

    @Override
    public void stop() throws ServerStoppedException {
        if (this.server == null) {
            throw new ServerStoppedException();
        }

        this.wsHandler.getPeerConnections().forEach(chan -> {
            try { chan.sendClose(); } catch (IOException ignore) {}
        });

        this.server.stop();
        this.server = null;
    }

    @Override
    public synchronized Set<WebSocket> getClients() throws ServerStoppedException {
        if (this.server == null) {
            throw new ServerStoppedException();
        }

        return this.wsHandler.getPeerConnections()
                .stream()
                .map(SimpleWebSocketClient::new)
                .collect(Collectors.toSet());
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        final WebSocket client = new SimpleWebSocketClient(channel);

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                onMessage(client, message.getData());
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
                WebSocketServerImpl.this.onError(client, error);
            }
        });

        channel.addCloseTask(chan ->
                onClose(client, chan.getCloseCode(), chan.getCloseReason(), chan.isCloseInitiatedByRemotePeer()));

        channel.resumeReceives();

        onOpen(client);
    }
}
