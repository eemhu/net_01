package com.teragrep.net_01.channel;

import com.teragrep.buf_01.buffer.lease.OpenableLease;
import com.teragrep.buf_01.buffer.pool.LeaseMultiGet;
import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.net_01.channel.buffer.writable.Writeable;
import com.teragrep.net_01.channel.context.StringWriteable;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class StringToLease {

    private final String origin;
    private final OpeningPool pool;
    public StringToLease(final String origin, OpeningPool pool) {
        this.origin = origin;
        this.pool = pool;
    }

    public Writeable toWriteable() {
        final byte[] bytes = origin.getBytes(StandardCharsets.UTF_8);
        final List<OpenableLease<MemorySegment>> leases = new LeaseMultiGet(pool).get(bytes.length);
        final List<TrackedMemorySegmentLease> trackedLeases = new ArrayList<>(leases.size());
        for (final OpenableLease<MemorySegment> lease : leases) {
            trackedLeases.add(new TrackedMemorySegmentLease(lease));
        }

        int i = 0;
        for (final TrackedMemorySegmentLease trackedLease : trackedLeases) {
            while (trackedLease.hasNext() && i < bytes.length) {
                trackedLease.write(bytes[i]);
                i++;
            }
        }

        return new StringWriteable(trackedLeases);
    }
}
