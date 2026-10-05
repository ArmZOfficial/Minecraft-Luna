package com.armzofficial.fantasycore.economy;

public record Balances(long gold, long bank, long red) {

    public static final Balances ZERO = new Balances(0, 0, 0);

    public long get(Bucket bucket) {
        return switch (bucket) {
            case GOLD_WALLET -> gold;
            case GOLD_BANK -> bank;
            case RED_WALLET -> red;
        };
    }
}
