package com.armzofficial.fantasycore.economy;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathPolicyTest {

    @Test
    void roundsDown() {
        assertEquals(300, DeathPolicy.loss(1_000, 30));
        assertEquals(0, DeathPolicy.loss(3, 30));
        assertEquals(29, DeathPolicy.loss(99, 30));
        assertEquals(0, DeathPolicy.loss(0, 30));
        assertEquals(0, DeathPolicy.loss(500, 0));
        assertEquals(500, DeathPolicy.loss(500, 100));
    }

    @Test
    void matchesExactMathWithoutOverflow() {
        long[] balances = {1, 7, 99, 100, 101, 12_345, 999_999_999_999L, Long.MAX_VALUE, Long.MAX_VALUE - 1};
        for (long balance : balances) {
            for (int percent = 0; percent <= 100; percent++) {
                long expected = BigInteger.valueOf(balance).multiply(BigInteger.valueOf(percent))
                        .divide(BigInteger.valueOf(100)).longValueExact();
                assertEquals(expected, DeathPolicy.loss(balance, percent), balance + " × " + percent + "%");
            }
        }
    }
}
