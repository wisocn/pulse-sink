package com.paymentology.config;

import com.paymentology.queue.BatchFlushQueue;
import com.paymentology.sink.Sink;
import com.paymentology.sink.impl.NoopSink;
import com.paymentology.sink.impl.PostgresSink;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@EachProperty("sinks.noop")
public class NoopSinkConfig implements SinkConfig{

    private static final Logger log = LoggerFactory.getLogger(NoopSinkConfig.class);

    private final String name;
    private boolean enabled;
    private QueueConfig queue;

    public NoopSinkConfig(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public QueueConfig getQueue() {
        return queue;
    }

    public void setQueue(QueueConfig queue) {
        this.queue = queue;
    }

    @Override
    public Sink<?> build() {
        var sink = new NoopSink<>(this);
        log.info("Constructed new sink {} of type {} with queue {}", sink.getName(), sink.getType(), this.queue);
        return sink;
    }




}
