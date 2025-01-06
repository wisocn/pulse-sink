package com.paymentology;

import com.paymentology.handler.SinkHandlerService;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

    @Test
    public void testApplicationConfigBinding() {
        try (ApplicationContext context = ApplicationContext.run()) {
            SinkHandlerService sinkHandlerService = context.getBean(SinkHandlerService.class);
        }
    }

}
