package com.armzofficial.fantasycore.economy;

/** สูตรเสียเงินเมื่อตาย: เปอร์เซ็นต์ของ gold.wallet ปัดลง (SERVER-SYSTEMS-PLAN §7) */
public final class DeathPolicy {

    private DeathPolicy() {
    }

    /**
     * คำนวณ floor(balance × percent / 100) แบบไม่ล้นแม้ยอดใกล้ Long.MAX_VALUE
     */
    public static long loss(long balance, int percent) {
        if (balance <= 0 || percent <= 0) {
            return 0;
        }
        if (percent >= 100) {
            return balance;
        }
        return (balance / 100) * percent + (balance % 100) * percent / 100;
    }
}
