package com.armzofficial.fantasycore.economy;

public record LedgerEntry(long id, String opId, Bucket bucket, long delta, long balanceAfter,
                          String reason, String ref, long createdAt) {
}
