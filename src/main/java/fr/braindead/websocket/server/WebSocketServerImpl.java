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
public abstract class WebSocketServerImpl implements WebSocketServer {

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
        this.wsHandler = websocket(new ConnectionHandler());
        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(this.wsHandler)
                .build();
    }

    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        this.wsHandler.getPeerConnections().forEach(chan -> {
            try {
                chan.close();
            } catch (IOException ignore) {}
        });
        this.server.stop();
    }

    @Override
    public Set<WebSocket> getClients() {
        return this.wsHandler.getPeerConnections()
                .stream()
                .map(SimpleWebSocketClient::new)
                .collect(Collectors.toSet());
    }

    private final class ConnectionHandler implements WebSocketConnectionCallback {
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
}
