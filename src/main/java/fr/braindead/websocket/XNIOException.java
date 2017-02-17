package fr.braindead.websocket;

import java.io.IOException;

/**
 *
 * Created by leiko on 2/17/17.
 */
public class XNIOException extends IOException {

    public XNIOException(IOException e) {
        super(e);
    }
}
