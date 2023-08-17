package io.github.ololx.samples.concurrent.bitset;

import edu.umd.cs.mtc.MultithreadedTest;
import edu.umd.cs.mtc.TestFramework;
import org.junit.Test;

public class ConcurrentBitSetMultithreadingTest extends MultithreadedTest {

    private NonBlockingConcurrentBitset nonBlockingConcurrentBitset;

    private ConcurrentBitSetWithSynchronizationByThis concurrentBitSetOnFullSynchronization;

    private ConcurrentBitSetWithOneReadWriteLock concurrentBitSetOnGeneralLock;

    private ConcurrentBitSetWithManyReadWriteLocksBySegments concurrentBitSetOnSegmentsLocks;

    @Override
    public void initialize() {
        this.nonBlockingConcurrentBitset = new NonBlockingConcurrentBitset(10);
        this.concurrentBitSetOnFullSynchronization = new ConcurrentBitSetWithSynchronizationByThis(10);
        this.concurrentBitSetOnGeneralLock = new ConcurrentBitSetWithOneReadWriteLock(10);
        this.concurrentBitSetOnSegmentsLocks = new ConcurrentBitSetWithManyReadWriteLocksBySegments(10);
    }

    public void thread1() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(1);
        this.concurrentBitSetOnFullSynchronization.set(1);
        this.concurrentBitSetOnGeneralLock.set(1);
        this.concurrentBitSetOnSegmentsLocks.set(1);
    }

    public void thread2() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(2);
        this.concurrentBitSetOnFullSynchronization.set(2);
        this.concurrentBitSetOnGeneralLock.set(2);
        this.concurrentBitSetOnSegmentsLocks.set(2);
    }

    public void thread3() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(3);
        this.concurrentBitSetOnFullSynchronization.set(3);
        this.concurrentBitSetOnGeneralLock.set(3);
        this.concurrentBitSetOnSegmentsLocks.set(3);
    }

    public void thread4() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(4);
        this.concurrentBitSetOnFullSynchronization.set(4);
        this.concurrentBitSetOnGeneralLock.set(4);
        this.concurrentBitSetOnSegmentsLocks.set(4);
    }

    @Override
    public void finish() {
        assertTrue(nonBlockingConcurrentBitset.get(1) &&
                           nonBlockingConcurrentBitset.get(2) &&
                           nonBlockingConcurrentBitset.get(3) &&
                           nonBlockingConcurrentBitset.get(4)
        );
        assertTrue(concurrentBitSetOnFullSynchronization.get(1) &&
                           concurrentBitSetOnFullSynchronization.get(2) &&
                           concurrentBitSetOnFullSynchronization.get(3) &&
                           concurrentBitSetOnFullSynchronization.get(4)
        );
        assertTrue(concurrentBitSetOnGeneralLock.get(1) &&
                           concurrentBitSetOnGeneralLock.get(2) &&
                           concurrentBitSetOnGeneralLock.get(3) &&
                           concurrentBitSetOnGeneralLock.get(4)
        );
        assertTrue(concurrentBitSetOnSegmentsLocks.get(1) &&
                           concurrentBitSetOnSegmentsLocks.get(2) &&
                           concurrentBitSetOnSegmentsLocks.get(3) &&
                           concurrentBitSetOnSegmentsLocks.get(4)
        );
    }

    @Test
    public void set_get_test() throws Throwable {
        TestFramework.runManyTimes(new ConcurrentBitSetMultithreadingTest(), 1000);
    }
}
