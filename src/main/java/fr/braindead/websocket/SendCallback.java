package fr.braindead.websocket;

/**
 *
 * Created by leiko on 2/14/17.
 */
public interface SendCallback {

    void complete(Throwable error);
}
