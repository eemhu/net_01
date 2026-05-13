/*
 * Java Zero Copy Networking Library net_01
 * Copyright (C) 2024 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.net_01.channel.server;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLeaseStub;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.buf_01.buffer.supply.ArenaMemorySegmentLeaseSupplier;
import com.teragrep.net_01.channel.context.ClockFactory;
import com.teragrep.net_01.channel.context.EchoingClockFactory;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.Server;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.poj_01.pool.UnboundPool;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ServerSendingTest {

    private Server server;
    private CountDownLatch countDownLatch;
    private OpeningPool pool;
    private Thread elT;

    @BeforeAll
    void beforeAll() {
        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        final SocketFactory socketFactory = new PlainFactory();
        this.pool = new OpeningPool(
                new UnboundPool<>(new ArenaMemorySegmentLeaseSupplier(Arena.ofShared(), 128), new MemorySegmentLeaseStub())
        );
        final ClockFactory clockFactory = new EchoingClockFactory((msgStr) -> countDownLatch.countDown(), pool);

        final EventLoop el = Assertions.assertDoesNotThrow(eventLoopFactory::create);

        // eventLoopThread must run, otherwise nothing will be processed
        elT = new Thread(el);
        elT.start();

        final ServerFactory serverFactory = new ServerFactory(
                el,
                Executors.newSingleThreadExecutor(),
                socketFactory,
                clockFactory
        );

        this.server = Assertions.assertDoesNotThrow(() -> serverFactory.create(9090));
    }

    @AfterAll
    void afterAll() {
        Assertions.assertDoesNotThrow(this.server::close);
        Assertions.assertDoesNotThrow(this.pool::close);
        elT.interrupt();
    }

    @Test
    void testSending() {
        Assertions.assertDoesNotThrow(() -> {
            this.countDownLatch = new CountDownLatch(1);
            try (final java.net.Socket clientSocket = new java.net.Socket("localhost", 9090)) {
                final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                final String request = "Hello world! This is some input";
                out.println(request);
                out.flush();

                countDownLatch.await();

                final String resp = Assertions.assertDoesNotThrow(in::readLine);

                // SendingClock replies with the request
                Assertions.assertEquals(request, resp);
                in.close();
                out.close();
            }
        });

    }
}
