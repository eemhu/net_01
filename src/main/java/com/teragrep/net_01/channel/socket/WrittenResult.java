package com.teragrep.net_01.channel.socket;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.util.List;

public class WrittenResult implements IOResult<TrackedMemorySegmentLease> {
    private final long bytesWritten;
    private final List<TrackedMemorySegmentLease> leases;

    public WrittenResult(final long bytesWritten, final List<TrackedMemorySegmentLease> leases) {
        this.bytesWritten = bytesWritten;
        this.leases = leases;
    }

    @Override
    public long bytes() {
        return bytesWritten;
    }

    @Override
    public List<TrackedMemorySegmentLease> leases() {
        return leases;
    }
}
