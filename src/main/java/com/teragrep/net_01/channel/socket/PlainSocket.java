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

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

final class PlainSocket implements Socket {

    private final SocketChannel socketChannel;
    private final TransportInfo transportInfo;

    PlainSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        EncryptionInfo encryptionInfo = new EncryptionInfoStub();
        this.transportInfo = new TransportInfoImpl(socketChannel, encryptionInfo);
    }

    @Override
    public ReadResult read(final TrackedLease<MemorySegment>[] dsts) throws IOException {
        final int size = dsts.length;
        final TrackedLease<MemorySegment>[] rv = new TrackedMemorySegmentLease[size];
        final ByteBuffer[] byteBuffers = new ByteBuffer[size];

        for (int i = 0; i < size; i++) {
            byteBuffers[i] = dsts[i].leasedObject().asByteBuffer();
        }

        final long readBytes = socketChannel.read(byteBuffers);

        for (int i = 0; i < size; i++) {
            final TrackedLease<MemorySegment> lease = dsts[i];
            final ByteBuffer byteBuffer = byteBuffers[i];

            lease.position(byteBuffer.position());
            lease.limit(byteBuffer.limit());

            lease.flip();

            rv[i] = lease;
        }

        return new ReadResult(readBytes, rv);
    }

    @Override
    public WrittenResult write(final TrackedLease<MemorySegment>[] dsts) throws IOException {
        final int size = dsts.length;
        final ByteBuffer[] buffersToWrite = new ByteBuffer[size];
        final TrackedLease<MemorySegment>[] rv = new TrackedMemorySegmentLease[size];

        for (int i = 0; i < size; i++) {
            buffersToWrite[i] = dsts[i].leasedObject().asByteBuffer();
        }

        final long bytesWritten = socketChannel.write(buffersToWrite);

        for (int i = 0; i < size; i++) {
            final TrackedLease<MemorySegment> lease = dsts[i];
            final ByteBuffer byteBuffer = buffersToWrite[i];

            lease.position(byteBuffer.position());
            lease.limit(byteBuffer.limit());

            lease.flip();

            rv[i] = lease;
        }

        return new WrittenResult(bytesWritten, rv);
    }

    @Override
    public TransportInfo getTransportInfo() {
        return transportInfo;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    @Override
    public SocketChannel socketChannel() {
        return socketChannel;
    }
}
