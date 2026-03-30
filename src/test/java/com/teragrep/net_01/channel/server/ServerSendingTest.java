package com.teragrep.net_01.channel.server;

import com.teragrep.net_01.channel.context.ClockFactory;
import com.teragrep.net_01.channel.context.ConsumingClockFactory;
import com.teragrep.net_01.channel.context.SendingClockFactory;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.Server;
import com.teragrep.net_01.server.ServerFactory;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ServerSendingTest {
    private Server server;
    private CountDownLatch countDownLatch;
    @BeforeAll
    void beforeAll() {
        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        final SocketFactory socketFactory = new PlainFactory();
        final ClockFactory clockFactory = new SendingClockFactory((msgStr) -> countDownLatch.countDown());

        final EventLoop el = Assertions.assertDoesNotThrow(eventLoopFactory::create);

        // eventLoopThread must run, otherwise nothing will be processed
        final Thread elT = new Thread(el);
        elT.start();

        final ServerFactory serverFactory = new ServerFactory(el,
                Executors.newSingleThreadExecutor(), socketFactory, clockFactory);

        this.server = Assertions.assertDoesNotThrow(() -> serverFactory.create(9090));
    }

    @AfterAll
    void afterAll() {
        Assertions.assertDoesNotThrow(this.server::close);
    }

    @Test
    void testSending() {
        this.countDownLatch = new CountDownLatch(1);
        final java.net.Socket clientSocket = Assertions.assertDoesNotThrow(() -> new java.net.Socket("localhost", 9090));

        final PrintWriter out = new PrintWriter(Assertions.assertDoesNotThrow(clientSocket::getOutputStream), true);
        final BufferedReader in = new BufferedReader(new InputStreamReader(Assertions.assertDoesNotThrow(clientSocket::getInputStream)));
        final String request = "Hello world! This is some input";
        out.println(request);
        out.flush();

        Assertions.assertDoesNotThrow(() -> countDownLatch.await());

        final String resp = Assertions.assertDoesNotThrow(in::readLine);

        // SendingClock replies with the request
        Assertions.assertEquals(request, resp);
        Assertions.assertDoesNotThrow(in::close);
        Assertions.assertDoesNotThrow(out::close);
        Assertions.assertDoesNotThrow(clientSocket::close);
    }
}
