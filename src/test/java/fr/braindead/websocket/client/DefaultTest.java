package fr.braindead.websocket.client;

import fr.braindead.websocket.TestConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/15/17.
 */
public class DefaultTest extends TestConfig {

    @Test
    public void testClose() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking();
        this.client.close();

        Mockito.verify(this.client, Mockito.timeout(250).times(1)).onClose(Mockito.eq(1000), Mockito.eq(""), Mockito.eq(false));
    }

    @Test
    public void testCustomClose() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking();
        this.client.close(1042, "foo");

        Mockito.verify(this.client, Mockito.timeout(250).times(1)).onClose(Mockito.eq(1042), Mockito.eq("foo"), Mockito.eq(false));
    }

    @Test
    public void testServerClose() throws IOException, InterruptedException {
        this.server.start();
        this.client.connectBlocking();

        this.server.stop();

        Mockito.verify(this.client, Mockito.timeout(250).times(1)).onClose(Mockito.eq(1000), Mockito.eq(""), Mockito.eq(false));
    }
}
