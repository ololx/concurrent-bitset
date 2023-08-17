package io.github.ololx.samples.concurrent.bitset;

import java.util.BitSet;

/**
 * Provides a concurrent implementation of a bit set using full synchronization.
 * This class extends {@link AbstractBitSetConcurrentAdapter}.
 *
 * @apiNote This class is suitable for scenarios where thread safety is required, but the performance
 * impact of full synchronization may be a concern.
 * @implNote This implementation ensures that all operations on the bit set are synchronized
 * using a monitor lock on the underlying {@link BitSet} instance, providing thread safety.
 * @implSpec All methods are synchronized using the underlying {@link BitSet} instance.
 * While providing thread safety, this approach may result in lower concurrency compared
 * to other implementations that use more fine-grained locking mechanisms.
 * @see AbstractBitSetConcurrentAdapter
 * <p>
 * project concurrent-bitset
 * created 14.08.2023 14:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetWithFULLSynchronization extends AbstractBitSetConcurrentAdapter {

    /**
     * Constructs a concurrent bit set with the specified size.
     *
     * @param size The size of the bit set.
     */
    public ConcurrentBitSetWithFULLSynchronization(int size) {
        super(size);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method is synchronized using a monitor lock on the underlying {@link BitSet}.
     */
    @Override
    public boolean get(int bitIndex) {
        synchronized (this.bitSet) {
            return this.bitSet.get(bitIndex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method is synchronized using a monitor lock on the underlying {@link BitSet}.
     */
    @Override
    public void set(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.set(bitIndex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method is synchronized using a monitor lock on the underlying {@link BitSet}.
     */
    @Override
    public void clear(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.clear(bitIndex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method is synchronized using a monitor lock on the underlying {@link BitSet}.
     */
    @Override
    public void flip(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.flip(bitIndex);
        }
    }
}
