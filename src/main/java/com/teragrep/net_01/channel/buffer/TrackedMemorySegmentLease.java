package com.teragrep.net_01.channel.buffer;

import com.teragrep.buf_01.buffer.lease.Lease;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

public class TrackedMemorySegmentLease implements Lease<MemorySegment>, Iterator<Byte> {
    private final Lease<MemorySegment> origin;
    private final AtomicLong currentOffset;

    public TrackedMemorySegmentLease(final Lease<MemorySegment> origin) {
        this(origin, new AtomicLong(0L));
    }

    public TrackedMemorySegmentLease(final Lease<MemorySegment> origin, final AtomicLong currentOffset) {
        this.origin = origin;
        this.currentOffset = currentOffset;
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
        return currentOffset.get() < origin.leasedObject().byteSize();
    }

    @Override
    public Byte next() {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException("Reached end of segment, cannot provide next byte");
        }
        final long nextIndex = currentOffset.getAndIncrement();

        return origin.leasedObject().get(ValueLayout.JAVA_BYTE, nextIndex);
    }

    public long position() {
        return currentOffset.get();
    }
}
