package io.github.ololx.samples.concurrent.bitset;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Thread)
@Warmup(
        iterations = 5,
        time = 1,
        timeUnit = TimeUnit.SECONDS
)
@Measurement(
        iterations = 5,
        time = 1,
        timeUnit = TimeUnit.SECONDS
)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MILLISECONDS)
public class ConcurrentBitSetBenchmark {

    private ConcurrentBitSet concurrentBitSet;

    @Param({"javaNative", "customOnUnsafe", "castomOnSegmentaryReadWriteLocks", "castomOnOneReadWriteLocks"})
    private String type;

    private final static int BITS = 100_000;

    private final static int BITSET_CAPACITY = BITS + 1;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ConcurrentBitSetBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() {
        switch (type) {
            case "javaNative" -> concurrentBitSet = new ConcurrentBitSetOnFullSynchronization(BITSET_CAPACITY);
            case "customOnUnsafe" -> concurrentBitSet = new NonBlockingConcurrentBitset(BITSET_CAPACITY);
            case "castomOnSegmentaryReadWriteLocks" -> concurrentBitSet = new ConcurrentBitSetOnSegmentsLocks(
                    BITSET_CAPACITY);
            case "castomOnOneReadWriteLocks" -> concurrentBitSet = new ConcurrentBitSetOnGeneralLock(BITSET_CAPACITY);
        }
    }

    @Benchmark
    public void updateAndGetOncePerBit(Blackhole blackhole) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.set(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.flip(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.clear(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> blackhole.consume(concurrentBitSet.get(j)))));

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    @Benchmark
    public void updateOnceAndReadManyTimesPerBit(Blackhole blackhole) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.set(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.flip(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> concurrentBitSet.clear(j))));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> {
                    blackhole.consume(concurrentBitSet.get(j));
                    blackhole.consume(concurrentBitSet.get(j));
                    blackhole.consume(concurrentBitSet.get(j));
                    blackhole.consume(concurrentBitSet.get(j));
                    blackhole.consume(concurrentBitSet.get(j));
                    blackhole.consume(concurrentBitSet.get(j));
                })));

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    @Benchmark
    public void updateManyTimesAndReadOncePerBit(Blackhole blackhole) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> {
                    concurrentBitSet.set(j);
                    concurrentBitSet.set(j);
                    concurrentBitSet.set(j);
                    concurrentBitSet.set(j);
                    concurrentBitSet.set(j);
                })));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> {
                    concurrentBitSet.flip(j);
                    concurrentBitSet.flip(j);
                    concurrentBitSet.flip(j);
                    concurrentBitSet.flip(j);
                    concurrentBitSet.flip(j);
                    concurrentBitSet.flip(j);
                })));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> {
                    concurrentBitSet.clear(j);
                    concurrentBitSet.clear(j);
                    concurrentBitSet.clear(j);
                    concurrentBitSet.clear(j);
                    concurrentBitSet.clear(j);
                    concurrentBitSet.clear(j);
                })));
        IntStream.range(0, BITS)
                .boxed()
                .forEach(j -> futures.add(CompletableFuture.runAsync(() -> blackhole.consume(concurrentBitSet.get(j)))));

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
}
