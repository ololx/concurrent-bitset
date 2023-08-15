package io.github.ololx.samples.concurrent.bitset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ConcurrentBitsetTest {

    private final ConcurrentBitSet bitset;

    private final List<Integer> unitIndexes;

    private final int bitSetSize;

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        var random = new Random();

        int bitSetSize = random.nextInt(1_000) + 100;
        var unitIndexes = IntStream.range(0, random.nextInt(99))
                .boxed()
                .filter(index -> random.nextBoolean())
                .collect(Collectors.toList());

        data.add(new Object[]{
                new ConcurrentBitSetOnFullSynchronization(bitSetSize),
                unitIndexes,
                bitSetSize
        });
        data.add(new Object[]{
                new ConcurrentBitSetOnGeneralLock(bitSetSize),
                unitIndexes,
                bitSetSize
        });
        data.add(new Object[]{
                new ConcurrentBitSetOnSegmentsLocks(bitSetSize),
                unitIndexes,
                bitSetSize
        });
        data.add(new Object[]{
                new NonBlockingConcurrentBitset(bitSetSize),
                unitIndexes,
                bitSetSize
        });

        return data;
    }

    public ConcurrentBitsetTest(ConcurrentBitSet bitset, List<Integer> unitIndexes, int bitSetSize) {
        this.bitset = bitset;
        this.unitIndexes = unitIndexes;
        this.bitSetSize = bitSetSize;
    }

    @Test
    public void get_whenBitIsTrue_thenReturnTrue() {
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

    @Test
    public void set_whenSetBit_thenBitIsTrue() {
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

    @Test
    public void clear_whenBitWasTrue_thenBitIsFalseNow() {
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

    @Test
    public void flip_whenBitWasTrue_thenBitIsFalseNowAndViceVersa() {
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
