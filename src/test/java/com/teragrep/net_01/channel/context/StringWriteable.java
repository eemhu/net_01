package com.teragrep.net_01.channel.context;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.net_01.channel.buffer.writable.Writeable;

import java.util.List;

public final class StringWriteable implements Writeable {
    private final List<TrackedMemorySegmentLease> buffers;

    public StringWriteable(final List<TrackedMemorySegmentLease> buffers) {
        this.buffers = buffers;
    }

    @Override
    public void close() {
        for (TrackedMemorySegmentLease buf : buffers) {
            try {
                buf.close();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<TrackedMemorySegmentLease> memorySegmentLeases() {
        return buffers;
    }

    @Override
    public boolean hasRemaining() {
        boolean rv = false;
        for (final TrackedMemorySegmentLease buffer : buffers) {
            if (buffer.hasNext()) {
                rv = true;
                break;
            }
        }
        return rv;
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
