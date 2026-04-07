package com.teragrep.net_01.channel.context;

import com.teragrep.buf_01.buffer.pool.OpeningPool;
import com.teragrep.net_01.channel.StringToLease;
import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;
import com.teragrep.net_01.channel.buffer.writable.Writeable;

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
            final char c = (char) bufferLease.next().byteValue();
            stringBuilder.append(c);
        }

        final String str = stringBuilder.toString();

        final Writeable w = new StringToLease(str, pool).toWriteable();
        ctx.egress().accept(w);

        consumer.accept(str);
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
