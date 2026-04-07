package com.teragrep.net_01.channel.socket;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.util.List;

public final class ReadResult implements IOResult<TrackedMemorySegmentLease> {
    private final long bytesRead;
    private final List<TrackedMemorySegmentLease> leases;

    public ReadResult(final long bytesRead, final List<TrackedMemorySegmentLease> leases) {
        this.bytesRead = bytesRead;
        this.leases = leases;
    }

    @Override
    public long bytes() {
        return bytesRead;
    }

    @Override
    public List<TrackedMemorySegmentLease> leases() {
        return leases;
    }
}
