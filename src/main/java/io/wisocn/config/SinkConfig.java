package io.wisocn.config;

import io.wisocn.sink.Sink;

public interface SinkConfig {

    Sink<?> build();

}
