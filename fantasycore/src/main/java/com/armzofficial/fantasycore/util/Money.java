package com.armzofficial.fantasycore.util;

import java.util.Locale;
import java.util.OptionalLong;

public final class Money {

    private Money() {
    }

    /** 1234567 → "1,234,567" */
    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    /**
     * อ่านจำนวนเงินที่ผู้เล่นพิมพ์: ตัวเลขล้วน คั่นด้วย , หรือ _ ได้ ต้องมากกว่า 0
     * ไม่รับทศนิยม ไม่รับค่าติดลบ
     */
    public static OptionalLong parsePositive(String input) {
        if (input == null) {
            return OptionalLong.empty();
        }
        String cleaned = input.trim().replace(",", "").replace("_", "");
        if (cleaned.isEmpty() || cleaned.length() > 18) {
            return OptionalLong.empty();
        }
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c < '0' || c > '9') {
                return OptionalLong.empty();
            }
        }
        long value = Long.parseLong(cleaned);
        return value > 0 ? OptionalLong.of(value) : OptionalLong.empty();
    }

    public static boolean isAll(String input) {
        if (input == null) {
            return false;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        return value.equals("all") || value.equals("ทั้งหมด");
    }
}
