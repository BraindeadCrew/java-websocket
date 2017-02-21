package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.server.SimpleWebSocketServer;
import fr.braindead.websocket.server.WebSocketServer;
import fr.braindead.websocket.util.Counter;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by leiko on 2/10/17.
 */
public class IdleClientTest extends TestConfig {

    @Test
    public void testIdle() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        CountDownLatch onCloseLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleIdleWebSocketClient(DEFAULT_URI, 300) {

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Assert.assertEquals(IDLE_CODE, code);
                Assert.assertEquals("idle disconnection", reason);
                Assert.assertFalse(remote);
                onCloseLatch.countDown();
            }
        };
        client.connectBlocking();

        // it should take ~300ms for the client to close itself for idling
        // so we put a timeout of 500ms just to be sure that it does not
        // take too much
        boolean succeed = onCloseLatch.await(500, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Idle disconnection took too long", succeed);

        server.stop();
    }

    @Test(timeout = 3000)
    public void testIdleReset() throws Exception {
        final int MSG_COUNT = 5;
        Counter counter = new Counter();
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onMessage(WebSocket client, String msg) {
                counter.inc();
            }
        };
        server.start();

        CountDownLatch onCloseLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleIdleWebSocketClient(DEFAULT_URI, 2000) {

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Assert.assertEquals(IDLE_CODE, code);
                Assert.assertEquals("idle disconnection", reason);
                Assert.assertFalse(remote);
                onCloseLatch.countDown();
            }
        };
        client.connectBlocking();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (int i=0; i < MSG_COUNT; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(300);
                    client.send("Message " + id);
                } catch (InterruptedException ignore) {}
            });
        }

        onCloseLatch.await();
        Assert.assertEquals("Connection has been closed for idle-ing too soon", MSG_COUNT, counter.getValue());

        server.stop();
    }

    @Test(timeout = 3000)
    public void testShouldReceiveMessage() throws Exception {
        CountDownLatch msgLatch = new CountDownLatch(1);
        receiveMessage(new SimpleIdleWebSocketClient(DEFAULT_URI) {
            @Override
            public void onMessage(String msg) {
                msgLatch.countDown();
            }
        }, msgLatch);
    }
}
