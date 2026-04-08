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
package com.teragrep.net_01.channel.buffer;

import com.teragrep.buf_01.buffer.lease.Lease;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public class TrackedMemorySegmentLease implements Lease<MemorySegment>, Iterator<Byte> {

    private final Lease<MemorySegment> origin;
    private final AtomicLong currentOffset;
    private final AtomicLong limit;

    public TrackedMemorySegmentLease(final Lease<MemorySegment> origin) {
        this(origin, new AtomicLong(0L));
    }

    public TrackedMemorySegmentLease(final Lease<MemorySegment> origin, final AtomicLong currentOffset) {
        this(origin, currentOffset, new AtomicLong(-1L));
    }

    public TrackedMemorySegmentLease(
            final Lease<MemorySegment> origin,
            final AtomicLong currentOffset,
            final AtomicLong limit
    ) {
        this.origin = origin;
        this.currentOffset = currentOffset;
        this.limit = limit;
    }

    @Override
    public long id() {
        return origin.id();
    }

    @Override
    public long refs() {
        return origin.refs();
    }

    @Override
    public MemorySegment leasedObject() {
        return origin.leasedObject();
    }

    @Override
    public boolean hasZeroRefs() {
        return origin.hasZeroRefs();
    }

    @Override
    public Lease<MemorySegment> sliceAt(final long offset) {
        return origin.sliceAt(offset);
    }

    @Override
    public boolean isStub() {
        return origin.isStub();
    }

    @Override
    public void close() throws Exception {
        origin.close();
    }

    @Override
    public boolean hasNext() {
        final boolean rv;
        if (limit.get() == -1) {
            // limit not set, ignore
            rv = currentOffset.get() < origin.leasedObject().byteSize();
        }
        else {
            rv = currentOffset.get() < Math.min(limit.get(), origin.leasedObject().byteSize());
        }
        return rv;
    }

    @Override
    public Byte next() {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException("Reached end of segment or limit, cannot provide next byte");
        }
        final long nextIndex = currentOffset.getAndIncrement();

        return origin.leasedObject().get(ValueLayout.JAVA_BYTE, nextIndex);
    }

    public void write(final byte b) {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException("Reached end of segment or limit, cannot write to next byte");
        }

        final long nextIndex = currentOffset.getAndIncrement();

        origin.leasedObject().set(ValueLayout.JAVA_BYTE, nextIndex, b);
    }

    public long currentPosition() {
        return currentOffset.get();
    }

    public void position(final long newPosition) {
        currentOffset.set(newPosition);
    }

    public long currentLimit() {
        return limit.get();
    }

    public long limit(final long newLimit) {
        if (newLimit < 0 || newLimit > leasedObject().byteSize()) {
            throw new IndexOutOfBoundsException("Out of bounds");
        }

        limit.set(newLimit);
        return limit.get();
    }
}
