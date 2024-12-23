package com.paymentology.queue;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * {@link BlockingQueue} implementation that features batch flushing of inserted elements either manually via {@link
 * #flush()} or automatically when {@code flushEvery} constructor parameter is set to a positive value.
 *
 * @param <E> queue element type
 */
public final class BatchFlushQueue<E> implements BlockingQueue<E>, Closeable {

    private static final Logger log = LoggerFactory.getLogger(BatchFlushQueue.class);
    String name;
    private final int batchSize;
    private final int maxFlushIterations;
    private final int flushEvery;
    private final Consumer<Collection<E>> flushConsumer;
    private final Supplier<ExecutorService> executorSupplier;
    private final BlockingQueue<E> queue;
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicLong flushCounter = new AtomicLong();
    private final AtomicLong flushErrors = new AtomicLong();
    private final AtomicLong consumerInvocations = new AtomicLong();
    private final AtomicBoolean flushInProgress = new AtomicBoolean();
    private final ScheduledFuture<?> periodicFlushTicker;

    /**
     * Creates new instance.
     *
     * @param name                      queue name, used for {@link #toString()}
     * @param capacity                  Queue max capacity limit.
     * @param batchSize                 number of elements taken from the queue in each flush iteration
     * @param maxFlushIterations        do at most given number of batch operations in each flush operation
     * @param flushEvery                trigger automatic flush every specified amount of additions, set to value {@code
     *                                  lower than 1} to disable this behaviour.
     * @param periodicFlushInterval     execute periodic flush every specified amount of time if {@code
     *                                  scheduledExecutorServiceSupplier} is defined
     * @param queueCreator              actual queue operation creator, function that takes capacity and creates the
     *                                  queue.
     * @param batchConsumer             consumer that is called on batch flushes; consumer must be thread-safe; element
     *                                  batch is re-inserted in the queue if consumer throws.
     * @param executorSupplier          Executor service supplier that is used to run automatic flushes when {@link
     *                                  #add(Object)}, {@link #addAll(Collection)}, {@link #put(Object)}, {@link
     *                                  #offer(Object)} methods managed to store element into delegate queue, thus
     *                                  making these methods non-blocking even though {@code batchConsumer} might be a
     *                                  blocking method
     * @param scheduledExecutorSupplier scheduled executor supplier for periodic flushes; <B>NOTE:</B> make sure that
     *                                  you invoke {@link #close()} when this queue is no longer needed!
     */
    public BatchFlushQueue(String name,
                              int capacity,
                              int batchSize,
                              int maxFlushIterations,
                              int flushEvery,
                              Duration periodicFlushInterval,
                              Consumer<Collection<E>> batchConsumer,
                              Function<Integer, ? extends BlockingQueue<E>> queueCreator,
                              Supplier<ExecutorService> executorSupplier,
                              Supplier<ScheduledExecutorService> scheduledExecutorSupplier) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Queue capacity must be >= 1.");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Queue batch size must be >= 1.");
        }
        if (maxFlushIterations < 1) {
            throw new IllegalArgumentException("Queue maxFlushIterations must be >= 1.");
        }

        this.batchSize = batchSize;
        this.maxFlushIterations = maxFlushIterations;
        this.flushEvery = flushEvery;
        this.flushConsumer = batchConsumer;
        this.queue = createQueue(queueCreator, capacity);
        this.executorSupplier = executorSupplier;
        this.periodicFlushTicker = createPeriodicFlushTicker(
                    scheduledExecutorSupplier,
                    periodicFlushInterval);
        this.name = name;
    }

    private ScheduledFuture<?> createPeriodicFlushTicker(Supplier<ScheduledExecutorService> scheduledExecutorSupplier,
                                                         Duration interval) {
        long intervalMillis = interval.toMillis();
        if (intervalMillis < 1) {
            return null;
        }

        return Optional.ofNullable(scheduledExecutorSupplier)
                .map(Supplier::get)
                .map(it -> it.scheduleAtFixedRate(this::invokeFlushWithOptionalExecutor,
                        intervalMillis, intervalMillis, TimeUnit.MILLISECONDS))
                .orElse(null);
    }

    private <E> BlockingQueue<E> createQueue(Function<Integer, ? extends BlockingQueue<E>> queueCreator,
                                             int capacity) {
        if (queueCreator == null) {
            queueCreator = LinkedBlockingQueue::new;
        }

        log.debug("creating blocking queue with capacity of {} with: {}", capacity, queueCreator);

        return Objects.requireNonNull(
                queueCreator.apply(capacity),
                "Queue creator " + queueCreator + "created null queue.");
    }

    @Override
    public boolean add(E e) {
        return conditionalFlush(queue.add(e), 1);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return conditionalFlush(queue.addAll(c), c.size());
    }

    @Override
    public boolean offer(E e) {
        return conditionalFlush(queue.offer(e), 1);
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public void put(E e) throws InterruptedException {
        queue.put(e);
        conditionalFlush(true, 1);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return conditionalFlush(queue.offer(e, timeout, unit), 1);
    }

    @Override
    public E take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public void close() {
        if (periodicFlushTicker != null && !periodicFlushTicker.isDone()) {
            log.debug("{} closing periodic ticker: {}", this, periodicFlushTicker);
            periodicFlushTicker.cancel(true);
        }
    }

    @Override
    public String toString() {
        return "size=" + size() +
                ", flush #" + flushes() +
                ", invocations=" + consumerInvocations() +
                ", errors=" + errors() +
                "]";
    }

    /**
     * Returns number of successful {@link #flush()} invocations.
     *
     * @return number of flush invcations.
     */
    public long flushes() {
        return flushCounter.get();
    }

    /**
     * Returns number of consumer invocations that completed with exception.
     *
     * @return number of consumer exceptions.
     */
    public long errors() {
        return flushErrors.get();
    }

    /**
     * Returns number of batch consumer invocations.
     *
     * @return number of batch consumer invocations
     */
    public long consumerInvocations() {
        return consumerInvocations.get();
    }

    /**
     * Performs batch batch flush of items in queue, ensuring that only one flush operation can be active at the same
     * time.
     *
     * @return number of items that were flushed from queue
     */
    public long flush() {
        if (flushInProgress.compareAndSet(false, true)) {
            try {
                return doFlush();
            } finally {
                flushInProgress.compareAndSet(true, false);
            }
        } else {
            log.debug("{} another flush is in progress, skipping.", this);
        }

        return 0;
    }

    /**
     * Performs {@link #flush()} and blocks until queue is empty or max up to 5 minutes.
     *
     * @return number of elements that have been flushed out
     * @throws IllegalArgumentException if timeout is < 1
     * @throws TimeoutException         in case of a timeout
     * @throws InterruptedException     if thread that waits gets interrupted.
     * @see #flushUntilEmpty(long, TimeUnit)
     */
    public long flushUntilEmpty() {
        return flushUntilEmpty(5, TimeUnit.MINUTES);
    }

    /**
     * Performs {@link #flush()} and blocks until queue is empty or given timeout is exceeded.
     *
     * @param timeout timeout amount
     * @param unit    timeout unit
     * @return number of elements that have been flushed out
     * @throws IllegalArgumentException if timeout is < 1
     * @throws TimeoutException         in case of a timeout
     * @throws InterruptedException     if thread that waits gets interrupted.
     */
    public long flushUntilEmpty(long timeout, TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Timeout can't be < 1.");
        }

        long stopTime = System.nanoTime() + unit.toNanos(timeout);

        return LongStream.generate(() -> 1)
                .takeWhile(it -> !isEmpty())
                .peek(it -> throwIfTimeoutExceeded(timeout, unit, stopTime))
                .map(it -> flush())
                .peek(this::shortSleep)
                .sum();
    }

    private void shortSleep(long it) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void throwIfTimeoutExceeded(long timeout, TimeUnit unit, long stopTime) {
        if (System.nanoTime() > stopTime) {
            throw new RuntimeException("Queue has not been emptied in " + timeout + " " + unit);
        }
    }

    private boolean conditionalFlush(boolean result, int numElements) {
        if (result && flushEvery > 0) {
            // this is horribly inefficient, but it simulates single element addition in the queue
            // so that batch consumer is executed in the same way if these elements would be added
            // separately
            for (int i = 0; i < numElements; i++) {
                maybeInvokeAutomaticFlush();
            }
        }
        return result;
    }

    private void maybeInvokeAutomaticFlush() {
        if (counter.incrementAndGet() % flushEvery == 0) {
            invokeFlushWithOptionalExecutor();
        }
    }

    private void invokeFlushWithOptionalExecutor() {
        // if there's a executor service available, run flush() in it, thus making flush() non-blocking,
        // otherwise run it in the invoking thread which might make it blocking if flush consumer does some
        // sort of a blocking I/O or time consuming computation.
        ExecutorService executor = executor();
        if (executor != null) {
            executor.submit(this::flush);
        } else {
            flush();
        }
    }

    private ExecutorService executor() {
        return (executorSupplier != null) ? executorSupplier.get() : null;
    }

    private long doFlush() {
        StopWatch sw = new StopWatch();
        sw.start();
        flushCounter.incrementAndGet();

        log.trace("{} starting full flush", this);

        int result = IntStream.range(0, maxFlushIterations)
                .mapToObj(idx -> createFlushBatch())
                .takeWhile(it -> !it.isEmpty())
                .mapToInt(this::tryBatchFlush)
                .sum();
        sw.stop();
        log.debug("{} full flush of {} elements done in {}", this, result, sw);
        return result;
    }

    private List<E> createFlushBatch() {
        if (queue.isEmpty()) {
            return List.of();
        }
        int maxSize = Math.max(batchSize, queue.size());
        ArrayList<E> result = new ArrayList<E>(maxSize);
        int numDrained = queue.drainTo(result, batchSize);
        log.trace("{} drained {} elements from the queue", this, numDrained);
        return result;
    }

    private int tryBatchFlush(Collection<E> elements) {
        try {
            batchFlush(elements);
            return elements.size();
        } catch (Throwable t) {
            flushErrors.incrementAndGet();
            log.error("{} error flushing {} elements from the queue.", this, elements.size(), t);

            // re-add failed elements back to the queue
            try {
                queue.addAll(elements);
            } catch (RuntimeException e) {
                log.error("{} error re-adding {} elements back to the queue", this, elements.size(), e);
            }

            return 0;
        }
    }

    private void batchFlush(Collection<E> elements) {
        StopWatch sw = new StopWatch();
        log.debug("{} flushing batch of {} elements with: {}", this, elements.size(), flushConsumer);
        log.trace("  {} elements to be flushed: {}", this, elements);
        sw.start();
        consumerInvocations.incrementAndGet();
        flushConsumer.accept(elements);
        sw.stop();
        log.debug("{} successfully flushed batch of {} element(s) in {}", this, elements.size(), sw);
    }
}

