package com.paymentology.handler;
import com.paymentology.config.SinkConfig;
import com.paymentology.sink.Sink;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class SinkHandlerService {

    private static final Logger log = LoggerFactory.getLogger(SinkHandlerService.class);

    private final Map<String, Sink<?>> sinks;

    public SinkHandlerService(Collection<SinkConfig> sinkConfigs) {
        // configure them out of sinks
        this.sinks = sinkConfigs.stream()
                .map(SinkConfig::build)
                .collect(Collectors.toMap(Sink::getName, Function.identity()));
    }

    public void submit(@PathVariable String sinkName,
                               @Body List<Map<String, Object>> records){

        var sink = Optional.ofNullable(sinks.get(sinkName))
                .orElseThrow(() -> {
                    var message = "Sink ".concat("not found.")
                            .concat("Configured: [")
                            .concat(String.join(",", this.sinks.keySet()))
                            .concat("]");
                    return new HttpStatusException(HttpStatus.NOT_FOUND, message);
                });
        sink.submit(records);
        log.info("Submited {} records into sink {}", records.size(), sink.getName());
    }





}
