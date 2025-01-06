package com.paymentology.controller;


import com.paymentology.handler.SinkHandlerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller("/v1/sink")
public class SinkController {

    private static final Logger log = LoggerFactory.getLogger(SinkController.class);

    private final SinkHandlerService sinkHandlerService;

    public SinkController(SinkHandlerService sinkHandlerService) {
        this.sinkHandlerService = sinkHandlerService;
    }

    @Post("/submit/{sinkName}")
    public Mono<HttpResponse<String>> submit(@PathVariable("sinkName") String sinkName,
                                             @Body List<Map<String, Object>> records) {
        return Mono.fromRunnable(() -> sinkHandlerService.submit(sinkName, records))
                .thenReturn(HttpResponse.ok());
    }
}
