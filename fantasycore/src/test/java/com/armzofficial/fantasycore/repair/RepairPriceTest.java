package com.armzofficial.fantasycore.repair;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class RepairPriceTest {
    @Test
    void fullItemIsFreeAndDamagedItemRoundsUp() {
        assertEquals(0, RepairPrice.calculate(0, 250, 50, 450));
        assertEquals(52, RepairPrice.calculate(1, 250, 50, 450));
        assertEquals(230, RepairPrice.calculate(100, 250, 50, 450));
        assertEquals(499, RepairPrice.calculate(249, 250, 50, 450));
    }

    @Test
    void invalidDurabilityAndPricingNeverProduceFreeRepair() {
        assertThrows(IllegalArgumentException.class, () -> RepairPrice.calculate(-1, 250, 50, 450));
        assertThrows(IllegalArgumentException.class, () -> RepairPrice.calculate(250, 250, 50, 450));
        assertThrows(IllegalArgumentException.class, () -> RepairPrice.calculate(1, 0, 50, 450));
        assertThrows(IllegalArgumentException.class, () -> RepairPrice.calculate(1, 250, -1, 450));
        assertThrows(IllegalArgumentException.class, () -> RepairPrice.calculate(1, 250, 0, 0));
        assertThrows(ArithmeticException.class, () -> RepairPrice.calculate(2, 3, 0, Long.MAX_VALUE));
    }

    @Test
    void integerRoundingMatchesRationalReferenceAtSmallAndLargeBounds() {
        for (int max : new int[]{2, 250, 1561, Integer.MAX_VALUE}) {
            for (int damage : new int[]{1, max / 2, max - 1}) {
                long full = 1_000_000_000L;
                long expected = BigInteger.valueOf(full).multiply(BigInteger.valueOf(damage))
                        .add(BigInteger.valueOf(max - 1L)).divide(BigInteger.valueOf(max)).add(BigInteger.valueOf(50)).longValueExact();
                assertEquals(expected, RepairPrice.calculate(damage, max, 50, full));
            }
        }
    }
}
