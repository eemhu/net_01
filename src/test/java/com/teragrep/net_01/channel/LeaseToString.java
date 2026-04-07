package com.teragrep.net_01.channel;

import com.teragrep.buf_01.buffer.lease.MemorySegmentLease;
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

public final class LeaseToString {

    private final String origin;
    private final OpeningPool pool;
    public LeaseToString(final String origin, OpeningPool pool) {
        this.origin = origin;
        this.pool = pool;
    }

    public Writeable toWriteable() {
        byte[] bytes = origin.getBytes(StandardCharsets.UTF_8);
        List<OpenableLease<MemorySegment>> leases = new LeaseMultiGet(pool).get(bytes.length);
        List<TrackedMemorySegmentLease> trackedLeases = new ArrayList<>(leases.size());
        for (OpenableLease<MemorySegment> lease : leases) {
            trackedLeases.add(new TrackedMemorySegmentLease(lease));
        }

        return new StringWriteable(trackedLeases);
    }
}
