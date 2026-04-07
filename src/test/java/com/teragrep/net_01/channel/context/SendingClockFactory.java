package com.teragrep.net_01.channel.context;

import com.teragrep.buf_01.buffer.pool.OpeningPool;

import java.util.function.Consumer;

public final class SendingClockFactory implements ClockFactory {
    private final Consumer<String> consumer;
    private final OpeningPool pool;

    public SendingClockFactory(final Consumer<String> consumer, final OpeningPool pool) {
        this.consumer = consumer;
        this.pool = pool;
    }

    @Override
    public Clock create(final EstablishedContext establishedContext) {
        return new SendingClock(establishedContext, consumer, pool);
    }
}
