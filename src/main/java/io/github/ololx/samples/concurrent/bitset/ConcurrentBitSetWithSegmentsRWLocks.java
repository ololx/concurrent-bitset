package io.github.ololx.samples.concurrent.bitset;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * A concurrent bit set implementation using separate segment locks.
 * This class extends {@link AbstractBitSetConcurrentAdapter}.
 *
 * @apiNote This class is suitable for scenarios where multiple threads may concurrently access
 * different segments of the bit set, ensuring better concurrency compared to full synchronization.
 * @implNote This implementation provides concurrency control by using separate read-write locks
 * for each segment of the bit set.
 * @implSpec All public methods in this class are thread-safe. The segment locks ensure
 * that operations on different segments can be performed concurrently by different threads.
 * @see AbstractBitSetConcurrentAdapter
 * <p>
 * project concurrent-bitset
 * created 14.08.2023 18:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetWithSegmentsRWLocks extends AbstractBitSetConcurrentAdapter {

    /**
     * The number of bits required to address a specific position within a "word."
     * Each "word" corresponds to a long (64 bits), so 6 bits are needed.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;

    /**
     * Array of read-write locks for each segment of the bit set.
     */
    private final ReadWriteLock[] readWriteLocks;

    /**
     * Constructs a concurrent bit set with separate segment locks.
     *
     * @param size The size of the bit set.
     */
    public ConcurrentBitSetWithSegmentsRWLocks(int size) {
        super(size);
        this.readWriteLocks = new ReadWriteLock[wordIndex(size - 1) + 1];
        IntStream.range(0, this.readWriteLocks.length)
                .forEach(index -> this.readWriteLocks[index] = new ReentrantReadWriteLock());
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a read lock on the segment containing the specified bit.
     */
    @Override
    public boolean get(int bitIndex) {
        return this.lockAndGet(bitIndex, () -> this.bitSet.get(bitIndex));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock on the segment containing the specified bit.
     */
    @Override
    public void set(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::set);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock on the segment containing the specified bit.
     */
    @Override
    public void clear(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::clear);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock on the segment containing the specified bit.
     */
    @Override
    public void flip(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::flip);
    }

    /**
     * Acquires a read lock, invokes the provided supplier, and releases the read lock afterward.
     *
     * @param bitIndex       The index of the bit to be accessed.
     * @param getBitSupplier The supplier providing the bit access operation.
     * @return The result of the bit access operation.
     */
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

    /**
     * Acquires a write lock, invokes the provided consumer, and releases the write lock afterward.
     *
     * @param bitIndex       The index of the bit to be modified.
     * @param setBitConsumer The consumer performing the bit modification operation.
     */
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

    /**
     * Calculates the index of the word containing the specified bit index.
     *
     * @param bitIndex The index of the bit.
     * @return The index of the word containing the bit.
     */
    private int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
}
