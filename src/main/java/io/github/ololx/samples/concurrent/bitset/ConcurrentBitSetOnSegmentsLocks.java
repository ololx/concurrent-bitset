package io.github.ololx.samples.concurrent.bitset;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * project concurrent-bitset
 * created 14.08.2023 18:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetOnSegmentsLocks extends AbstractBitSetConcurrentAdapter {

    private static final int ADDRESS_BITS_PER_WORD = 6;

    private final ReadWriteLock[] readWriteLocks;

    public ConcurrentBitSetOnSegmentsLocks(int size) {
        super(size);
        this.readWriteLocks = new ReadWriteLock[wordIndex(size - 1) + 1];
        IntStream.range(0, this.readWriteLocks.length)
                .forEach(index -> this.readWriteLocks[index] = new ReentrantReadWriteLock());
    }

    @Override
    public boolean get(int bitIndex) {
        this.lockReading(bitIndex);

        try {
            return this.bitSet.get(bitIndex);
        } finally {
            this.unlockReading(bitIndex);
        }
    }

    @Override
    public void set(int bitIndex) {
        this.lockWriting(bitIndex);

        try {
            this.bitSet.set(bitIndex);
        } finally {
            this.unlockWriting(bitIndex);
        }
    }

    @Override
    public void clear(int bitIndex) {
        this.lockWriting(bitIndex);

        try {
            this.bitSet.clear(bitIndex);
        } finally {
            this.unlockWriting(bitIndex);
        }
    }

    @Override
    public void flip(int bitIndex) {
        this.lockWriting(bitIndex);

        try {
            this.bitSet.flip(bitIndex);
        } finally {
            this.unlockWriting(bitIndex);
        }
    }

    private void lockReading(int bitIndex) {
        this.readWriteLocks[wordIndex(bitIndex)].readLock().lock();
    }

    private void unlockReading(int bitIndex) {
        this.readWriteLocks[wordIndex(bitIndex)].readLock().unlock();
    }

    private void lockWriting(int bitIndex) {
        this.readWriteLocks[wordIndex(bitIndex)].writeLock().lock();
    }

    private void unlockWriting(int bitIndex) {
        this.readWriteLocks[wordIndex(bitIndex)].writeLock().unlock();
    }

    private int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
}
