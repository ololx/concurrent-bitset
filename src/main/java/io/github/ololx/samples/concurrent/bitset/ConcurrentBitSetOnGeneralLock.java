package io.github.ololx.samples.concurrent.bitset;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * project concurrent-bitset
 * created 14.08.2023 14:52
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitSetOnGeneralLock extends AbstractBitSetConcurrentAdapter {

    private final ReadWriteLock readWriteLock;

    public ConcurrentBitSetOnGeneralLock(int size) {
        super(size);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean get(int bitIndex) {
        this.readWriteLock.readLock().lock();

        try {
            return this.bitSet.get(bitIndex);
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void set(int bitIndex) {
        this.readWriteLock.writeLock().lock();

        try {
            this.bitSet.set(bitIndex);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void clear(int bitIndex) {
        this.readWriteLock.writeLock().lock();

        try {
            this.bitSet.clear(bitIndex);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void flip(int bitIndex) {
        this.readWriteLock.writeLock().lock();

        try {
            this.bitSet.flip(bitIndex);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }
}
