package com.paymentology;


import com.paymentology.config.NoopSinkConfig;
import com.paymentology.handler.SinkHandlerService;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

@Singleton
public class StartupVerifier {

    private static final Logger log = LoggerFactory.getLogger(StartupVerifier.class);

    private final Set<Class<?>> clazz = Set.of(SinkHandlerService.class);

    @EventListener
    void onStartup(StartupEvent event) {
        log.info("Startup verifier triggered");
        var ctx = event.getSource();
        clazz.forEach(it-> fetchFromCtx(it, ctx));
    }

    private <T> T fetchFromCtx(Class<T> clazz, BeanContext ctx) {
        var bean = Objects.requireNonNull(ctx.getBean(clazz),
                "Can't fetch bean of type " + clazz.getName() + " from from application's context: " + ctx);
        return bean;
    }
}
