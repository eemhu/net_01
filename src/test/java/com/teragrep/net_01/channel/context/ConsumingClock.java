package com.teragrep.net_01.channel.context;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ConsumingClock implements Clock {
    private final EstablishedContext ctx;
    private final Consumer<List<Byte>> messageConsumer;

    public ConsumingClock(final EstablishedContext ctx, final Consumer<List<Byte>> messageConsumer) {
        this.ctx = ctx;
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void advance(TrackedMemorySegmentLease lease) {
        final List<Byte> bytes = new ArrayList<>();
        while (lease.hasNext()) {
            bytes.add(lease.next());
        }

        messageConsumer.accept(bytes);
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
