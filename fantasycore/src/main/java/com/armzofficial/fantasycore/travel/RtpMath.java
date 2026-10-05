package com.armzofficial.fantasycore.travel;

import java.util.random.RandomGenerator;

/** คณิตศาสตร์ของการสุ่มวาร์ป (ไม่มี Bukkit API เพื่อทดสอบได้) */
public final class RtpMath {

    private RtpMath() {
    }

    /**
     * สุ่มจุดแบบกระจายเท่ากันต่อพื้นที่ในวงแหวน rMin..rMax รอบ (cx, cz) — CASUAL-SURVIVAL §3 ข้อ 2
     * แล้วขยับให้ footprint 2×2 อยู่ใน chunk เดียว
     *
     * @return {x, z} มุมตะวันตกเฉียงเหนือของ footprint
     */
    public static int[] sample(RandomGenerator random, int cx, int cz, int rMin, int rMax) {
        if (rMin < 0 || rMax <= rMin) {
            throw new IllegalArgumentException("ต้องมี 0 <= rMin < rMax");
        }
        double u = random.nextDouble();
        double r = Math.sqrt(u * ((double) rMax * rMax - (double) rMin * rMin) + (double) rMin * rMin);
        double theta = random.nextDouble() * Math.PI * 2;
        int x = cx + (int) Math.floor(r * Math.cos(theta));
        int z = cz + (int) Math.floor(r * Math.sin(theta));
        return new int[]{alignFootprint(x), alignFootprint(z)};
    }

    /** ถ้า coord อยู่ขอบขวาสุดของ chunk (local 15) เลื่อนเข้า 1 บล็อกให้ 2×2 อยู่ใน chunk เดียว */
    public static int alignFootprint(int coord) {
        return (coord & 15) == 15 ? coord - 1 : coord;
    }

    public static double distance(int cx, int cz, int x, int z) {
        double dx = x - cx;
        double dz = z - cz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
