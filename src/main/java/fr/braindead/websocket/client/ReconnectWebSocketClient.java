package fr.braindead.websocket.client;

import fr.braindead.websocket.XNIOException;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ReconnectWebSocketClient.class);

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
    public void connect() throws XNIOException {
        logger.debug("Trying to connect to {} ... {}", this.uri, this);
        connectionBuilder().connect().addNotifier(((ioFuture, client) -> {
            if (ioFuture.getStatus() == IoFuture.Status.DONE) {
                // ok
                try {
                    this.channel = ioFuture.get();
                    registerChannelReceivers();
                    logger.debug("Connected :) {}", this.uri, this);
                } catch (IOException ignore) {}
            } else {
                handlerService.submit(() -> onError(ioFuture.getException()));
                startReconnectTask();
            }
        }), this);
    }

    @Override
    public boolean connectBlocking() throws IOException {
        return this.connectBlocking(reconnectDelay);
    }

    @Override
    public boolean connectBlocking(long timeout) throws IOException {
        logger.debug("Connect blocking... ({})", this);
        IoFuture<WebSocketChannel> futureChan = connectionBuilder().connect();
        IoFuture.Status status = futureChan.await(timeout, TimeUnit.MILLISECONDS);
        logger.debug("Connect blocking status: {}", status);
        switch (status) {
            case DONE:
                // ok
                this.channel = futureChan.get();
                registerChannelReceivers();
                return true;

            default:
                handlerService.submit(() -> onError(futureChan.getException()));
                try {
                    Thread.sleep(reconnectDelay);
                    return this.connectBlocking(timeout);
                } catch (InterruptedException e) {
                    logger.warn("Connect blocking interrupted while sleeping", e);
                }
                // error or interrupted or timed-out
                return false;
        }
    }

    @Override
    protected void registerChannelReceivers() {
        this.channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage msgBuffer) throws IOException {
                super.onFullTextMessage(channel, msgBuffer);
                handlerService.submit(() -> onMessage(msgBuffer.getData()));
            }

            @Override
            protected void onError(WebSocketChannel wsChannel, Throwable error) {
                super.onError(wsChannel, error);
                if (error instanceof ConnectException) {
                    // unable to connect => try to startReconnectTask in 'reconnectDelay' milliseconds
                    startReconnectTask();
                }
                handlerService.submit(() -> ReconnectWebSocketClient.this.onError(error));
            }
        });

        this.channel.addCloseTask(chan -> {
            if (chan.getCloseCode() != 1000 || chan.isCloseInitiatedByRemotePeer()) {
                // abnormal close => try to startReconnectTask in 'reconnectDelay' milliseconds
                startReconnectTask();
            }
            handlerService.submit(() -> onClose(chan.getCloseCode(), chan.getCloseReason(), chan.isCloseInitiatedByRemotePeer()));
        });

        this.channel.resumeReceives();
        handlerService.submit(this::onOpen);
    }

    private void startReconnectTask() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(() -> {
            try {
                connect();
            } catch (XNIOException e) {
                // TODO handle this case!?
                logger.error("Unable to create an XNIO worker", e);
            }
        }, reconnectDelay, TimeUnit.MILLISECONDS);
    }
}
