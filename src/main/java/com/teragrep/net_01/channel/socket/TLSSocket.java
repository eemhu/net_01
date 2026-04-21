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

import com.teragrep.buf_01.buffer.lease.TrackedLease;
import com.teragrep.buf_01.buffer.lease.TrackedMemorySegmentLease;
import tlschannel.TlsChannel;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

final class TLSSocket implements Socket {

    private final SocketChannel socketChannel;
    private final TlsChannel tlsChannel;
    private final TransportInfo transportInfo;

    TLSSocket(SocketChannel socketChannel, TlsChannel tlsChannel) {
        this.socketChannel = socketChannel;
        this.tlsChannel = tlsChannel;
        EncryptionInfo encryptionInfo = new EncryptionInfoTLS(tlsChannel);
        this.transportInfo = new TransportInfoImpl(socketChannel, encryptionInfo);
    }

    @Override
    public ReadResult read(List<TrackedLease<MemorySegment>> srcs) throws IOException {
        final List<TrackedLease<MemorySegment>> rv = new ArrayList<>(srcs.size());
        final List<ByteBuffer> byteBuffers = new ArrayList<>(srcs.size());
        srcs.forEach(src -> {
            byteBuffers.add(src.leasedObject().asByteBuffer());
        });

        final long readBytes = tlsChannel.read(byteBuffers.toArray(new ByteBuffer[0]));

        long bytesLeft = readBytes;
        boolean allRead = false;
        for (final TrackedLease<MemorySegment> bufferLease : srcs) {
            final long byteSize = bufferLease.leasedObject().byteSize();

            if (!allRead && readBytes > 0) {
                // same as ByteBuffer.flip()
                final long diff = bytesLeft - byteSize;
                if (diff < 0) {
                    // mem.segment bigger than bytes left.
                    // set limit to read amount.
                    final long limit = byteSize - Math.abs(diff);
                    rv.add(new TrackedMemorySegmentLease(bufferLease, 0L, limit));
                }
                else {
                    //else: full mem.segment used, no need to set limit.
                    rv.add(new TrackedMemorySegmentLease(bufferLease));
                }
            }

            bytesLeft -= byteSize;

            if (bytesLeft <= 0) {
                allRead = true;
            }
        }

        //rv.forEach(l -> {
        System.out.println("Lease:");
        /*for (long i = 0 ; i < l.leasedObject().byteSize(); i++) {
            System.out.printf("%s", (char)l.leasedObject().get(ValueLayout.JAVA_BYTE, i));
        }*/

        /* while (l.hasNext()) {
             System.out.printf("%s", (char)l.next().byteValue());
         }

         System.out.println();*/
        //});
        return new ReadResult(readBytes, rv);
    }

    @Override
    public WrittenResult write(List<TrackedLease<MemorySegment>> leases) throws IOException {
        final List<ByteBuffer> buffersToWrite = new ArrayList<>(leases.size());
        final List<TrackedLease<MemorySegment>> rv = new ArrayList<>(leases.size());

        for (final TrackedLease<MemorySegment> lease : leases) {
            buffersToWrite.add(lease.leasedObject().asByteBuffer());
        }

        final long bytesWritten = tlsChannel.write(buffersToWrite.toArray(new ByteBuffer[0]));
        System.out.println("wrote bytes: " + bytesWritten);

        long bytesLeft = bytesWritten;
        boolean allWritten = false;
        for (final TrackedLease<MemorySegment> bufferLease : leases) {
            final long byteSize = bufferLease.leasedObject().byteSize();

            if (!allWritten && bytesWritten > 0) {
                // same as ByteBuffer.flip()
                final long diff = bytesLeft - byteSize;
                if (diff < 0) {
                    // mem.segment bigger than bytes left.
                    // set limit to written amount.
                    final long limit = byteSize - Math.abs(diff);
                    //rv.add(new TrackedMemorySegmentLease(bufferLease, new AtomicLong(0L), new AtomicLong(limit)));
                    bufferLease.position(0L);
                    bufferLease.limit(limit);
                    rv.add(bufferLease);
                }
                else {
                    //else: full mem.segment used, no need to set limit.
                    //rv.add(new TrackedMemorySegmentLease(bufferLease));
                    rv.add(bufferLease);
                }
            }

            bytesLeft -= byteSize;

            if (bytesLeft <= 0) {
                allWritten = true;
            }
        }

        return new WrittenResult(bytesWritten, rv);
    }

    @Override
    public TransportInfo getTransportInfo() {
        return transportInfo;
    }

    @Override
    public void close() throws IOException {
        tlsChannel.close();
    }

    @Override
    public SocketChannel socketChannel() {
        return socketChannel;
    }
}
