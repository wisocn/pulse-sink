package com.paymentology.config;

import com.paymentology.sink.Sink;

public interface SinkConfig {

    Sink<?> build();

}
