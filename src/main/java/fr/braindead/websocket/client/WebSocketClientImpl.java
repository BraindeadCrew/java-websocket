package fr.braindead.websocket.client;

import fr.braindead.websocket.SendCallback;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.core.*;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by leiko on 27/02/15.
 *
 */
public abstract class WebSocketClientImpl implements WebSocketClient {

    public static final long DEFAULT_TIMEOUT = 10;

    protected URI uri;
    protected CountDownLatch connectLatch;
    protected WebSocketChannel channel;

    /**
     * Creates a WebSocket client using the given WebSocketChannel for communications
     * @param channel undertow WebSocket channel
     */
    public WebSocketClientImpl(WebSocketChannel channel) {
        this.channel = channel;
    }

    /**
     * Creates a WebSocket client connecting to the given URI endpoint
     * @param uri web socket server uri
     */
    public WebSocketClientImpl(URI uri) {
        this.uri = uri;
    }

    /**
     * Non-blocking connection attempt to the given URI endpoint
     * @throws IOException if something goes wrong
     */
    @Override
    public void connect() throws IOException {
        connectionBuilder().connect().addNotifier((ioFuture, client) -> {
            if (ioFuture.getStatus() == IoFuture.Status.DONE) {
                try {
                    this.channel = ioFuture.get();
                    registerChannelReceivers();
                } catch (IOException ignore) { /* should never happen because Status.DONE */ }
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
        IoFuture<WebSocketChannel> futureChan = connectionBuilder().connect();
        IoFuture.Status status = futureChan.await(timeout, TimeUnit.SECONDS);
        switch (status) {
            case DONE:
                // ok
                this.channel = futureChan.get();
                registerChannelReceivers();
                return true;

            default:
                // error or interrupted or timed-out
                return false;
        }
    }

    protected ConnectionBuilder connectionBuilder() throws IOException {
        XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.EMPTY);
        return new ConnectionBuilder(worker, new DefaultByteBufferPool(true, 1024), uri);
    }

    protected void registerChannelReceivers() {
        this.channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage msgBuffer) throws IOException {
                super.onFullTextMessage(channel, msgBuffer);
                WebSocketClientImpl.this.onMessage(msgBuffer.getData());
            }

            @Override
            protected void onError(WebSocketChannel wsChannel, Throwable error) {
                super.onError(wsChannel, error);
                WebSocketClientImpl.this.onError(error);
            }
        });

        this.channel.addCloseTask(wsChannel -> this.onClose(
                wsChannel.getCloseCode(), wsChannel.getCloseReason(), wsChannel.isCloseInitiatedByRemotePeer()));

        this.channel.resumeReceives();

        this.onOpen();
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
     * @param code close code
     * @param reason close reason
     * @throws IOException if something goes wrong
     */
    @Override
    public void close(int code, String reason) throws IOException {
        if (this.channel != null) {
            this.channel.setCloseCode(code);
            this.channel.setCloseReason(reason);
            this.channel.close();
        }
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
    public void send(String text, SendCallback callback) {
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
}
