package io.github.ololx.samples.concurrent.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

/**
 * A thread-safe implementation of a concurrent bitset using atomic operations.
 *
 * @apiNote This class provides methods to manipulate individual bits in a thread-safe manner.
 *          It is designed for scenarios where multiple threads need to access and modify
 *          a shared bitset concurrently.
 */
public class NonBlockingConcurrentBitset implements ConcurrentBitSet{

    /**
     * The size of a word in bits.
     */
    private static final int WORD_SIZE = Byte.SIZE;

    /**
     * The internal storage for the bitset.
     */
    private final AtomicWordArray data;

    /**
     * Creates a new NonBlockingConcurrentBitset with the specified size.
     *
     * @param size The number of bits in the bitset.
     */
    public NonBlockingConcurrentBitset(int size) {
        this.data = new AtomicWordArray((size + WORD_SIZE - 1) / WORD_SIZE);
    }

    /**
     * Gets the value of the bit at the specified index.
     *
     * @param bitIndex The index of the bit to retrieve.
     * @return The value of the bit (true or false) at the specified index.
     */
    @Override
    public boolean get(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        return (this.data.getWordVolatile(wordIndex) & bitMask) >> bitOffset != 0;
    }

    /**
     * Sets the bit at the specified index to 1.
     *
     * @param bitIndex The index of the bit to set.
     */
    @Override
    public void set(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word | bitMask));
    }

    /**
     * Clears the bit at the specified index (sets it to 0).
     *
     * @param bitIndex The index of the bit to clear.
     */
    @Override
    public void clear(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word & ~bitMask));
    }

    /**
     * Flips the value of the bit at the specified index (0 becomes 1, and 1 becomes 0).
     *
     * @param bitIndex The index of the bit to flip.
     */
    @Override
    public void flip(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;
        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word ^ bitMask));
    }

    /**
     * An internal class that represents an atomic word array for bit storage.
     */
    private static class AtomicWordArray {

        private static final VarHandle WORDS_VAR_HANDLER = MethodHandles.arrayElementVarHandle(byte[].class);

        private final byte[] words;

        /**
         * Creates a new AtomicWordArray with the specified size.
         *
         * @param size The size of the array in bytes.
         */
        private AtomicWordArray(int size) {
            this.words = new byte[size];
        }

        /**
         * Sets the value of the word at the specified index using a binary operation.
         *
         * @param wordIndex       The index of the word to set.
         * @param binaryOperation A function that defines the binary operation on the word.
         * @implSpec This method uses a loop with compareAndSet to ensure atomicity of the operation.
         */
        private void setWordVolatile(int wordIndex, Function<Byte, Byte> binaryOperation) {
            byte newWordValue;

            do {
                byte currentWordValue = this.words[wordIndex];
                newWordValue = binaryOperation.apply(currentWordValue);
            } while (!WORDS_VAR_HANDLER.compareAndSet(this.words, wordIndex, this.words[wordIndex], newWordValue));
        }

        /**
         * Gets the value of the word at the specified index in a volatile manner.
         *
         * @param wordIndex The index of the word to retrieve.
         * @return The value of the word at the specified index.
         * @implSpec This method uses VarHandle to perform a volatile read of the array element.
         */
        private byte getWordVolatile(int wordIndex) {
            return (byte) WORDS_VAR_HANDLER.getVolatile(this.words, wordIndex);
        }
    }
}
