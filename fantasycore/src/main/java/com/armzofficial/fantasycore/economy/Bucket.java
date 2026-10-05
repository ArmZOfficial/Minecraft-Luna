package com.armzofficial.fantasycore.economy;

import java.util.Locale;
import java.util.Optional;

/** ช่องเก็บเงินของผู้เล่นตาม SERVER-SYSTEMS-PLAN §7: gold.wallet / gold.bank / red.wallet */
public enum Bucket {
    GOLD_WALLET("gold.wallet"),
    GOLD_BANK("gold.bank"),
    RED_WALLET("red.wallet");

    private final String key;

    Bucket(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** รับชื่อสั้นจากคำสั่งแอดมิน: gold / bank / red หรือชื่อเต็ม gold.wallet */
    public static Optional<Bucket> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "gold", "wallet", "gold.wallet" -> Optional.of(GOLD_WALLET);
            case "bank", "gold.bank" -> Optional.of(GOLD_BANK);
            case "red", "red.wallet" -> Optional.of(RED_WALLET);
            default -> Optional.empty();
        };
    }

    public static Bucket fromKey(String key) {
        for (Bucket bucket : values()) {
            if (bucket.key.equals(key)) {
                return bucket;
            }
        }
        throw new IllegalArgumentException("unknown bucket " + key);
    }
}
