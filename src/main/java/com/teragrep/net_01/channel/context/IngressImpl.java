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
package com.teragrep.net_01.channel.context;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLease;
import com.teragrep.buf_01.buffer.lease.OpenableLease;
import com.teragrep.buf_01.buffer.pool.LeaseMultiGet;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.poj_01.pool.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tlschannel.NeedsReadException;
import tlschannel.NeedsWriteException;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

final class IngressImpl implements Ingress {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngressImpl.class);
    private final EstablishedContextImpl establishedContext;
    private final Pool<OpenableLease<MemorySegment>> memorySegmentLeasePool;

    private final List<TrackedMemorySegmentLease> activeBuffers;
    private final Lock lock;
    // tls
    public final AtomicBoolean needWrite;

    private final List<Clock> interestedClocks;

    IngressImpl(EstablishedContextImpl establishedContext, Pool<OpenableLease<MemorySegment>> memorySegmentLeasePool) {
        this.establishedContext = establishedContext;
        this.memorySegmentLeasePool = memorySegmentLeasePool;

        this.activeBuffers = Collections.synchronizedList(new LinkedList<>());
        this.lock = new ReentrantLock();
        this.needWrite = new AtomicBoolean();

        this.interestedClocks = Collections.synchronizedList(new LinkedList<>());
    }

    @Override
    public void run() {
        LOGGER.debug("run entry!");
        lock.lock();
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("run lock! with activeBuffers.size() <{}>", activeBuffers.size());
            }
            while (true) {
                LOGGER.debug("run loop start");

                // fill buffers for read
                long readBytes = readData();

                if (!isDataAvailable(readBytes)) {
                    System.out.println("No data available");
                    break;
                }

                boolean continueReading = true;
                System.out.println("activeBuffers.isEmpty=" + activeBuffers.isEmpty());
                while (!activeBuffers.isEmpty()) {
                    // IMPORTANT: current tls implementation will skip bytes if BufferLeases are not fully consumed.
                    TrackedMemorySegmentLease bufferLease = activeBuffers.removeFirst();
                    LOGGER
                            .debug(
                                    "submitting buffer <{}> from activeBuffers <{}> to relpFrame", bufferLease,
                                    activeBuffers
                            );

                    if (!interestedClocks.isEmpty()) {
                        for (Clock clock : interestedClocks) {
                            clock.advance(bufferLease);
                        }
                    }

                    if (interestedClocks.isEmpty()) {
                        continueReading = false;
                    }

                    LOGGER.debug("clock returned continueReading <{}>", continueReading);

                    if (bufferLease.hasNext()) {
                        // return back as it has some remaining
                        System.out.println("something remaining");
                        LOGGER.debug("pushBack bufferLease id <{}>", bufferLease.id());
                        activeBuffers.add(bufferLease);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER
                                    .debug(
                                            "buffer.leasedObject <{}>, buffer.hasNext <{}> returned it to activeBuffers <{}>",
                                            bufferLease.leasedObject(), bufferLease.hasNext(), activeBuffers
                                    );
                        }
                    }
                    if (!continueReading) {
                        break;
                    }
                }
                if (!continueReading) {
                    break;
                }
            }
        }
        catch (NeedsReadException nre) {
            LOGGER.debug("need read", nre);
            try {
                establishedContext.interestOps().add(OP_READ);
            }
            catch (CancelledKeyException cke) {
                LOGGER.debug("Connection already closed for need read.", cke);
                establishedContext.close();
            }
            catch (Throwable t) {
                LOGGER.error("unexpected error while changing socket interest operations to OP_READ", t);
            }
        }
        catch (NeedsWriteException nwe) {
            LOGGER.debug("need write", nwe);
            needWrite.set(true);
            try {
                establishedContext.interestOps().add(OP_WRITE);
            }
            catch (CancelledKeyException cke) {
                LOGGER.debug("Connection already closed for need write.", cke);
                establishedContext.close();
            }
            catch (Throwable t) {
                LOGGER.error("unexpected error while changing socket interest operations to OP_WRITE", t);
            }
        }
        catch (EndOfStreamException eose) {
            // close connection
            try {
                LOGGER
                        .warn(
                                "End of stream for PeerAddress <{}> PeerPort <{}>. Closing Connection.",
                                establishedContext.socket().getTransportInfo().getPeerAddress(),
                                establishedContext.socket().getTransportInfo().getPeerPort()
                        );
            }
            catch (Exception ignored) {

            }
            finally {
                establishedContext.close();
            }
        }
        catch (Throwable t) {
            LOGGER.error("run() threw", t);
            establishedContext.close();
        }
        finally {
            lock.unlock();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("thread <{}> going to pool", Thread.currentThread());
            }
        }
    }

    private boolean isDataAvailable(long readBytes) throws IOException {
        boolean rv;
        if (readBytes == 0) {
            // socket needs to read more
            establishedContext.interestOps().add(OP_READ);
            LOGGER.debug("more bytes requested from socket");
            rv = false;
        }
        else if (readBytes < 0) {
            throw new EndOfStreamException("negative readBytes <" + readBytes + ">");
        }
        else {
            rv = true;
        }
        return rv;
    }

    private long readData() throws IOException {
        long readBytes;

        List<OpenableLease<MemorySegment>> bufferLeases = new LeaseMultiGet(memorySegmentLeasePool).get(4);

        List<ByteBuffer> byteBufferList = new LinkedList<>();
        for (OpenableLease<MemorySegment> bufferLease : bufferLeases) {
            if (bufferLease.isStub()) {
                continue;
            }
            byteBufferList.add(bufferLease.leasedObject().asByteBuffer());
        }
        ByteBuffer[] byteBufferArray = byteBufferList.toArray(new ByteBuffer[0]);

        readBytes = establishedContext.socket().read(byteBufferArray);
        System.out.println("readBytes = " + readBytes);
            long bytesLeft = readBytes;
            boolean allRead = false;
            for (final OpenableLease<MemorySegment> bufferLease : bufferLeases) {
                final long byteSize = bufferLease.leasedObject().byteSize();

                if (!allRead) {
                    activeBuffers.add(new TrackedMemorySegmentLease(bufferLease, new AtomicLong(Math.min(bytesLeft, byteSize))));
                }

                bytesLeft -= byteSize;

                if (bytesLeft <= 0) {
                    allRead = true;
                }
            }

        System.out.println("buffers.size=" + activeBuffers.size());

        LOGGER.debug("establishedContext.read got <{}> bytes from socket", readBytes);

        return readBytes;
    }

    public AtomicBoolean needWrite() {
        return needWrite;
    }

    @Override
    public void register(Clock clock) {
        lock.lock();
        try {
            if (!interestedClocks.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to register ingress clock, only one interested clock is allowed"
                );
            }
            interestedClocks.add(clock);
            establishedContext.interestOps().add(OP_READ);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void unregister(Clock clock) {
        lock.lock();
        try {
            if (!interestedClocks.contains(clock)) {
                throw new IllegalStateException("Unable to unregister ingress clock, it is not registered");
            }
            interestedClocks.remove(clock);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        lock.lock();
        try {
            for (Clock clock : interestedClocks) {
                clock.close();
            }
        }
        finally {
            lock.unlock();
        }
    }
}
