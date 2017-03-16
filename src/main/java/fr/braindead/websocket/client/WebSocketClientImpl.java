package fr.braindead.websocket.client;

import fr.braindead.websocket.Callback;
import fr.braindead.websocket.XNIOException;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by leiko on 27/02/15.
 *
 */
public abstract class WebSocketClientImpl implements WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientImpl.class);

    public static final long DEFAULT_TIMEOUT = 10;

    private XnioWorker worker;
    private long idleTimeout = -1;

    protected URI uri;
    protected WebSocketChannel channel;
    protected ExecutorService handlerService;

    /**
     * Creates a WebSocket client using the given WebSocketChannel for communications
     * @param channel undertow WebSocket channel
     */
    public WebSocketClientImpl(WebSocketChannel channel) {
        this.channel = channel;
        this.channel.setIdleTimeout(this.idleTimeout);
        this.handlerService = Executors.newCachedThreadPool();
    }

    /**
     * Creates a WebSocket client connecting to the given URI endpoint
     * @param uri web socket server uri
     */
    public WebSocketClientImpl(URI uri) {
        this.uri = uri;
        this.handlerService = Executors.newCachedThreadPool();
    }

    /**
     * Non-blocking connection attempt to the given URI endpoint
     * @throws XNIOException if unable to create XNIO worker
     */
    @Override
    public void connect() throws XNIOException {
        logger.debug("Trying to connect to {}", this.uri);
        IoFuture<WebSocketChannel> chanFuture = connectionBuilder().connect();
        chanFuture.addNotifier((ioFuture, client) -> {
            if (ioFuture.getStatus() == IoFuture.Status.DONE) {
                try {
                    this.channel = ioFuture.get();
                    this.channel.setIdleTimeout(this.idleTimeout);
                    registerChannelReceivers();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                handlerService.submit(() -> onError(ioFuture.getException()));
            }
        }, this);
    }

    /**
     * Blocking connection attempt to the given URI endpoint
     *
     * @return true if connected; false otherwise
     * @throws IOException if something goes wrong with XNIO worker
     */
    @Override
    public boolean connectBlocking() throws IOException {
        return this.connectBlocking(DEFAULT_TIMEOUT);
    }

    /**
     * Blocking connection attempt to the given URI endpoint
     *
     * @param timeout time to wait in seconds before aborting connection attempt
     * @return true if connected; false otherwise
     * @throws IOException if something goes wrong with XNIO worker
     */
    @Override
    public boolean connectBlocking(long timeout) throws IOException {
        logger.debug("Connect blocking... ({})", this);
        IoFuture<WebSocketChannel> futureChan = connectionBuilder().connect();
        IoFuture.Status status = futureChan.await(timeout, TimeUnit.SECONDS);
        logger.debug("Connect blocking status: {}", status);
        switch (status) {
            case DONE:
                // ok
                this.channel = futureChan.get();
                this.channel.setIdleTimeout(this.idleTimeout);
                registerChannelReceivers();
                return true;

            default:
                // error or interrupted or timed-out
                return false;
        }
    }

    protected ConnectionBuilder connectionBuilder() throws XNIOException {
        try {
            this.worker = Xnio.getInstance().createWorker(OptionMap.EMPTY);
            return new ConnectionBuilder(this.worker, new DefaultByteBufferPool(true, 1024), uri);
        } catch (IOException e) {
            throw new XNIOException(e);
        }
    }

    protected void registerChannelReceivers() {
        this.channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel wsChannel, BufferedTextMessage msgBuffer) throws IOException {
                super.onFullTextMessage(wsChannel, msgBuffer);
                handlerService.submit(() -> onMessage(msgBuffer.getData()));
            }

            @Override
            protected void onError(WebSocketChannel wsChannel, Throwable error) {
                super.onError(wsChannel, error);
                handlerService.submit(() -> WebSocketClientImpl.this.onError(error));
            }
        });

        this.channel.addCloseTask(wsChannel -> {
            handlerService.submit(() -> onClose(wsChannel.getCloseCode(), wsChannel.getCloseReason(),
                            wsChannel.isCloseInitiatedByRemotePeer()));
            internalClose();
        });

        this.channel.resumeReceives();

        this.handlerService.submit(this::onOpen);
    }

    /**
     * Close connection with remote web socket server
     * @throws IOException if something goes wrong
     */
    @Override
    public void close() throws IOException {
        this.close(1000);
    }

    /**
     * Closes the connection
     * @param code close code
     * @throws IOException if something goes wrong
     */
    @Override
    public void close(int code) throws IOException {
        this.close(code, "");
    }

    /**
     * Closes the connection
     * @param code
     * @param reason
     * @throws IOException
     */
    @Override
    public void close(int code, String reason) throws IOException {
        this.close(code, reason, null);
    }

    /**
     * Closes the connection
     * @param code close code
     * @param reason close reason
     * @param callback optional callback #complete(error: Throwable) will be called
     *                 when done (if success then error is null)
     * @throws IOException if something goes wrong
     */
    @Override
    public void close(int code, String reason, Callback callback) throws IOException {
        if (this.channel != null) {
            WebSockets.sendClose(code, reason, this.channel, new WebSocketCallback<Void>() {
                @Override
                public void complete(WebSocketChannel channel, Void context) {
                    if (callback != null) {
                        handlerService.submit(() -> callback.complete(null));
                    }
                }

                @Override
                public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
                    if (callback != null) {
                        handlerService.submit(() -> callback.complete(throwable));
                    }
                }
            });
        }
    }

    protected void internalClose() {
        try {
            this.handlerService.shutdown();
            boolean succeed = this.handlerService.awaitTermination(5000, TimeUnit.MILLISECONDS);
            if (!succeed) {
                logger.debug("Handler service has been shutdown forcefully because of timeout (5000ms)");
            } else {
                logger.debug("Handler service has been successfully shutdown");
            }
        } catch (InterruptedException e) {
            logger.debug("Handler service has been interrupted while waiting for shutdown. Forcing shutdown...");
            this.handlerService.shutdownNow();
        } finally {
            this.worker.shutdownNow();
            logger.debug("Worker successfully shutdown");
            if (this.channel != null) {
                this.channel.getIoThread().getWorker().shutdownNow();
                logger.debug("Channel IoThread worker successfully shutdown");
                this.channel.getWorker().shutdownNow();
                logger.debug("Channel worker successfully shutdown");
            }
        }
    }

    @Override
    public void setIdleTimeout(long timeout) {
        this.idleTimeout = timeout;
    }

    /**
     *
     * @return true if connection is opened
     */
    @Override
    public boolean isOpen() {
        return this.channel != null && this.channel.isOpen();
    }

    /**
     *
     * @return true if connection is closed
     */
    @Override
    public boolean isClosed() {
        return this.channel != null && this.channel.isCloseFrameReceived();
    }

    /**
     *
     * @param text message to send to server
     */
    @Override
    public void send(String text) {
        this.send(text, null);
    }

    @Override
    public void send(String text, Callback callback) {
        if (this.channel != null && this.channel.isOpen()) {
            WebSockets.sendText(text, this.channel, new WebSocketCallback<Void>() {
                @Override
                public void complete(WebSocketChannel channel, Void ignore) {
                    if (callback != null) {
                        callback.complete(null);
                    }
                }

                @Override
                public void onError(WebSocketChannel channel, Void ignore, Throwable throwable) {
                    if (callback != null) {
                        callback.complete(throwable);
                    }
                }
            });
        }
    }

    @Override
    public String getUrl() {
        return this.channel.getUrl();
    }

    @Override
    public InetSocketAddress getDestinationAddress() {
        return this.channel.getDestinationAddress();
    }

    @Override
    public InetSocketAddress getSourceAddress() {
        return this.channel.getSourceAddress();
    }

    public static void main(String[] args) throws IOException {
        WebSocketClient client = new SimpleWebSocketClient(URI.create("ws://localhost:9000"));
        client.connect();
    }
}
