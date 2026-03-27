package com.teragrep.net_01.channel.context;

import java.util.List;
import java.util.function.Consumer;

public final class ClockFactoryFake implements ClockFactory{

    private final Consumer<List<Byte>> messageConsumer;

    public ClockFactoryFake(Consumer<List<Byte>> messageConsumer){
        this.messageConsumer = messageConsumer;
    }

    @Override
    public Clock create(final EstablishedContext establishedContext) {
        return new ClockFake(establishedContext, messageConsumer);
    }
}
