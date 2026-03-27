package com.teragrep.net_01.channel.context;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ClockFake implements Clock {
    private final EstablishedContext ctx;
    private final Consumer<List<Byte>> messageConsumer;

    public ClockFake(final EstablishedContext ctx, final Consumer<List<Byte>> messageConsumer) {
        this.ctx = ctx;
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void advance(TrackedMemorySegmentLease lease) {
        System.out.println("advancing clock @" + Thread.currentThread().getName());
        System.out.println("pos=" + lease.position());

        final List<Byte> bytes = new ArrayList<>();
        while (lease.hasNext()) {
            final byte b = lease.next();
            if (b != 0){
                bytes.add(b);
            }
        }

        messageConsumer.accept(bytes);
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
