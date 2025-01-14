package io.wisocn.sink.impl;

import io.wisocn.config.NoopSinkConfig;
import io.wisocn.config.QueueConfig;
import io.wisocn.queue.BatchFlushQueue;
import io.wisocn.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NoopSink<E> implements Sink<E> {

    private static final Logger log = LoggerFactory.getLogger(NoopSink.class);
    private final Queue<Map<String, Object>> queue;
    private final NoopSinkConfig noopSinkConfig;

    public NoopSink(NoopSinkConfig config){
        this.queue = constructQueue(config);
        this.noopSinkConfig = config;
    }

    private Queue<Map<String, Object>> constructQueue(NoopSinkConfig noopSinkConfig) {
        Consumer<Collection<Map<String, Object>>> batchConsumer = batch -> log.info("Flushing batch of: " + batch);
        Supplier<ExecutorService> supplier = Executors::newCachedThreadPool;
        Supplier<ScheduledExecutorService> scheduled = () -> Executors.newScheduledThreadPool(10);
        QueueConfig queueConfig = noopSinkConfig.getQueue();
        return new BatchFlushQueue<>(
                "batch-flush-queue".concat(noopSinkConfig.getName()),
                queueConfig.getCapacity(),
                queueConfig.getBatchSize(),
                queueConfig.getMaxFlushIterations(),
                queueConfig.getFlushEvery(),
                queueConfig.getFlush(),
                batchConsumer,
                LinkedBlockingQueue::new,
                supplier,
                scheduled
        );
    }

    @Override
    public String getType() {
        return "noop";
    }

    @Override
    public String getName() {
        return this.noopSinkConfig.getName();
    }

    @Override
    public void submit(List<Map<String, Object>> record) {
        this.queue.addAll(record);
        log.info("inserted {} items into queue", record.size());
    }

    @Override
    public void forceFlush() {

    }
}
