package com.paymentology.config;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.SerdeImport;

import java.time.Duration;

@SerdeImport(QueueConfig.class)
@Introspected
public class QueueConfig {

    String type;
    Integer capacity;
    Integer batchSize;
    Integer maxFlushIterations;
    Integer flushEvery;
    Duration flush = Duration.ofSeconds(5);

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getMaxFlushIterations() {
        return maxFlushIterations;
    }

    public void setMaxFlushIterations(Integer maxFlushIterations) {
        this.maxFlushIterations = maxFlushIterations;
    }

    public Integer getFlushEvery() {
        return flushEvery;
    }

    public void setFlushEvery(Integer flushEvery) {
        this.flushEvery = flushEvery;
    }

    public Duration getFlush() {
        return flush;
    }

    public void setFlush(Duration flush) {
        this.flush = flush;
    }

    @Override
    public String toString() {
        return "QueueConfig{" +
                "type='" + type + '\'' +
                ", capacity=" + capacity +
                ", batchSize=" + batchSize +
                ", maxFlushIterations=" + maxFlushIterations +
                ", flushEvery=" + flushEvery +
                ", flush=" + flush +
                '}';
    }
}
