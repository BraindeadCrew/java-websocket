package fr.braindead.websocket.server;

import fr.braindead.websocket.TestConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class ServerTest extends TestConfig {

    @Test
    public void testConnection() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking();

        Mockito.verify(this.client, Mockito.timeout(250).times(1)).onOpen();
        //Mockito.verify(this.server, Mockito.timeout(250).times(1)).onOpen(Mockito.any(WebSocket.class));
    }
}
