package com.teragrep.net_01.channel.context;

import java.util.function.Consumer;

public final class SendingClockFactory implements ClockFactory {
    private final Consumer<String> consumer;

    public SendingClockFactory(final Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Clock create(final EstablishedContext establishedContext) {
        return new SendingClock(establishedContext, consumer);
    }
}
