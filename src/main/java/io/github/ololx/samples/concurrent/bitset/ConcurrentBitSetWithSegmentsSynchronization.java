package io.github.ololx.samples.concurrent.bitset;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * A concurrent bit set implementation using separate segment locks.
 * This class extends {@link AbstractBitSetConcurrentWrapper}.
 *
 * @author Alexander A. Kropotin
 * @apiNote This class is designed for scenarios where multiple threads may concurrently access
 * different segments of the bit set, leading to better concurrency compared to full synchronization.
 * @implNote This implementation achieves concurrency control by utilizing separate
 * read-write locks for each segment of the bit set.
 * @implSpec All public methods in this class are thread-safe. The segment locks ensure
 * that operations on different segments can be executed concurrently by different threads.
 * @see AbstractBitSetConcurrentWrapper
 * <p>
 * project concurrent-bitset
 * created 24.08.2023 19:52
 */
public class ConcurrentBitSetWithSegmentsSynchronization extends AbstractBitSetConcurrentWrapper {

    /**
     * The number of bits needed to address a specific position within a "word."
     * Each "word" corresponds to a long (64 bits), so 6 bits are required.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;

    /**
     * An array of read-write locks, each corresponding to a segment of the bit set.
     */
    private final Object[] monitors;

    /**
     * Constructs a concurrent bit set with individual segment locks.
     *
     * @param size The size of the bit set.
     */
    public ConcurrentBitSetWithSegmentsSynchronization(int size) {
        super(size);
        this.monitors = new Object[wordIndex(size - 1) + 1];
        IntStream.range(0, this.monitors.length)
                .forEach(index -> this.monitors[index] = new Object());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the value of the bit at the specified index.
     *
     * @param bitIndex The index of the bit to be retrieved.
     * @return The value of the bit at the specified index.
     */
    @Override
    public boolean get(int bitIndex) {
        return this.syncAndGet(bitIndex, () -> this.bitSet.get(bitIndex));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the bit at the specified index.
     *
     * @param bitIndex The index of the bit to be set.
     */
    @Override
    public void set(int bitIndex) {
        this.syncAndSet(bitIndex, this.bitSet::set);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Clears the bit at the specified index.
     *
     * @param bitIndex The index of the bit to be cleared.
     */
    @Override
    public void clear(int bitIndex) {
        this.syncAndSet(bitIndex, this.bitSet::clear);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Flips the bit at the specified index.
     *
     * @param bitIndex The index of the bit to be flipped.
     */
    @Override
    public void flip(int bitIndex) {
        this.syncAndSet(bitIndex, this.bitSet::flip);
    }

    /**
     * Acquires a read lock, invokes the provided supplier, and releases the read lock afterward.
     *
     * @param bitIndex       The index of the bit to be accessed.
     * @param accessSupplier The supplier providing the bit access operation.
     * @return The result of the bit access operation.
     */
    private Boolean syncAndGet(int bitIndex, Supplier<Boolean> accessSupplier) {
        synchronized (this.monitors[wordIndex(bitIndex)]) {
            return accessSupplier.get();
        }
    }

    /**
     * Acquires a write lock, invokes the provided consumer, and releases the write lock afterward.
     *
     * @param bitIndex             The index of the bit to be modified.
     * @param modificationConsumer The consumer performing the bit modification operation.
     */
    private void syncAndSet(int bitIndex, Consumer<Integer> modificationConsumer) {
        synchronized (this.monitors[wordIndex(bitIndex)]) {
            modificationConsumer.accept(bitIndex);
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

