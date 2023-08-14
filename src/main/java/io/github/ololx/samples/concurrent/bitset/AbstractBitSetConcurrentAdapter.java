package io.github.ololx.samples.concurrent.bitset;

import java.util.BitSet;

/**
 * project concurrent-bitset
 * created 14.08.2023 14:55
 *
 * @author Alexander A. Kropotin
 */
public abstract class AbstractBitSetConcurrentAdapter implements ConcurrentBitSet {

    protected final BitSet bitSet;

    public AbstractBitSetConcurrentAdapter(int size) {
        this.bitSet = new BitSet(size);
    }
}
