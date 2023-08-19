package io.github.ololx.samples.concurrent.bitset;

import edu.umd.cs.mtc.MultithreadedTest;
import edu.umd.cs.mtc.TestFramework;
import org.junit.Test;

public class ConcurrentBitSetMultithreadingTest extends MultithreadedTest {

    private ConcurrentBitSetWithFullSynchronization concurrentBitSetWithFullSynchronization;

    private ConcurrentBitSetWithGeneralRWLock concurrentBitSetWithGeneralRWLock;

    private ConcurrentBitSetWithSegmentsRWLocks concurrentBitSetWithSegmentsRWLocks;

    private NonBlockingConcurrentBitSet nonBlockingConcurrentBitset;

    @Override
    public void initialize() {
        this.nonBlockingConcurrentBitset = new NonBlockingConcurrentBitSet(10);
        this.concurrentBitSetWithFullSynchronization = new ConcurrentBitSetWithFullSynchronization(10);
        this.concurrentBitSetWithGeneralRWLock = new ConcurrentBitSetWithGeneralRWLock(10);
        this.concurrentBitSetWithSegmentsRWLocks = new ConcurrentBitSetWithSegmentsRWLocks(10);
    }

    public void thread1() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(1);
        this.concurrentBitSetWithFullSynchronization.set(1);
        this.concurrentBitSetWithGeneralRWLock.set(1);
        this.concurrentBitSetWithSegmentsRWLocks.set(1);
    }

    public void thread2() throws InterruptedException {
        this.nonBlockingConcurrentBitset.set(2);
        this.concurrentBitSetWithFullSynchronization.set(2);
        this.concurrentBitSetWithGeneralRWLock.set(2);
        this.concurrentBitSetWithSegmentsRWLocks.set(2);
    }

    @Override
    public void finish() {
        assertTrue(nonBlockingConcurrentBitset.get(1) && nonBlockingConcurrentBitset.get(2));
        assertTrue(concurrentBitSetWithFullSynchronization.get(1) && concurrentBitSetWithFullSynchronization.get(2));
        assertTrue(concurrentBitSetWithGeneralRWLock.get(1) && concurrentBitSetWithGeneralRWLock.get(2));
        assertTrue(concurrentBitSetWithSegmentsRWLocks.get(1) && concurrentBitSetWithSegmentsRWLocks.get(2));
    }

    @Test
    public void set_get_test() throws Throwable {
        TestFramework.runManyTimes(new ConcurrentBitSetMultithreadingTest(), 1000);
    }
}
