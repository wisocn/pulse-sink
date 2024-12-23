package com.paymentology.controller;


import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class SinkController {

    @Get("/test")
    public Mono<HttpResponse<String>> get() {
        return Mono.just(HttpResponse.ok("{}"));
    }

    @Post("/{sinkName}")
    public Mono<HttpResponse<String>> submit(@PathVariable String sinkName,
                                             @Body List<Map<String, Object>> records) {
        return Mono.just(HttpResponse.ok("{}"));
    }
}
