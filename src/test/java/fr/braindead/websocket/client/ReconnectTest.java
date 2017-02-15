package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/10/17.
 */
public class ReconnectTest extends TestConfig {

    @Override
    public void setUpClient() {
        this.client = Mockito.spy(new SimpleReconnectWebSocketClient(DEFAULT_URI, 100));
    }

    @Test
    public void testConnect() throws InterruptedException, IOException {
        this.server.start();
        this.client.connect();

        Mockito.verify(this.client, Mockito.timeout(250).times(1))
                .onOpen();
    }

    @Test
    public void testReconnect() throws InterruptedException, IOException {
        this.client.connect();

        Mockito.verify(this.client, Mockito.after(300).times(3))
                .connect();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConnectBlocking() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConnectBlockingWithTimeout() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking(42000);
    }

    @Test
    public void testSend() throws IOException {
        this.server.start();
        this.client.connect();
        this.client.send("Hello world");

//        Mockito.verify(this.server, Mockito.after(150).times(1))
//                .onOpen(Mockito.any());
//        Mockito.verify(this.server, Mockito.after(150).times(1))
//                .onMessage(Mockito.any(WebSocket.class), Mockito.eq("Hello world"));
    }
}
