package io.github.ololx.samples.concurrent.bitset;

/**
 * An interface for a concurrent bit set, providing methods to manipulate
 * individual bits in a thread-safe manner.
 *
 * @apiNote Implementations of this interface are expected to provide thread-safe
 * behavior for all methods.
 *
 * project concurrent-bitset
 * created 14.08.2023 13:51
 *
 * @author Alexander A. Kropotin
 */
public interface ConcurrentBitSet {

    /**
     * Gets the value of the bit at the specified index.
     *
     * @param bitIndex The index of the bit to retrieve.
     * @return The value of the bit (true or false) at the specified index.
     */
    boolean get(int bitIndex);

    /**
     * Sets the bit at the specified index to 1.
     *
     * @param bitIndex The index of the bit to set.
     */
    void set(int bitIndex);

    /**
     * Clears the bit at the specified index (sets it to 0).
     *
     * @param bitIndex The index of the bit to clear.
     */
    void clear(int bitIndex);

    /**
     * Flips the value of the bit at the specified index (0 becomes 1, and 1 becomes 0).
     *
     * @param bitIndex The index of the bit to flip.
     */
    void flip(int bitIndex);
}
