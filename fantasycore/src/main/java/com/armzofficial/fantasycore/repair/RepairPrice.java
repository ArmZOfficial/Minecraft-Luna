package com.armzofficial.fantasycore.repair;

/** ค่าซ่อม = base + ceil(fullDamageCost × damage / maxDamage); ของเต็มไม่คิดเงิน */
public final class RepairPrice {
    private RepairPrice() {
    }

    public static long calculate(int damage, int maxDamage, long base, long fullDamageCost) {
        if (maxDamage < 1 || damage < 0 || damage >= maxDamage || base < 0 || fullDamageCost < 1) {
            throw new IllegalArgumentException("invalid repair pricing");
        }
        if (damage == 0) {
            return 0;
        }
        long scaled = Math.multiplyExact(fullDamageCost, damage);
        long rounded = Math.addExact(scaled, maxDamage - 1L) / maxDamage;
        return Math.addExact(base, rounded);
    }
}
