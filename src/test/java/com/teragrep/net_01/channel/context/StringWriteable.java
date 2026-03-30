package com.teragrep.net_01.channel.context;

import com.teragrep.net_01.channel.buffer.writable.Writeable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class StringWriteable implements Writeable {
    private final ByteBuffer[] buffers;

    public StringWriteable(final String str) {
        this(new ByteBuffer[]{ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))});
    }

    public StringWriteable(final ByteBuffer[] buffers) {
        this.buffers = buffers;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public ByteBuffer[] buffers() {
        return buffers;
    }

    @Override
    public boolean hasRemaining() {
        boolean rv = false;
        for (final ByteBuffer buffer : buffers) {
            if (buffer.hasRemaining()) {
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
