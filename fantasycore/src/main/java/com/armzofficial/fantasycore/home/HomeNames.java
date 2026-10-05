package com.armzofficial.fantasycore.home;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ชื่อบ้าน 1–16 ตัวอักษร: ไทย, a-z, 0-9, _ และ - (CASUAL-SURVIVAL §9)
 * key = ตัวพิมพ์เล็กสำหรับค้นหา; display = ตามที่ผู้เล่นพิมพ์
 */
public final class HomeNames {

    public static final String DEFAULT = "home";
    public static final int MAX_LENGTH = 16;

    private static final Pattern ALLOWED = Pattern.compile("[\\p{IsThai}A-Za-z0-9_-]+");

    private HomeNames() {
    }

    public static Optional<Name> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String display = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC);
        int length = display.codePointCount(0, display.length());
        if (length < 1 || length > MAX_LENGTH) {
            return Optional.empty();
        }
        if (!ALLOWED.matcher(display).matches()) {
            return Optional.empty();
        }
        return Optional.of(new Name(display.toLowerCase(Locale.ROOT), display));
    }

    public record Name(String key, String display) {
    }
}
