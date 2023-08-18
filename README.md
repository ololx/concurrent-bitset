# concurrent-bitset

Starting from Java version 1.0, the BitSet class has been available, representing an array of bits that can be set or cleared. It can be employed in various
scenarios where manipulation of bit-level information and flag handling is required, owing to its efficiency and compactness when dealing with substantial
volumes of bit-based data.
It sounds impressive, but there's a caveat: the native implementation of BitSet in Java is not thread-safe. The authors themselves caution about this
in comments within the class: **"A BitSet is not safe for multithreaded use without external synchronization."**

I tried to implement my thread-safe BitSet and want to show what came out of it. Important: the complete implementation will be included in the new version of 
my [moonshine project](https://github.com/ololx/moonshine), and you'll be able to check out the implementation there. For now, let's consider only 4 methods 
of the thread-safe BitSet.

```java
public interface ConcurrentBitSet {
    
    boolean get(int bitIndex);
    
    void set(int bitIndex);
    
    void clear(int bitIndex);
    
    void flip(int bitIndex);
}
```

Below, I will provide code examples of various ways to implement a thread-safe BitSet and their comparison using the following tests:
1. **thread safety correctness test**, which will demonstrate that a specific implementation is free from data races.
2. **benchmark**, which will showcase the performance of each implementation in terms of throughput.

The **thread safety correctness test** is written using the [**MultithreadedTC**](https://www.cs.umd.edu/projects/PL/multithreadedtc/overview.html) framework, while 
the **benchmark** is conducted using the [**Java Microbenchmark Harness (JMH) **](https://github.com/openjdk/jmh).

To begin, let's attempt the Thread Safety Correctness Test on the native Java implementation of BitSet and verify that it's indeed not thread-safe, 
and the test can identify this.

```java
public class ConcurrentBitSetMultithreadingTest extends MultithreadedTest {

    private BitSet nativeBitSet;

    @Override
    public void initialize() {
        this.nativeBitSet = new BitSet(10);
    }

    public void thread1() throws InterruptedException {
        this.nativeBitSet.set(1);
    }

    public void thread2() throws InterruptedException {
        this.nativeBitSet.set(2);
    }

    @Override
    public void finish() {
        assertTrue(nativeBitSet.get(1) && nativeBitSet.get(2));
    }

    @Test
    public void set_get_test() throws Throwable {
        TestFramework.runManyTimes(new ConcurrentBitSetMultithreadingTest(), 1000);
    }
}
```

In this test, there are two threads simultaneously executing the thread1 and thread2 methods. Both of these methods set the values of bits 1 and 2, 
respectively. If there are no data races, both threads should be able to see the designated values and set them. *In the Java BitSet implementation, 
bits 1 and 2 belong to the same array element.* The finish method checks the values of bits 1 and 2: if both bits were set, then assertTrue won't throw an 
exception, otherwise, the test will fail.

Such a test on the native Java implementation of BitSet is expected to fail, indicating that the test has identified thread safety issues.

```shell
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running io.github.ololx.samples.concurrent.bitset.ConcurrentBitsetTest
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.056 sec - in io.github.ololx.samples.concurrent.bitset.ConcurrentBitsetTest
Running io.github.ololx.samples.concurrent.bitset.ConcurrentBitSetMultithreadingTest
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.019 sec <<< FAILURE! - in io.github.ololx.samples.concurrent.bitset.ConcurrentBitSetMultithreadingTest
set_get_test(io.github.ololx.samples.concurrent.bitset.ConcurrentBitSetMultithreadingTest)  Time elapsed: 0.018 sec  <<< FAILURE!
junit.framework.AssertionFailedError: null
        at junit.framework.Assert.fail(Assert.java:55)
        at junit.framework.Assert.assertTrue(Assert.java:22)
        at junit.framework.Assert.assertTrue(Assert.java:31)
        at io.github.ololx.samples.concurrent.bitset.ConcurrentBitSetMultithreadingTest.finish(ConcurrentBitSetMultithreadingTest.java:28)


Results :

Failed tests: 
  ConcurrentBitSetMultithreadingTest.set_get_test:33->finish:28 null

Tests run: 17, Failures: 1, Errors: 0, Skipped: 0
```
## Concurrent BitSet using synchronization

The first and most apparent approach is to synchronize each access to the internal data structures — the array of bits.
Effectively, it's enough to create a wrapper around the native BitSet instance, in which every invocation of methods from this instance would be synchronized.

```java
public class ConcurrentBitSetWithFullSynchronization {

    private final BitSet bitSet;

    private final Object monitor;

    public ConcurrentBitSetWithFullSynchronization(int size) {
        this.bitSet = new BitSet(size);
        this.monitor = new Object();
    }

    @Override
    public boolean get(int bitIndex) {
        synchronized (this.monitor) {
            return this.bitSet.get(bitIndex);
        }
    }

    @Override
    public void set(int bitIndex) {
        synchronized (this.monitor) {
            this.bitSet.set(bitIndex);
        }
    }

    @Override
    public void clear(int bitIndex) {
        synchronized (this.monitor) {
            this.bitSet.clear(bitIndex);
        }
    }

    @Override
    public void flip(int bitIndex) {
        synchronized (this.monitor) {
            this.bitSet.flip(bitIndex);
        }
    }
}
```

Such an implementation of BitSet is already considered thread-safe and passes the **thread safety correctness test**. It's important to note that now only one 
thread can be in any given method at a time, even if different parts of our BitSet are being accessed. In this scenario, under heavy multi-threaded usage, 
high performance should not be expected. I won't conduct the **benchmark** results for this implementation since there's nothing to compare the results to at 
the moment.

## Concurrent BitSet using read-write lock
The main drawback of the first implementation is the synchronization of any thread accessing the BitSet methods, which puts the threads in a sequential queue, 
causing them to wait for another thread to finish. In this case, it might be worth considering not blocking the threads attempting to read the BitSet value 
until a thread tries to modify the BitSet value. To implement such an approach, using a read-write lock could be a suitable choice.

A read-write lock distinguishes between read operations and write operations:
1. Read Lock: Multiple threads can hold a read lock simultaneously as long as no threads hold a write lock. This allows for concurrent read operations without 
blocking each other, which is useful when the data is being read more frequently than being written.
2. Write Lock: Only one thread can hold a write lock at a time, and during this time, no other thread can hold either a read or write lock. This ensures that 
   write operations are exclusive and don't interfere with each other or ongoing read operations.
   
The java.util.concurrent package in Java provides the ReentrantReadWriteLock class, which implements a read-write lock. This kind of lock can help achieve 
better concurrency in scenarios where there's a mix of read-heavy and write-heavy operations.

To implement a thread-safe BitSet using Java's ReentrantReadWriteLock, it's enough to create a wrapper around the native BitSet instance. In this wrapper, 
also need to initialize an instance of ReadWriteLock, and every call to the BitSet methods would be surrounded by the appropriate lock:
- A write-lock for methods that modify the BitSet's value.
- A read-lock for methods that read the BitSet's value.

```java
public class ConcurrentBitSetWithGeneralRWLock {

    private final BitSet bitSet;

    private final ReadWriteLock readWriteLock;

    public ConcurrentBitSetWithGeneralRWLock(int size) {
        this.bitSet = new BitSet(size);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean get(int bitIndex) {
        return this.lockAndGet(bitIndex, () -> this.bitSet.get(bitIndex));
    }

    @Override
    public void set(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::set);
    }

    @Override
    public void clear(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::clear);
    }

    @Override
    public void flip(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::flip);
    }

    private Boolean lockAndGet(int bitIndex, Supplier<Boolean> getBitSupplier) {
        this.readWriteLock.readLock()
                .lock();

        try {
            return getBitSupplier.get();
        } finally {
            this.readWriteLock.readLock()
                    .unlock();
        }
    }

    private void lockAndSet(int bitIndex, Consumer<Integer> setBitConsumer) {
        this.readWriteLock.writeLock()
                .lock();

        try {
            setBitConsumer.accept(bitIndex);
        } finally {
            this.readWriteLock.writeLock()
                    .unlock();
        }
    }
}
```

Such an implementation, in theory, should be more performant than the previous one, as not all threads would be synchronized at all times. Let's test this 
assumption in practice using a **benchmark**.

```java
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

    private ConcurrentBitSet concurrentBitSet;

    @Param(
            {
                    FULL_SYNCHRONIZATION,
                    ONE_READ_WRITE_LOCK
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
```

This test is executed with different combinations of parameters:
- typeOfBitSetRealization - the type of thread-safe BitSet implementation:
    - javaNativeWithSynchronizationByThis - concurrent BitSet using synchronization;
    - javaNativeWithOneReadWriteLock - concurrent BitSet using read-write lock;
- countOfGetters - the number of accesses for reading a specific bit from the BitSet (number of threads for reading);
- countOfSetters - the number of accesses for writing a specific bit to the BitSet (number of threads for writing);
- sizeOfBitSet - the number of bits in the BitSet, also determines the number of bits read/written by each thread.

```shell
Benchmark                                    (countOfGetters)  (countOfSetters)  (sizeOfBitSet)            (typeOfBitSetRealization)  Mode  Cnt     Score     Error  Units
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              10  javaNativeWithSynchronizationByThis  avgt   15     6,428 ±   0,321  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              10       javaNativeWithOneReadWriteLock  avgt   15     6,531 ±   0,294  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             100  javaNativeWithSynchronizationByThis  avgt   15     8,271 ±   0,176  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             100       javaNativeWithOneReadWriteLock  avgt   15     9,347 ±   0,268  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1            1000  javaNativeWithSynchronizationByThis  avgt   15    41,002 ±   3,840  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1            1000       javaNativeWithOneReadWriteLock  avgt   15    49,092 ±   1,320  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10              10  javaNativeWithSynchronizationByThis  avgt   15     8,107 ±   0,214  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10              10       javaNativeWithOneReadWriteLock  avgt   15     8,996 ±   0,310  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10             100  javaNativeWithSynchronizationByThis  avgt   15    42,455 ±   1,916  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10             100       javaNativeWithOneReadWriteLock  avgt   15    29,082 ±   0,833  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10            1000  javaNativeWithSynchronizationByThis  avgt   15  1177,444 ±  33,276  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                10            1000       javaNativeWithOneReadWriteLock  avgt   15   254,745 ±   4,431  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1              10  javaNativeWithSynchronizationByThis  avgt   15     8,084 ±   0,359  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1              10       javaNativeWithOneReadWriteLock  avgt   15     9,394 ±   0,289  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1             100  javaNativeWithSynchronizationByThis  avgt   15    37,827 ±   0,907  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1             100       javaNativeWithOneReadWriteLock  avgt   15    49,622 ±   4,295  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1            1000  javaNativeWithSynchronizationByThis  avgt   15  1055,402 ±  45,362  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                 1            1000       javaNativeWithOneReadWriteLock  avgt   15  2624,097 ± 346,594  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10              10  javaNativeWithSynchronizationByThis  avgt   15    10,623 ±   0,330  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10              10       javaNativeWithOneReadWriteLock  avgt   15    12,797 ±   0,496  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10             100  javaNativeWithSynchronizationByThis  avgt   15   103,904 ±   7,629  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10             100       javaNativeWithOneReadWriteLock  avgt   15    73,886 ±   3,412  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10            1000  javaNativeWithSynchronizationByThis  avgt   15  2299,186 ±  64,660  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                10                10            1000       javaNativeWithOneReadWriteLock  avgt   15  1936,798 ± 191,437  us/op
```

As a result, we can observe that the implementation with a read-write lock performs better compared to the synchronized approach when there is high contention 
and more frequent write accesses.

## Lock-striping BitSet

```java
public class ConcurrentBitSetWithSegmentsRWLocks {

    private static final int ADDRESS_BITS_PER_WORD = 6;

    private final BitSet bitSet;

    private final ReadWriteLock[] readWriteLocks;

    public ConcurrentBitSetWithSegmentsRWLocks(int size) {
        this.bitSet = new BitSet(size);
        this.readWriteLocks = new ReadWriteLock[wordIndex(size - 1) + 1];
        IntStream.range(0, this.readWriteLocks.length)
                .forEach(index -> this.readWriteLocks[index] = new ReentrantReadWriteLock());
    }

    @Override
    public boolean get(int bitIndex) {
        return this.lockAndGet(bitIndex, () -> this.bitSet.get(bitIndex));
    }

    @Override
    public void set(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::set);
    }

    @Override
    public void clear(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::clear);
    }

    @Override
    public void flip(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::flip);
    }

    private Boolean lockAndGet(int bitIndex, Supplier<Boolean> getBitSupplier) {
        this.readWriteLocks[wordIndex(bitIndex)].readLock()
                .lock();

        try {
            return getBitSupplier.get();
        } finally {
            this.readWriteLocks[wordIndex(bitIndex)].readLock()
                    .unlock();
        }
    }

    private void lockAndSet(int bitIndex, Consumer<Integer> setBitConsumer) {
        this.readWriteLocks[wordIndex(bitIndex)].writeLock()
                .lock();

        try {
            setBitConsumer.accept(bitIndex);
        } finally {
            this.readWriteLocks[wordIndex(bitIndex)].writeLock()
                    .unlock();
        }
    }

    private int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
}
```

## Lock free BitSet

```java
public class NonBlockingConcurrentBitset implements ConcurrentBitSet {

    private static final int WORD_SIZE = Byte.SIZE;

    private final AtomicWordArray data;

    public NonBlockingConcurrentBitset(int size) {
        this.data = new AtomicWordArray((size + WORD_SIZE - 1) / WORD_SIZE);
    }

    @Override
    public boolean get(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        return (this.data.getWordVolatile(wordIndex) & bitMask) >> bitOffset != 0;
    }

    @Override
    public void set(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word | bitMask));
    }

    @Override
    public void clear(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word & ~bitMask));
    }

    @Override
    public void flip(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word ^ bitMask));
    }

    private static class AtomicWordArray {

        private static final VarHandle WORDS_VAR_HANDLER = MethodHandles.arrayElementVarHandle(byte[].class);

        private final byte[] words;

        private AtomicWordArray(int size) {
            this.words = new byte[size];
        }

        private void setWordVolatile(int wordIndex, Function<Byte, Byte> binaryOperation) {
            byte currentWordValue;
            byte newWordValue;

            do {
                currentWordValue = this.getWordVolatile(wordIndex);
                newWordValue = binaryOperation.apply(currentWordValue);
            } while (!WORDS_VAR_HANDLER.compareAndSet(this.words, wordIndex, currentWordValue, newWordValue));
        }

        private byte getWordVolatile(int wordIndex) {
            return (byte) WORDS_VAR_HANDLER.getVolatile(this.words, wordIndex);
        }
    }
}
```
