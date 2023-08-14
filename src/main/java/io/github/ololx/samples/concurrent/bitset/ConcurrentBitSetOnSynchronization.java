package io.github.ololx.samples.concurrent.bitset;

/**
 * project concurrent-bitset
 * created 14.08.2023 14:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetOnSynchronization extends AbstractBitSetConcurrentAdapter {

    public ConcurrentBitSetOnSynchronization(int size) {
        super(size);
    }

    @Override
    public boolean get(int bitIndex) {
        synchronized (this.bitSet) {
            return this.bitSet.get(bitIndex);
        }
    }

    @Override
    public void set(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.set(bitIndex);
        }
    }

    @Override
    public void clear(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.clear(bitIndex);
        }
    }

    @Override
    public void flip(int bitIndex) {
        synchronized (this.bitSet) {
            this.bitSet.flip(bitIndex);
        }
    }
}
