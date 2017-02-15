package fr.braindead.websocket.client;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.xnio.IoFuture;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by leiko on 2/15/17.
 */
public abstract class ReconnectWebSocketClient extends WebSocketClientImpl {

    /**
     * Default reconnection delay in milliseconds
     */
    public static final long DEFAULT_RECONNECT_DELAY = 3000;

    private long reconnectDelay;
    private ScheduledExecutorService executorService;

    public ReconnectWebSocketClient(URI uri) {
        this(uri, DEFAULT_RECONNECT_DELAY);
    }

    public ReconnectWebSocketClient(URI uri, long reconnectDelay) {
        super(uri);
        this.reconnectDelay = reconnectDelay;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void connect() {
        try {
            connectionBuilder().connect().addNotifier(((ioFuture, client) -> {
                if (ioFuture.getStatus() == IoFuture.Status.DONE) {
                    // ok
                    try {
                        this.channel = ioFuture.get();
                        registerChannelReceivers();
                    } catch (IOException ignore) { /* should never happen because Status.DONE */ }
                } else {
                    startReconnectTask();
                }
            }), this);
        } catch (IOException e) {
            startReconnectTask();
        }
    }

    @Override
    public boolean connectBlocking() throws IOException {
        throw new UnsupportedOperationException("Blocking connection is not available for ReconnectWebSocketClient");
    }

    @Override
    public boolean connectBlocking(long timeout) throws IOException {
        throw new UnsupportedOperationException("Blocking connection is not available for ReconnectWebSocketClient");
    }

    @Override
    protected void registerChannelReceivers() {
        this.channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage msgBuffer) throws IOException {
                super.onFullTextMessage(channel, msgBuffer);
                ReconnectWebSocketClient.this.onMessage(msgBuffer.getData());
            }

            @Override
            protected void onError(WebSocketChannel wsChannel, Throwable error) {
                super.onError(wsChannel, error);
                if (error instanceof ConnectException) {
                    // unable to connect => try to startReconnectTask in 'reconnectDelay' milliseconds
                    startReconnectTask();
                }
                ReconnectWebSocketClient.this.onError(error);
            }
        });

        this.channel.addCloseTask(chan -> {
            if (chan.getCloseCode() != 1000) {
                // abnormal close => try to startReconnectTask in 'reconnectDelay' milliseconds
                startReconnectTask();
            }
            ReconnectWebSocketClient.this.onClose(chan.getCloseCode(), chan.getCloseReason(), chan.isCloseInitiatedByRemotePeer());
        });

        this.channel.resumeReceives();
        this.onOpen();
    }

    private void startReconnectTask() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(ReconnectWebSocketClient.this::connect, reconnectDelay, TimeUnit.MILLISECONDS);
    }
}
