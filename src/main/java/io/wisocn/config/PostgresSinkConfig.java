package io.wisocn.config;

import io.wisocn.queue.BatchFlushQueue;
import io.wisocn.sink.Sink;
import io.wisocn.sink.impl.PostgresSink;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;


@EachProperty("sinks.postgres")
public class PostgresSinkConfig implements SinkConfig {

    private static final Logger log = LoggerFactory.getLogger(PostgresSinkConfig.class);

    private final String name;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private String table;

    private QueueConfig queue;



    public PostgresSinkConfig(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public QueueConfig getQueue() {
        return queue;
    }

    public void setQueue(QueueConfig queue) {
        this.queue = queue;
    }

    @Override
    public Sink<?> build() {
        var sink = new PostgresSink<>(this);
        log.info("Constructed new sink {} of type {} with queue {}", sink.getName(), sink.getType(), this.queue);
        return sink;
    }

    private BatchFlushQueue<Map<String, Object>> constructQueue(PostgresSinkConfig postgresSinkConfig) {
        Consumer<Collection<Map<String, Object>>> batchConsumer = batch -> System.out.println("Processing batch: " + batch);
        Supplier<ExecutorService> supplier = Executors::newCachedThreadPool;
        Supplier<ScheduledExecutorService> scheduled = () -> Executors.newScheduledThreadPool(10);
        QueueConfig queueConfig = postgresSinkConfig.getQueue();
        return new BatchFlushQueue<>(
                "batch-flush-queue".concat(postgresSinkConfig.getName()),
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

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
