package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/10/17.
 */
public class IdleTest extends TestConfig {

    @Override
    public void setUpClient() {
        this.client = Mockito.spy(new SimpleIdleWebSocketClient(DEFAULT_URI, 300));
    }

    @Test
    public void testIdle() throws InterruptedException, IOException {
        this.server.start();
        this.client.connectBlocking();

        Assert.assertTrue(this.client.isOpen());
        Mockito.verify(this.client, Mockito.timeout(500).times(1))
                .onClose(Mockito.eq(IdleWebSocketClient.IDLE_CODE), Mockito.eq("idle disconnection"), Mockito.eq(false));
    }
}
