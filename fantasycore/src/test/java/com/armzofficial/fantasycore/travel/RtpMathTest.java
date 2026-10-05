package com.armzofficial.fantasycore.travel;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtpMathTest {

    @Test
    void samplesStayInsideAnnulusAndFootprintStaysInOneChunk() {
        SplittableRandom random = new SplittableRandom(42);
        for (int i = 0; i < 50_000; i++) {
            int[] p = RtpMath.sample(random, 100, -200, 500, 4000);
            double d = RtpMath.distance(100, -200, p[0], p[1]);
            assertTrue(d >= 497 && d <= 4003, "distance " + d);
            assertNotEquals(15, p[0] & 15);
            assertNotEquals(15, p[1] & 15);
        }
    }

    @Test
    void distributionIsUniformByArea() {
        // วงแหวน 500..4000: สัดส่วนจุดที่ระยะ < 2250 ต้องเท่าสัดส่วนพื้นที่ (2250²-500²)/(4000²-500²) ≈ 0.305
        SplittableRandom random = new SplittableRandom(7);
        int inner = 0;
        int total = 200_000;
        for (int i = 0; i < total; i++) {
            int[] p = RtpMath.sample(random, 0, 0, 500, 4000);
            if (RtpMath.distance(0, 0, p[0], p[1]) < 2250) {
                inner++;
            }
        }
        double expected = (2250.0 * 2250 - 500.0 * 500) / (4000.0 * 4000 - 500.0 * 500);
        assertEquals(expected, inner / (double) total, 0.01);
    }

    @Test
    void alignFootprint() {
        assertEquals(14, RtpMath.alignFootprint(15));
        assertEquals(-2, RtpMath.alignFootprint(-1));
        assertEquals(16, RtpMath.alignFootprint(16));
        assertThrows(IllegalArgumentException.class, () -> RtpMath.sample(new SplittableRandom(), 0, 0, 10, 10));
    }
}
