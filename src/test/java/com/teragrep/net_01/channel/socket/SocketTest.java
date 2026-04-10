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
package com.teragrep.net_01.channel.socket;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLeaseStub;
import com.teragrep.buf_01.buffer.lease.OpenableLease;
import com.teragrep.buf_01.buffer.pool.LeaseMultiGet;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.buf_01.buffer.supply.ArenaMemorySegmentLeaseSupplier;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.poj_01.pool.UnboundPool;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class SocketTest {

    private OpeningPool pool;

    @BeforeAll
    void beforeAll() {
        this.pool = new OpeningPool(
                new UnboundPool<>(new ArenaMemorySegmentLeaseSupplier(Arena.ofShared(), 128), new MemorySegmentLeaseStub())
        );
    }

    @AfterAll
    void afterAll() {
        this.pool.close();
    }

    @Test
    void testPlainSocketConnection() {
        Assertions.assertDoesNotThrow(() -> {
            // Create ServerSocketChannel and bind it to port=9090
            final ServerSocketChannel socketCh = ServerSocketChannel.open();
            socketCh.bind(new InetSocketAddress(9090));

            // Init client
            final java.net.Socket clientSocket = new java.net.Socket("localhost", 9090);

            // Init PlainSocket
            final Socket socket = new PlainSocket(socketCh.accept());

            clientSocket.close();
            socketCh.close();
            socket.close();
        });
    }

    @Test
    void testPlainSocketWrite() {
        Assertions.assertDoesNotThrow(() -> {
            // Create ServerSocketChannel and bind it to port=9090
            final ServerSocketChannel socketCh = ServerSocketChannel.open();
            socketCh.bind(new InetSocketAddress(9090));

            // Init client
            final java.net.Socket clientSocket = new java.net.Socket("localhost", 9090);
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(Assertions.assertDoesNotThrow(clientSocket::getInputStream))
            );

            // Init PlainSocket
            final Socket socket = new PlainSocket(socketCh.accept());

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

            List<TrackedMemorySegmentLease> bufs = emptyBuffers(10);
            ReadResult res = socket.read(bufs);

            Assertions.assertEquals("worldHello\n", bufferToString(res.leases()));

            clientSocket.close();
            socketCh.close();
            socket.close();
        });
    }

    private String bufferToString(final List<TrackedMemorySegmentLease> leases) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final TrackedMemorySegmentLease buf : leases) {
            while (buf.hasNext()) {
                stringBuilder.append((char) buf.next());
            }
        }
        return stringBuilder.toString();
    }

    private List<TrackedMemorySegmentLease> stringToBuffer(final String str) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        final List<TrackedMemorySegmentLease> leases = emptyBuffers(bytes.length);

        Iterator<TrackedMemorySegmentLease> it = leases.iterator();
        TrackedMemorySegmentLease currentLease = it.next();
        int currentIndex = 0;
        long size = currentLease.leasedObject().byteSize();
        for (int i = 0; i < bytes.length; i++) {
            System.out.println("i: " + i + " " + bytes[i]);
            if (currentIndex < size) {
                currentLease.leasedObject().set(ValueLayout.JAVA_BYTE, currentIndex, bytes[i]);
                currentIndex++;
            }
            else {
                if (!it.hasNext()) {
                    throw new IllegalStateException();
                }
                currentLease = it.next();
                currentIndex = 0;
                size = currentLease.leasedObject().byteSize();
                currentLease.leasedObject().set(ValueLayout.JAVA_BYTE, currentIndex, bytes[i]);
            }
        }

        System.out.println("stringToBuffer");
        leases.forEach(l -> {
            System.out.println(Arrays.toString(l.leasedObject().toArray(ValueLayout.JAVA_BYTE)));
        });

        return leases;
    }

    private List<TrackedMemorySegmentLease> emptyBuffers(int bytes) {
        final List<OpenableLease<MemorySegment>> leases = new LeaseMultiGet(pool).get(bytes);
        final List<TrackedMemorySegmentLease> rv = new ArrayList<>(leases.size());

        leases.forEach(lease -> {
            rv.add(new TrackedMemorySegmentLease(lease));
        });

        return rv;
    }
}
