package io.wisocn.config;

import io.wisocn.sink.Sink;
import io.wisocn.sink.impl.NoopSink;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
