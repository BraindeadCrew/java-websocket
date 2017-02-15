package fr.braindead.websocket.client;

/**
 * Created by leiko on 27/02/15.
 *
 */
public interface WebSocketClientHandlers {

    void onOpen();

    void onMessage(String msg);

    void onClose(int code, String reason, boolean remote);

    void onError(Throwable e);
}
