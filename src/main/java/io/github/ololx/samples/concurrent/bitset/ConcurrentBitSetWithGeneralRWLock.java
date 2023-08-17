package io.github.ololx.samples.concurrent.bitset;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A concurrent bit set implementation using a general read-write lock.
 * This class extends {@link AbstractBitSetConcurrentAdapter}.
 *
 * @apiNote This class is suitable for scenarios where access to the entire bit set needs
 * to be synchronized using a single lock.
 * @implSpec All public methods in this class are thread-safe. The single read-write lock
 * ensures that only one thread can perform write operations at a time, while allowing multiple
 * threads to perform read operations concurrently.
 * @see AbstractBitSetConcurrentAdapter
 * <p>
 * project concurrent-bitset
 * created 14.08.2023 14:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetWithGeneralRWLock extends AbstractBitSetConcurrentAdapter {

    /**
     * General read-write lock for the whole bit set.
     */
    private final ReadWriteLock readWriteLock;

    /**
     * Constructs a concurrent bit set with general lock.
     *
     * @param size The size of the bit set.
     */
    public ConcurrentBitSetWithGeneralRWLock(int size) {
        super(size);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a read lock.
     */
    @Override
    public boolean get(int bitIndex) {
        return this.lockAndGet(bitIndex, () -> this.bitSet.get(bitIndex));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock.
     */
    @Override
    public void set(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::set);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock.
     */
    @Override
    public void clear(int bitIndex) {
        this.lockAndSet(bitIndex, this.bitSet::clear);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method acquires a write lock.
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
        this.readWriteLock.readLock()
                .lock();

        try {
            return getBitSupplier.get();
        } finally {
            this.readWriteLock.readLock()
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
