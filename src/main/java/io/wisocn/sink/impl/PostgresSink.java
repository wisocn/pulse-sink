package io.wisocn.sink.impl;

import io.wisocn.config.PostgresSinkConfig;
import io.wisocn.config.QueueConfig;
import io.wisocn.queue.BatchFlushQueue;
import io.wisocn.sink.Sink;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PostgresSink<E> implements Sink<E> {
    private static final Logger log = LoggerFactory.getLogger(PostgresSink.class);
    private final Queue<Map<String, Object>> queue;
    private final PostgresSinkConfig postgresSinkConfig;

    private final DataSource dataSource;

    public PostgresSink(PostgresSinkConfig config){
        this.queue = constructQueue(config, this::flush);
        this.postgresSinkConfig = config;
        this.dataSource = construct(postgresSinkConfig);
    }

    private void flush(Collection<Map<String, Object>> entries) {
        if (entries.isEmpty()) {
            return;
        }

        Iterator<Map<String, Object>> iterator = entries.iterator();
        Map<String, Object> firstElement = iterator.next();
        String sql = getInsertStatement(firstElement);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
             for (Map<String, Object> entry : entries) {
                 int index = 1;
                 for (Object value : entry.values()) {
                     preparedStatement.setObject(index++, value);
                 }
                 preparedStatement.addBatch();
             }

             // Execute batch
            int[] result = preparedStatement.executeBatch();
            log.info("{} records inserted successfully!", result.length);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private String getInsertStatement(Map<String, Object> it) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        for (String key : it.keySet()) {
            if (!columns.isEmpty()) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(key);
            placeholders.append("?");
        }

        return "INSERT INTO " + this.postgresSinkConfig.getTable() + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    private Queue<Map<String, Object>> constructQueue(PostgresSinkConfig noopSinkConfig,
                                                      Consumer<Collection<Map<String, Object>>> batchConsumer) {
        Supplier<ExecutorService> supplier = Executors::newCachedThreadPool;
        Supplier<ScheduledExecutorService> scheduled = () -> Executors.newScheduledThreadPool(10);
        QueueConfig queueConfig = noopSinkConfig.getQueue();
        return new BatchFlushQueue<>(
                "batch-flush-queue".concat(noopSinkConfig.getName()),
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

    private DataSource construct(PostgresSinkConfig postgresSinkConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(generateJdbcUrl(postgresSinkConfig.getHost(),
                postgresSinkConfig.getPort(),
                postgresSinkConfig.getDatabase()));
        config.setDriverClassName("org.postgresql.Driver");
        config.setUsername(postgresSinkConfig.getUsername());
        config.setPassword(postgresSinkConfig.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    public static String generateJdbcUrl(String host, int port, String database) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be a valid number between 1 and 65535");
        }
        if (database == null || database.isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }


    @Override
    public void submit(List<Map<String, Object>> record) {
        this.queue.addAll(record);
        log.info("inserted {} items into queue. Current queue size {}", record.size(), this.queue.size());
    }



    @Override
    public void forceFlush() {

    }

    @Override
    public String getType() {
        return "postgres";
    }

    @Override
    public String getName() {
        return this.postgresSinkConfig.getName();
    }
}
