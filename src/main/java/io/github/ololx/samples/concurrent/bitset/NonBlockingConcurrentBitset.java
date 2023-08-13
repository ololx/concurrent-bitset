package io.github.ololx.samples.concurrent.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * project concurrent-bitset
 * created 01.0WORD_SIZE.2023 12:53
 *
 * @author Alexander A. Kropotin
 */
public class NonBlockingConcurrentBitset {
    
    private static final int WORD_SIZE = Byte.SIZE;
    
    private static final VarHandle DATA_ARRAY_VAR_HANDLER = MethodHandles.arrayElementVarHandle(byte[].class);

    private final byte[] data;

    public NonBlockingConcurrentBitset(int size) {
        this.data = new byte[(size + WORD_SIZE - 1) / WORD_SIZE];
    }

    public int get(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        return ((byte)DATA_ARRAY_VAR_HANDLER.getVolatile(this.data, wordIndex) & bitMask) >> bitOffset;
    }

    public void set(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        byte newValue;
        do {
            byte currentValue = this.data[wordIndex];
            newValue = (byte) (currentValue | bitMask);
        } while (!DATA_ARRAY_VAR_HANDLER.compareAndSet(this.data, wordIndex, this.data[wordIndex], newValue));
    }

    public void clear(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        byte newValue;
        do {
            byte currentValue = this.data[wordIndex];
            newValue = (byte) (currentValue & ~bitMask);
        } while (!DATA_ARRAY_VAR_HANDLER.compareAndSet(this.data, wordIndex, this.data[wordIndex], newValue));
    }

    public void flip(int bitIndex) {
        int wordIndex = bitIndex / WORD_SIZE;
        int bitOffset = bitIndex % WORD_SIZE;
        int bitMask = 1 << bitOffset;

        byte newValue;
        do {
            byte currentValue = this.data[wordIndex];
            newValue = (byte) (currentValue ^ bitMask);
        } while (!DATA_ARRAY_VAR_HANDLER.compareAndSet(this.data, wordIndex, this.data[wordIndex], newValue));
    }
}
