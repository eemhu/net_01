package com.teragrep.net_01.channel.context;

import com.teragrep.net_01.channel.buffer.TrackedMemorySegmentLease;

import java.util.function.Consumer;

public final class SendingClock implements Clock {
    private final EstablishedContext ctx;
    private final Consumer<String> consumer;

    public SendingClock(final EstablishedContext ctx, final Consumer<String> consumer) {
        this.ctx = ctx;
        this.consumer = consumer;
    }

    @Override
    public void advance(final TrackedMemorySegmentLease bufferLease) {
        final StringBuilder stringBuilder = new StringBuilder();
        while (bufferLease.hasNext()) {
            stringBuilder.append((char)bufferLease.next().byteValue());
        }

        final String str = stringBuilder.toString();

        ctx.egress().accept(new StringWriteable(str));

        consumer.accept(str);
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
