package com.teragrep.net_01.channel.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class SocketTest {

    @Test
    void testPlainSocketConnection() {
        Assertions.assertDoesNotThrow(() -> {
            // Create ServerSocketChannel and bind it to port=9090
            ServerSocketChannel socketCh = ServerSocketChannel.open();
            socketCh.bind(new InetSocketAddress(9090));

            // Init client
            java.net.Socket clientSocket = new java.net.Socket("localhost", 9090);

            // Init PlainSocket
            Socket socket = new PlainSocket(socketCh.accept());

            clientSocket.close();
            socketCh.close();
            socket.close();
        });
    }
}
