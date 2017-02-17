package fr.braindead.websocket.server;

import fr.braindead.websocket.TestConfig;
import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.client.SimpleWebSocketClient;
import fr.braindead.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class ServerTest extends TestConfig {

    @Test(timeout = 5000)
    public void testConnection() throws Exception {
        CountDownLatch serverOnOpenLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onOpen(WebSocket client) {
                serverOnOpenLatch.countDown();
            }
        };
        server.start();

        WebSocketClient client = createClient();
        client.connectBlocking();
        serverOnOpenLatch.await();

        Assert.assertEquals(1, server.getClients().size());

        server.stop();
    }

    @Test(timeout = 5000)
    public void testMessage() throws Exception {
        CountDownLatch serverOnMessageLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onMessage(WebSocket client, String msg) {
                Assert.assertEquals("Hello world", msg);
                serverOnMessageLatch.countDown();
            }
        };
        server.start();

        WebSocketClient client = createClient();
        client.connectBlocking();
        client.send("Hello world");

        serverOnMessageLatch.await();

        server.stop();
    }

    @Test(timeout = 5000)
    public void testClose() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        CountDownLatch clientOnCloseLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onOpen() {
                try {
                    server.stop();
                } catch (ServerStoppedException ignore) {}
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Assert.assertEquals(1000, code);
                Assert.assertEquals("", reason);
                Assert.assertTrue(remote);
                clientOnCloseLatch.countDown();
            }
        };
        client.connect();

        clientOnCloseLatch.await();
    }

}
