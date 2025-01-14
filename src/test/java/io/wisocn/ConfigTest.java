package io.wisocn;

import io.wisocn.handler.SinkHandlerService;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

public class ConfigTest {

    @Test
    public void testApplicationConfigBinding() {
        try (ApplicationContext context = ApplicationContext.run()) {
            SinkHandlerService sinkHandlerService = context.getBean(SinkHandlerService.class);
        }
    }

}
