package io.github.ololx.samples.concurrent.bitset;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Warmup(
        iterations = 5,
        time = 100,
        timeUnit = TimeUnit.MILLISECONDS
)
@Measurement(
        iterations = 5,
        time = 100,
        timeUnit = TimeUnit.MILLISECONDS
)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ConcurrentBitSetBenchmark {

    private static final int AVAILABLE_CPU = Runtime.getRuntime()
            .availableProcessors();

    private static final String FULL_SYNCHRONIZATION = "javaNativeWithSynchronizationByThis";

    private static final String ONE_READ_WRITE_LOCK = "javaNativeWithOneReadWriteLock";

    private static final String MANY_READ_WRITE_LOCKS = "javaNativeWithManyReadWriteLocksBySegments";

    private static final String NON_BLOCKING = "NonBlockingConcurrentBitset";

    private ConcurrentBitSet concurrentBitSet;

    @Param(
            {
                    FULL_SYNCHRONIZATION,
                    ONE_READ_WRITE_LOCK,
                    MANY_READ_WRITE_LOCKS,
                    NON_BLOCKING
            }
    )
    private String typeOfBitSetRealization;

    private ExecutorService executor;

    @Param({"10", "100", "1000"})
    private int sizeOfBitSet;

    @Param({"1", "10"})
    private int countOfSetters;

    @Param({"1", "10"})
    private int countOfGetters;

    public ConcurrentBitSetBenchmark() {}

    public static void main(String[] args) throws RunnerException {
        var options = new OptionsBuilder()
                .include(ConcurrentBitSetBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() {
        switch (typeOfBitSetRealization) {
            case FULL_SYNCHRONIZATION -> concurrentBitSet = new ConcurrentBitSetWithFullSynchronization(sizeOfBitSet);
            case ONE_READ_WRITE_LOCK -> concurrentBitSet = new ConcurrentBitSetWithGeneralRWLock(sizeOfBitSet);
            case MANY_READ_WRITE_LOCKS -> concurrentBitSet = new ConcurrentBitSetWithSegmentsRWLocks(sizeOfBitSet);
            case NON_BLOCKING -> concurrentBitSet = new NonBlockingConcurrentBitset(sizeOfBitSet);
        }

        executor = Executors.newWorkStealingPool(AVAILABLE_CPU);
    }

    @TearDown
    public void tearDown() {
        executor.shutdown();
    }

    @Benchmark
    public void get_set_benchmark(Blackhole blackhole) {
        var tasksCount = sizeOfBitSet * (countOfGetters + countOfSetters);
        var bitsetInvocations = new ArrayList<CompletableFuture<Void>>(tasksCount);

        for (int setBitOpNumber = 0; setBitOpNumber < countOfGetters; setBitOpNumber++) {
            bitsetInvocations.add(CompletableFuture.runAsync(() -> {
                for (int bitNumber = 0; bitNumber < sizeOfBitSet; bitNumber++) {
                    blackhole.consume(concurrentBitSet.get(bitNumber));
                }

            }, executor));
        }

        for (int getBitOpNumber = 0; getBitOpNumber < countOfSetters; getBitOpNumber++) {
            bitsetInvocations.add(CompletableFuture.runAsync(() -> {
                for (int bitNumber = 0; bitNumber < sizeOfBitSet; bitNumber++) {
                    concurrentBitSet.set(bitNumber);
                }

            }, executor));
        }

        CompletableFuture.allOf(bitsetInvocations.toArray(CompletableFuture[]::new))
                .join();
    }
}
