package io.github.ololx.samples.concurrent.bitset;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.*;

/**
 * project concurrent-bitset
 * created 14.08.2023 13:49
 *
 * @author Alexander A. Kropotin
 */
public class ConcurrentBitsetTest {

    @DataProvider
    public Object[][] providesBitSetsAndUnitIndexes() {
        var random = new Random();
        int bitSetSize = random.nextInt(1_000) + 100;
        var unitIndexes = IntStream.range(0, random.nextInt(99))
                .boxed()
                .filter(index -> random.nextBoolean())
                .collect(Collectors.toList());

        return new Object[][]{
                {
                        new ConcurrentBitSetOnFullSynchronization(bitSetSize),
                        unitIndexes,
                        bitSetSize
                },
                {
                        new ConcurrentBitSetOnGeneralLock(bitSetSize),
                        unitIndexes,
                        bitSetSize
                },
                {
                        new ConcurrentBitSetOnSegmentsLocks(bitSetSize),
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
    public void get_whenBitIsTrue_thenReturnTrue(ConcurrentBitSet bitset, List<Integer> unitIndexes, int bitSetSize) {
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

        assertTrue(unitBits.stream()
                           .allMatch(bit -> bit));
        assertTrue(nilBits.stream()
                           .noneMatch(bit -> bit));
    }

    @Test(dataProvider = "providesBitSetsAndUnitIndexes")
    public void set_whenSetBit_thenBitIsTrue(ConcurrentBitSet bitset, List<Integer> unitIndexes, int bitSetSize) {
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

        assertTrue(unitBits.stream()
                           .allMatch(bit -> bit));
        assertTrue(nilBits.stream()
                           .noneMatch(bit -> bit));
    }

    @Test(dataProvider = "providesBitSetsAndUnitIndexes")
    public void clear_whenBitWasTrue_thenBitIsFalseNow(ConcurrentBitSet bitset, List<Integer> unitIndexes,
                                                       int bitSetSize) {
        unitIndexes.parallelStream()
                .forEach(bitset::set);
        IntStream.range(0, bitSetSize)
                .parallel()
                .forEach(bitset::clear);

        var unitBits = unitIndexes.parallelStream()
                .map(bitset::get)
                .toList();
        var nilBits = IntStream.range(0, bitSetSize)
                .parallel()
                .filter(index -> !unitIndexes.contains(index))
                .mapToObj(bitset::get)
                .toList();

        assertTrue(nilBits.stream()
                           .noneMatch(bit -> bit));
        assertTrue(nilBits.stream()
                           .noneMatch(bit -> bit));
    }

    @Test(dataProvider = "providesBitSetsAndUnitIndexes")
    public void flip_whenBitWasTrue_thenBitIsFalseNowAndViceVersa(ConcurrentBitSet bitset, List<Integer> unitIndexes,
                                                                  int bitSetSize) {
        unitIndexes.parallelStream()
                .forEach(bitset::set);
        IntStream.range(0, bitSetSize)
                .parallel()
                .forEach(bitset::flip);

        var nilBits = unitIndexes.stream()
                .map(bitset::get)
                .toList();
        var unitBits = IntStream.range(0, bitSetSize)
                .parallel()
                .filter(index -> !unitIndexes.contains(index))
                .mapToObj(bitset::get)
                .toList();

        assertTrue(nilBits.stream()
                           .noneMatch(bit -> bit));
        assertTrue(unitBits.stream()
                           .allMatch(bit -> bit));
    }
}
