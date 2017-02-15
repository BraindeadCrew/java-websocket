package fr.braindead.websocket.server;

import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.client.SimpleWebSocketClient;
import fr.braindead.websocket.client.WebSocketClient;
import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
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
    private Set<WebSocket> clients = new HashSet<>();

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
        this.server.stop();
    }

    @Override
    public Set<WebSocket> getClients() {
        return this.wsHandler.getPeerConnections()
                .stream()
                .map(SimpleWebSocketClient::new)
                .collect(Collectors.toSet());
    }

    public class ConnectionHandler implements WebSocketConnectionCallback {

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            final WebSocket client = new SimpleWebSocketClient(channel);

            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                    WebSocketServerImpl.this.onMessage(client, message.getData());
                }

                @Override
                protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message)
                        throws IOException {
                    System.out.println("full close message: " + message.getData());
                    // Overriding onFullCloseMessage so that onFullTextMessage is called even though the data were sent by fragments
                }

                @Override
                protected void onClose(WebSocketChannel channel, StreamSourceFrameChannel frame) throws IOException {
                    WebSocketServerImpl.this.onClose(
                            client, channel.getCloseCode(), channel.getCloseReason(), channel.isCloseInitiatedByRemotePeer());
                }

                @Override
                protected void onError(WebSocketChannel channel, Throwable error) {
                    WebSocketServerImpl.this.onError(client, error);
                }
            });

            channel.addCloseTask(chan -> WebSocketServerImpl.this.onClose(
                    client, chan.getCloseCode(), chan.getCloseReason(), chan.isCloseInitiatedByRemotePeer()));

            channel.resumeReceives();

            WebSocketServerImpl.this.onOpen(client);
        }
    }

    public static void main(String[] args) throws IOException {
        WebSocketClient client = new SimpleWebSocketClient(URI.create("ws://localhost:9000")) {
            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println(String.format("%s, \"%s\", %s", code, reason, remote));
            }
        };

        client.connect();
    }
}
