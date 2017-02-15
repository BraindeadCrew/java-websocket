package fr.braindead.websocket.client;

import fr.braindead.websocket.SendCallback;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by leiko on 2/15/17.
 */
public abstract class IdleWebSocketClient extends WebSocketClientImpl {

    public final static int IDLE_CODE = 3800;

    /**
     * Default idle timeout in milliseconds
     */
    public final static long DEFAULT_IDLE_TIMEOUT = 10000;

    private long idleTimeout;
    private ScheduledExecutorService executorService;

    public IdleWebSocketClient(URI uri) {
        this(uri, DEFAULT_IDLE_TIMEOUT);
    }

    public IdleWebSocketClient(URI uri, long reconnectDelay) {
        super(uri);
        this.idleTimeout = reconnectDelay;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void connect() throws IOException {
        resetTimeout();
        super.connect();
    }

    @Override
    public boolean connectBlocking() throws IOException {
        resetTimeout();
        return super.connectBlocking();
    }

    @Override
    public boolean connectBlocking(long timeout) throws IOException {
        resetTimeout();
        return super.connectBlocking(timeout);
    }

    @Override
    public void send(String text, SendCallback callback) {
        resetTimeout();
        super.send(text, callback);
    }

    @Override
    public void close(int code, String reason) throws IOException {
        cleanExecutorService();
        super.close(code, reason);
    }

    @Override
    protected void registerChannelReceivers() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.schedule(this::shutdown, idleTimeout, TimeUnit.MILLISECONDS);
        super.registerChannelReceivers();
    }

    private void cleanExecutorService() {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
    }

    private void shutdown() {
        cleanExecutorService();
        try {
            this.close(IDLE_CODE, "idle disconnection");
        } catch (IOException ignore) {
            /* ignore exception we just want to shutdown */
        }
    }

    private void resetTimeout() {
        cleanExecutorService();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.schedule(this::shutdown, this.idleTimeout, TimeUnit.MILLISECONDS);
    }
}
