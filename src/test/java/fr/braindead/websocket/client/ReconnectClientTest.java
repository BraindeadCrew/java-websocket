package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import fr.braindead.websocket.WebSocket;
import fr.braindead.websocket.server.SimpleWebSocketServer;
import fr.braindead.websocket.server.WebSocketServer;
import fr.braindead.websocket.util.Counter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by leiko on 2/10/17.
 */
public class ReconnectClientTest extends TestConfig {

    @Test(timeout = 3000)
    public void testConnect() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        CountDownLatch onOpenLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleReconnectWebSocketClient(DEFAULT_URI, 300) {
            @Override
            public void onOpen() {
                onOpenLatch.countDown();
            }
        };
        client.connect();

        onOpenLatch.await();

        server.stop();
    }

    @Test(timeout = 3000)
    public void testConnectLoop() throws Exception {
        WebSocketServer server = createServer();
        // but don't start it yet

        Counter failCount = new Counter();
        CountDownLatch onOpenLatch = new CountDownLatch(1);
        WebSocketClient client = new SimpleReconnectWebSocketClient(DEFAULT_URI, 300) {
            @Override
            public void onOpen() {
                onOpenLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                failCount.inc();
            }
        };
        client.connect();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                // now start the server
                server.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        onOpenLatch.await();
        // during the 1000ms of waiting, the client should have tried 4 times to connect
        // because the reconnect timeout is set to 300ms so:
        // t0:          = 0    (fail)
        // t1: t0 + 300 = 300  (fail)
        // t2: t1 + 300 = 600  (fail)
        // t3: t2 + 300 = 900  (fail)
        // t4: t3 + 300 = 1200 (success)
        Assert.assertTrue("at least tried to reconnect 4 times", failCount.getValue() >= 4);

        server.stop();
    }

    @Test
    public void testConnectBlocking() throws Exception {
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        CountDownLatch serverOnOpenLatch = new CountDownLatch(1);
        WebSocketServer server = new SimpleWebSocketServer(DEFAULT_PORT) {
            @Override
            public void onOpen(WebSocket client) {
                serverOnOpenLatch.countDown();
                // automatically close incoming client after 1000ms
                scheduledExecutor.schedule(() -> {
                    try {
                        client.close(3900, "fake code just not to throw but to trigger the reconnect process");
                    } catch (IOException ignore) {}
                }, 1000, TimeUnit.MILLISECONDS);
            }
        };

        Counter errorCounter = new Counter();
        WebSocketClient client = new SimpleReconnectWebSocketClient(DEFAULT_URI, 300) {
            @Override
            public void onError(Throwable error) {
                if (error instanceof ConnectException) {
                    errorCounter.inc();
                }
            }
        };

        scheduledExecutor.schedule(server::start, 1000, TimeUnit.MILLISECONDS);

        client.connectBlocking();
        serverOnOpenLatch.await();
        Assert.assertTrue("at least tried to reconnect 4 times", errorCounter.getValue() >= 4);

        server.stop();
    }

    @Test
    public void testWontReconnectIfNormalClose() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        Counter errorCounter = new Counter();
        WebSocketClient client = new SimpleReconnectWebSocketClient(DEFAULT_URI, 100) {
            @Override
            public void onError(Throwable e) {
                errorCounter.inc();
            }
        };
        client.connectBlocking();
        client.close();

        Thread.sleep(500);
        Assert.assertEquals(0, errorCounter.getValue());

        server.stop();
    }

    @Test
    public void testShouldTryToReconnectIfRemoteClose() throws Exception {
        WebSocketServer server = createServer();
        server.start();

        Counter errorCounter = new Counter();
        WebSocketClient client = new SimpleReconnectWebSocketClient(DEFAULT_URI, 300) {
            @Override
            public void onError(Throwable e) {
                errorCounter.inc();
            }
        };
        client.connectBlocking();
        server.stop();

        Thread.sleep(750);
        // during the 750ms sleep the reconnect process should have been called twice
        Assert.assertEquals(2, errorCounter.getValue());
    }
}
