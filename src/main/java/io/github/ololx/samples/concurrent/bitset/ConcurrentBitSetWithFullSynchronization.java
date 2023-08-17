package io.github.ololx.samples.concurrent.bitset;

import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Provides a concurrent implementation of a bit set using full synchronization.
 * This class extends {@link AbstractBitSetConcurrentWrapper}.
 *
 * @apiNote This class is suitable for scenarios where thread safety is required, but the performance
 * impact of full synchronization may be a concern.
 * @implNote This implementation ensures that all operations on the bit set are synchronized
 * using a monitor lock on the underlying {@link BitSet} instance, providing thread safety.
 * @implSpec All methods are synchronized using the underlying {@link BitSet} instance.
 * While providing thread safety, this approach may result in lower concurrency compared
 * to other implementations that use more fine-grained locking mechanisms.
 * @see AbstractBitSetConcurrentWrapper
 * <p>
 * project concurrent-bitset
 * created 14.08.2023 14:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetWithFullSynchronization extends AbstractBitSetConcurrentWrapper {

    /**
     * General monitor for the whole bit set.
     */
    private final Object monitor;
    
    /**
     * Constructs a concurrent bit set with the specified size.
     *
     * @param size The size of the bit set.
     */
    public ConcurrentBitSetWithFullSynchronization(int size) {
        super(size);
        this.monitor = new Object();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This method is synchronized using a monitor lock on the underlying {@link BitSet}.
     */
    @Override
    public boolean get(int bitIndex) {
        synchronized (this.monitor) {
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
        synchronized (this.monitor) {
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
        synchronized (this.monitor) {
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
        synchronized (this.monitor) {
            this.bitSet.flip(bitIndex);
        }
    }
}
