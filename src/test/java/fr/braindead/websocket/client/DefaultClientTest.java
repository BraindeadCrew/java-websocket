package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.server.SimpleWebSocketServer;
import fr.braindead.websocket.server.WebSocketServer;
import org.junit.Assert;
import org.junit.Test;

import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class DefaultClientTest extends TestConfig {

    @Test(timeout = 3000)
    public void testConnect() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        CountDownLatch clientOnOpenLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onOpen() {
                clientOnOpenLatch.countDown();
            }
        };
        client.connect();

        clientOnOpenLatch.await();

        server.stop();
    }

    @Test(timeout = 3000)
    public void testMessage() throws Exception {
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onOpen(WebSocket client) {
                client.send("Hello world");
            }
        };
        server.start();

        CountDownLatch clientOnMessageLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onMessage(String msg) {
                Assert.assertEquals("Hello world", msg);
                clientOnMessageLatch.countDown();
            }
        };
        client.connectBlocking();

        clientOnMessageLatch.await();

        server.stop();
    }

    @Test(timeout = 3000)
    public void testClose() throws Exception {
        CountDownLatch serverOnCloseLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onClose(WebSocket client, int code, String reason, boolean remote) {
                Assert.assertEquals(1000, code);
                Assert.assertEquals("", reason);
                Assert.assertTrue(remote);
                serverOnCloseLatch.countDown();
            }
        };
        server.start();

        WebSocketClient client = createClient();
        client.connectBlocking();
        client.close();

        serverOnCloseLatch.await();

        server.stop();
    }

    @Test(timeout = 3000)
    public void testCustomCloseCode() throws Exception {
        CountDownLatch serverOnCloseLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onClose(WebSocket client, int code, String reason, boolean remote) {
                Assert.assertEquals(3800, code);
                Assert.assertEquals("", reason);
                Assert.assertTrue(remote);
                serverOnCloseLatch.countDown();
            }
        };
        server.start();

        WebSocketClient client = createClient();
        client.connectBlocking();

        client.close(3800);
        serverOnCloseLatch.await();
        server.stop();
    }

    @Test(timeout = 3000)
    public void testOnCloseRemoteFlag() throws Exception {
        CountDownLatch serverOnCloseLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onClose(WebSocket client, int code, String reason, boolean remote) {
                Assert.assertFalse(remote);
                serverOnCloseLatch.countDown();
            }
        };
        server.start();

        CountDownLatch clientOnCloseLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onClose(int code, String reason, boolean remote) {
                Assert.assertTrue(remote);
                clientOnCloseLatch.countDown();
            }
        };
        client.connectBlocking();

        server.stop();
        serverOnCloseLatch.await();
        clientOnCloseLatch.await();
    }

    @Test
    public void testFailConnectBlocking() throws Exception {
        // do not start server

        // but try to connect
        WebSocketClient client = createClient();
        boolean isConnected = client.connectBlocking();
        Assert.assertFalse(isConnected);
    }

    @Test(timeout = 3000)
    public void testFailConnect() throws Exception {
        // do not start server

        // but try to connect anyway
        CountDownLatch clientOnErrorLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onError(Throwable e) {
                Assert.assertEquals(e.getClass(), ConnectException.class);
                clientOnErrorLatch.countDown();
            }
        };
        client.connect();
        clientOnErrorLatch.await();
    }

    @Test(timeout = 3000)
    public void testShouldReceiveMessage() throws Exception {
        CountDownLatch msgLatch = new CountDownLatch(1);
        receiveMessage(new SimpleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onMessage(String msg) {
                msgLatch.countDown();
            }
        }, msgLatch);
    }
}
