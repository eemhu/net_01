package com.teragrep.net_01.channel.context;

import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.net_01.channel.LeaseToString;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.util.function.Consumer;

public final class SendingClock implements Clock {
    private final EstablishedContext ctx;
    private final Consumer<String> consumer;
    private final OpeningPool pool;

    public SendingClock(final EstablishedContext ctx, final Consumer<String> consumer, final OpeningPool pool) {
        this.ctx = ctx;
        this.consumer = consumer;
        this.pool = pool;
    }

    @Override
    public void advance(final TrackedMemorySegmentLease bufferLease) {
        final StringBuilder stringBuilder = new StringBuilder();
        while (bufferLease.hasNext()) {
            stringBuilder.append((char)bufferLease.next().byteValue());
        }

        final String str = stringBuilder.toString();

        ctx.egress().accept(new LeaseToString(str, pool).toWriteable());

        consumer.accept(str);
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
