package io.github.ololx.samples.concurrent.bitset;

import java.util.BitSet;

/**
 * An abstract base class for concurrent bit set implementations.
 * This class provides the basic structure and common functionality for concurrent bit set implementations.
 *
 * @apiNote This abstract class defines the common structure and initialization logic for concurrent bit set
 * implementations.
 * @implSpec Concrete subclasses are expected to implement the actual bit manipulation methods.
 * @see ConcurrentBitSet
 * <p>
 * project concurrent-bitset
 * created 14.08.2023 14:55
 *
 * @author Alexander A. Kropotin
 */
public abstract class AbstractBitSetConcurrentWrapper implements ConcurrentBitSet {

    /**
     * The underlying bit set to be manipulated concurrently.
     */
    protected final BitSet bitSet;

    /**
     * Constructs an abstract concurrent bit set adapter with the specified size.
     *
     * @param size The size of the bit set.
     */
    public AbstractBitSetConcurrentWrapper(int size) {
        this.bitSet = new BitSet(size);
    }
}
