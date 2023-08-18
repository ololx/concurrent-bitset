# concurrent-bitset

Starting from Java version 1.0, the BitSet class has been available, representing an array of bits that can be set or cleared. It can be employed in various
scenarios where manipulation of bit-level information and flag handling is required, owing to its efficiency and compactness when dealing with substantial
volumes of bit-based data.
It sounds impressive, but there's a caveat: the native implementation of BitSet in Java is not thread-safe. The authors themselves caution about this
in comments within the class: **"A BitSet is not safe for multithreaded use without external synchronization."**

Я попробовал реализовать свой потокобезопасный BitSet и хочу показать, что из этого получлось. *Важно: полная реализация будет включена в новую версию моего
[moonshine project](https://github.com/ololx/moonshine) и если очень интересно - с реализацией можно будет ознакомиться там.* Сейчас мы рассмотрим только 
следующие методы потокобезопасного BItSet.
```java
public interface ConcurrentBitSet {
    
    boolean get(int bitIndex);
    
    void set(int bitIndex);
    
    void clear(int bitIndex);
    
    void flip(int bitIndex);
}
```

Ниже я приведу примеры кода различных способов реализовать потокобезопасный BitSet и их сравнение друг с другом. 
Для этого будет использовано два вида тестов:
1. **thread safety correctness test**, который покажет, что у конкретной реализации нет data races.
2. **benchmark**, который покажет производительность каждой реализации с точки зрения throughput.

Для начала, давайте попробуем применить **thread safety correctness test** к нативной реализации BitSet в Java и убедимся, что она действительно не 
потокобезопасна, а тест позволяет это выявить.
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

В этом тесте есть два потока, выполняющих одновременно методы thread1 и thread2, соответственно, оба они устанавливают значение битов 1 и 2 соответстченно;
если нет data races, оба потока должны видеть заданные значения и установить их. В методе finish проверяется значение битов 1 и 2: если оба бита были 
установленны, то assertTrue не вызовет исключения, в противном случае - тест будет провален.
Такой тест на нативной Java реализации BitSet ожидаемо провалился, значит тест выявил проблемы с потокобезопасностью.

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
Effectively, we can create a wrapper around the native BitSet instance, in which every invocation of methods from this instance would be synchronized.

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

Such an implementation of BitSet is already considered thread-safe and passes our thread safety test. It's important to note that now only one thread can
be in any given method at a time, even if different parts of our BitSet are being accessed. In this scenario, under heavy multi-threaded usage, high performance
should not be expected.

## Concurrent BitSet using read-write lock

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
