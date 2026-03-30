package com.teragrep.net_01.channel.context;

import java.util.List;
import java.util.function.Consumer;

public final class ConsumingClockFactory implements ClockFactory {

    private final Consumer<List<Byte>> messageConsumer;

    public ConsumingClockFactory(Consumer<List<Byte>> messageConsumer){
        this.messageConsumer = messageConsumer;
    }

    @Override
    public Clock create(final EstablishedContext establishedContext) {
        return new ConsumingClock(establishedContext, messageConsumer);
    }
}
