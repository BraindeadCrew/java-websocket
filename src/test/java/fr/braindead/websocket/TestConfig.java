package fr.braindead.websocket;

import fr.braindead.websocket.client.SimpleWebSocketClient;
import fr.braindead.websocket.client.WebSocketClient;
import fr.braindead.websocket.server.SimpleWebSocketServer;
import fr.braindead.websocket.server.WebSocketServer;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;

/**
 *
 * Created by leiko on 2/15/17.
 */
public abstract class TestConfig {

    public static final URI DEFAULT_URI = URI.create("ws://localhost:9000");
    public static final int DEFAULT_PORT = 9000;

    protected WebSocketClient client;
    protected WebSocketServer server;

    @Before
    public void setUpServer() {
        this.server = Mockito.spy(new SimpleWebSocketServer(DEFAULT_PORT));
    }

    @Before
    public void setUpClient() {
        this.client = Mockito.spy(new SimpleWebSocketClient(DEFAULT_URI));
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        this.client.close();
        this.server.stop();
    }
}
