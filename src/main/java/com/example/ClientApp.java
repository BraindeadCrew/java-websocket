package com.example;


import fr.braindead.websocket.client.WebSocketClient;
import fr.braindead.websocket.client.WebSocketClientImpl;

import java.io.IOException;
import java.net.URI;

public class ClientApp {

    /**
     * Creates a WebSocket client that connects to ws://echo.websocket.org
     * Once connected to the endpoint it sends a "Hello world" message.
     * The server endpoint is supposed to echo it back to the sender and you should see:
     *
     * Client connected to endpoint: ws://echo.websocket.org
     * > Hello world
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // endpoint URI
        URI uri = URI.create("ws://echo.websocket.org");

        // client impl
        WebSocketClient client = new WebSocketClientImpl(uri) {
            @Override
            public void onOpen() {
                System.out.println(String.format("Client connected to endpoint: %s", uri));
                this.send("Hello world");
            }

            @Override
            public void onMessage(String msg) {
                System.out.println(String.format("> %s", msg));
                try {
                    this.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println(String.format("Connection closed (%s, \"%s\", %s)", code, reason, remote));
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Something wrong happened");
                e.printStackTrace();
            }
        };

        // connect client to server endpoint
        client.connect();
    }
}
