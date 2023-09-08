# concurrent-bitset

## About

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
the **benchmark** is conducted using the [**Java Microbenchmark Harness (JMH)**](https://github.com/openjdk/jmh).

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
### 1. Concurrent BitSet using synchronization

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

### 2. Concurrent BitSet using read-write lock
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
Benchmark                                    (countOfGetters)  (countOfSetters)  (sizeOfBitSet)            (typeOfBitSetRealization)  Mode  Cnt    Score    Error  Units
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64  javaNativeWithSynchronizationByThis  avgt   15    7,599 ±  0,249  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64       javaNativeWithOneReadWriteLock  avgt   15   16,029 ± 12,473  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640  javaNativeWithSynchronizationByThis  avgt   15   26,445 ±  1,427  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640       javaNativeWithOneReadWriteLock  avgt   15   33,103 ±  1,770  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64  javaNativeWithSynchronizationByThis  avgt   15   13,650 ±  1,473  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64       javaNativeWithOneReadWriteLock  avgt   15   15,289 ±  1,517  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640  javaNativeWithSynchronizationByThis  avgt   15  171,227 ± 49,553  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640       javaNativeWithOneReadWriteLock  avgt   15  101,555 ±  6,714  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64  javaNativeWithSynchronizationByThis  avgt   15   12,510 ±  0,704  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64       javaNativeWithOneReadWriteLock  avgt   15   16,947 ±  3,171  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640  javaNativeWithSynchronizationByThis  avgt   15  157,836 ± 34,753  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640       javaNativeWithOneReadWriteLock  avgt   15  155,556 ± 13,568  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64  javaNativeWithSynchronizationByThis  avgt   15   21,713 ±  1,146  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64       javaNativeWithOneReadWriteLock  avgt   15   23,875 ±  1,942  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640  javaNativeWithSynchronizationByThis  avgt   15  550,413 ± 31,637  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640       javaNativeWithOneReadWriteLock  avgt   15  220,764 ±  4,870  us/op
```

As a result, we can observe that the implementation with a read-write lock performs better compared to the synchronized approach when there is high contention 
and more frequent write accesses.

### 3. Concurrent BitSet with fine-grained locking

In the previous enhancement, is attempted to restrict a thread's access to reading from the BitSet only if another thread was attempting to write a value to 
the BitSet at that moment. Therefore, the new enhancement is based on the following idea - instead of locking access to the entire BitSet, it's better 
to synchronize access only when threads are accessing the same index in the array of bits. This approach is known as **fine-grained locking**. *It's worth noting 
that in a BitSet, all bits are stored in an array (in the native Java BitSet, this is a long[]), where each array element represents a machine word. 
And two threads writing to adjacent bytes shouldn't interfere with each other - word tearing is forbidden in the JVM.*

For the 'words' array, it's necessary to correspond array of locks. At the start, the size of the lock array should be less than or equal to the 
internal size of the 'words' array - this is important to prevent two locks on a single word. *The size of the lock array can be made a constructor parameter 
and configured more finely, but for simplicity, we'll create a lock array with the same dimension as the 'words' array.*

```java
public class ConcurrentBitSetWithSegmentsRWLocks {

    private static final int ADDRESS_BITS_PER_WORD = 6;

    private final BitSet bitSet;

    private final ReadWriteLock[] readWriteLocks;

    public ConcurrentBitSetWithSegmentsRWLocks(int size) {
        this.bitSet = new BitSet(size);
        this.readWriteLocks = new ReadWriteLock[wordIndex(size - 1) + 1];

        for (int index = 0; index < this.readWriteLocks.length; index++) {
            this.readWriteLocks[index] = new ReentrantReadWriteLock();
        }
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

Such an implementation passes the **thread safety correctness test** and in theory, it should be more performant than the previous implementations, as 
evidenced by the **benchmark** results. In the new **benchmark**, the parameter typeOfBitSetRealization is added:
- javaNativeWithSynchronizationByThis - concurrent BitSet using synchronization;
- javaNativeWithOneReadWriteLock - concurrent BitSet using read-write lock;
- javaNativeWithManyReadWriteLocksBySegments - concurrent BitSet using fine-grained locking.

```shell
Benchmark                                    (countOfGetters)  (countOfSetters)  (sizeOfBitSet)                   (typeOfBitSetRealization)  Mode  Cnt    Score    Error  Units
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64         javaNativeWithSynchronizationByThis  avgt   15    8,049 ±  1,229  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64              javaNativeWithOneReadWriteLock  avgt   15    8,357 ±  0,529  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15    8,260 ±  0,337  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640         javaNativeWithSynchronizationByThis  avgt   15   25,674 ±  1,121  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640              javaNativeWithOneReadWriteLock  avgt   15   31,116 ±  0,803  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   25,550 ±  0,490  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64         javaNativeWithSynchronizationByThis  avgt   15   12,312 ±  0,684  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64              javaNativeWithOneReadWriteLock  avgt   15   13,336 ±  0,415  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   13,865 ±  0,943  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640         javaNativeWithSynchronizationByThis  avgt   15  149,213 ± 37,374  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640              javaNativeWithOneReadWriteLock  avgt   15   96,545 ±  1,655  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   82,410 ±  5,834  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64         javaNativeWithSynchronizationByThis  avgt   15   12,124 ±  0,486  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64              javaNativeWithOneReadWriteLock  avgt   15   15,382 ±  0,639  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   15,305 ±  0,345  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640         javaNativeWithSynchronizationByThis  avgt   15  134,842 ± 22,531  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640              javaNativeWithOneReadWriteLock  avgt   15  154,292 ±  5,257  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   58,527 ±  1,225  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64         javaNativeWithSynchronizationByThis  avgt   15   20,826 ±  1,509  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64              javaNativeWithOneReadWriteLock  avgt   15   22,992 ±  1,885  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   22,446 ±  0,687  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640         javaNativeWithSynchronizationByThis  avgt   15  507,174 ± 17,105  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640              javaNativeWithOneReadWriteLock  avgt   15  236,177 ± 42,689  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15  148,687 ±  4,516  us/op
```

As a result, you can observe that fine-grained locking has proven to be more performant than the two previous approaches, and the performance advantage becomes 
more noticeable as the size of the BitSet elements increases.

### 4. Concurrent BitSet using lock free

The previous implementation turned out to be even more performant, but it still queues up threads, forcing them to wait for another thread's completion. 
Additionally, the extra cost of synchronizing the system context increases as the number of waiting threads grows. Therefore, the emphasis in this new 
implementation is on eliminating locks entirely.

The requirements for the new implementation can be formulated as follows:
1. Any two threads working with different words should not be synchronized or queued (word tearing is forbidden in the JVM).
2. If multiple threads access the same word for read/write, there should be no reordering of operations (happens-before guarantees between threads should be 
   provided).
3. Multiple readers for the same word index should not be blocked unless there is a writer among them.

When looking at these requirements, (1) is already inherent in the JVM and was utilized in the previous implementation, and for (2)-(3), we need to make our 
'words' array volatile. Unfortunately, we can't simply declare our 'words' array as volatile and be done with it: even if an array is declared as volatile, 
it doesn't provide volatile semantics for reading or writing its elements. For simultaneous access to the k-th element of the array, external synchronization 
is required. Volatile applies only to the array reference itself. However, we can implement our custom array with atomic features (similar to atomic data types), 
which would fulfill both (2) and (3). This custom array can provide us with volatile read operations and non-blocking insertion of new values through CAS operations.

```java
class AtomicWordArray { 
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
```

I intentionally use a byte[] as the 'words' array, as this allows us to use 1 byte when creating an 8-bit BitSet instead of 64 bits, and simplifies 
the logic of working with bits. *However, nothing prevents us from implementing a similar Atomic array over long[].*
Now, it's enough to apply the AtomicWordArray in the lock-free BitSet implementation and write the implementation of the ConcurrentBitSet methods.

```java
public class NonBlockingConcurrentBitSet {

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
        this.data.setWordVolatile(wordIndex, word -> (byte) (word | bitMask));
    }

    @Override
    public void clear(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, word -> (byte) (word & ~bitMask));
    }

    @Override
    public void flip(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, word -> (byte) (word ^ bitMask));
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

This implementation passes the **thread safety correctness test**. In the new **benchmark**, the parameter typeOfBitSetRealization is added:
- javaNativeWithSynchronizationByThis - concurrent BitSet using synchronization;
- javaNativeWithOneReadWriteLock - concurrent BitSet using read-write lock;
- javaNativeWithManyReadWriteLocksBySegments - concurrent BitSet using fine-grained locking;
- NonBlockingConcurrentBitset - concurrent BitSet using lock free.

```shell
Benchmark                                    (countOfGetters)  (countOfSetters)  (sizeOfBitSet)                   (typeOfBitSetRealization)  Mode  Cnt    Score    Error  Units
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64         javaNativeWithSynchronizationByThis  avgt   15    7,560 ±  0,346  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64              javaNativeWithOneReadWriteLock  avgt   15    8,263 ±  0,256  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15    8,368 ±  0,479  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1              64                 NonBlockingConcurrentBitset  avgt   15    6,820 ±  0,316  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640         javaNativeWithSynchronizationByThis  avgt   15   26,041 ±  1,395  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640              javaNativeWithOneReadWriteLock  avgt   15   32,026 ±  0,840  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   25,453 ±  0,910  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 1             640                 NonBlockingConcurrentBitset  avgt   15   12,267 ±  0,258  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64         javaNativeWithSynchronizationByThis  avgt   15   11,902 ±  0,655  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64              javaNativeWithOneReadWriteLock  avgt   15   13,446 ±  0,759  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   13,747 ±  0,906  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5              64                 NonBlockingConcurrentBitset  avgt   15    9,903 ±  0,560  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640         javaNativeWithSynchronizationByThis  avgt   15  153,956 ± 34,896  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640              javaNativeWithOneReadWriteLock  avgt   15   95,176 ±  3,605  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   83,124 ±  2,602  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 1                 5             640                 NonBlockingConcurrentBitset  avgt   15   44,346 ±  2,026  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64         javaNativeWithSynchronizationByThis  avgt   15   12,377 ±  0,795  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64              javaNativeWithOneReadWriteLock  avgt   15   15,112 ±  0,397  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   16,468 ±  1,364  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1              64                 NonBlockingConcurrentBitset  avgt   15    7,733 ±  0,290  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640         javaNativeWithSynchronizationByThis  avgt   15  168,127 ± 34,236  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640              javaNativeWithOneReadWriteLock  avgt   15  151,939 ±  4,203  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15   59,806 ±  3,437  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 1             640                 NonBlockingConcurrentBitset  avgt   15   16,633 ±  0,574  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64         javaNativeWithSynchronizationByThis  avgt   15   19,994 ±  0,483  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64              javaNativeWithOneReadWriteLock  avgt   15   22,420 ±  0,471  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64  javaNativeWithManyReadWriteLocksBySegments  avgt   15   22,747 ±  1,589  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5              64                 NonBlockingConcurrentBitset  avgt   15   11,279 ±  0,949  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640         javaNativeWithSynchronizationByThis  avgt   15  507,401 ± 18,371  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640              javaNativeWithOneReadWriteLock  avgt   15  218,627 ±  4,848  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640  javaNativeWithManyReadWriteLocksBySegments  avgt   15  137,042 ± 10,853  us/op
ConcurrentBitSetBenchmark.get_set_benchmark                 5                 5             640                 NonBlockingConcurrentBitset  avgt   15   48,743 ±  2,908  us/op
```

As a result, it's evident that the current implementation has outperformed all the previous ones in terms of performance, all while maintaining the absence 
of compromises present in the 2nd and 3rd implementations. It's also worth noting that with low or no contention (1 setter, 1 getter, and 64 bits), 
all implementations exhibit similar performance.
