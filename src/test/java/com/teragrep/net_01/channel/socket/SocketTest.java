package com.teragrep.net_01.channel.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;

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

    @Test
    void testPlainSocketWrite() {
        Assertions.assertDoesNotThrow(() -> {
            // Create ServerSocketChannel and bind it to port=9090
            ServerSocketChannel socketCh = ServerSocketChannel.open();
            socketCh.bind(new InetSocketAddress(9090));

            // Init client
            java.net.Socket clientSocket = new java.net.Socket("localhost", 9090);
            final BufferedReader in = new BufferedReader(new InputStreamReader(Assertions.assertDoesNotThrow(clientSocket::getInputStream)));

            // Init PlainSocket
            Socket socket = new PlainSocket(socketCh.accept());

            socket.write(stringToBuffer("helloWorld\n"));

            final String readLine = in.readLine();

            Assertions.assertEquals("helloWorld", readLine);

            clientSocket.close();
            socketCh.close();
            socket.close();
        });
    }

    @Test
    void testPlainSocketRead() {
        Assertions.assertDoesNotThrow(() -> {
            // Create ServerSocketChannel and bind it to port=9090
            ServerSocketChannel socketCh = ServerSocketChannel.open();
            socketCh.bind(new InetSocketAddress(9090));

            // Init client
            java.net.Socket clientSocket = new java.net.Socket("localhost", 9090);
            final PrintWriter out = new PrintWriter(Assertions.assertDoesNotThrow(clientSocket::getOutputStream), true);

            // Init PlainSocket
            Socket socket = new PlainSocket(socketCh.accept());

            out.println("worldHello");

            ByteBuffer[] bufs = emptyBuffers(1, 10);
            socket.read(bufs);

            Assertions.assertEquals("worldHello", bufferToString(bufs));

            clientSocket.close();
            socketCh.close();
            socket.close();
        });
    }

    private String bufferToString(final ByteBuffer[] bufs) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final ByteBuffer buf : bufs) {
            buf.flip();
            while (buf.hasRemaining()) {
                stringBuilder.append((char) buf.get());
            }
            buf.flip();
        }
        return stringBuilder.toString();
    }

    private ByteBuffer[] stringToBuffer(final String str) {
        final ByteBuffer[] arr = new ByteBuffer[1];

        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        arr[0] = ByteBuffer.wrap(bytes);

        return arr;
    }

    private ByteBuffer[] emptyBuffers(int n, int bytesEach) {
        final ByteBuffer[] arr = new ByteBuffer[n];

        for (int i = 0; i < n; i++) {
            arr[i] = ByteBuffer.allocate(bytesEach);
        }

        return arr;
    }
}
