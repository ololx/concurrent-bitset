package io.github.ololx.samples.concurrent.bitset;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * project concurrent-bitset
 * created 14.08.2023 13:49
 *
 * @author Alexander A. Kropotin
 */
public class NonBlockingConcurrentBitsetTest {

    @DataProvider
    public Object[][] providesBitSetsAndUnitIndexes() {
        var random = new Random();
        int bitSetSize = random.nextInt(1_000) + 100;
        var unitIndexes = IntStream.range(0, 10)
                .mapToObj(index -> random.nextInt(99))
                .collect(Collectors.toList());

        return new Object[][]{
                {
                        new ConcurrentBitSetOnSynchronization(bitSetSize),
                        unitIndexes,
                        bitSetSize
                },
                {
                        new ConcurrentBitSetOnGeneralLock(bitSetSize),
                        unitIndexes,
                        bitSetSize
                },
                {
                        new NonBlockingConcurrentBitset(bitSetSize),
                        unitIndexes,
                        bitSetSize
                },
        };
    }

    @Test(dataProvider = "providesBitSetsAndUnitIndexes")
    public void set_whenSetBit_thenBitIsSetted(ConcurrentBitSet bitset, List<Integer> unitIndexes, int bitSetSize) {
        unitIndexes.parallelStream()
                .forEach(bitset::set);
        var unitBits = unitIndexes.parallelStream()
                .map(bitset::get)
                .toList();
        var nilBits = IntStream.range(0, bitSetSize)
                .parallel()
                .filter(index -> !unitIndexes.contains(index))
                .mapToObj(bitset::get)
                .toList();

        assertTrue(unitBits.stream().allMatch(bit -> bit));
        assertTrue(nilBits.stream().noneMatch(bit -> bit));
    }
}
