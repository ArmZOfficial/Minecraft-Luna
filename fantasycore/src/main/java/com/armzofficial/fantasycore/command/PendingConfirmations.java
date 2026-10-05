package com.armzofficial.fantasycore.command;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * nonce ยืนยันงานแอดมิน (ADMIN-PANEL-SPEC: preview → apply): ผูกผู้สั่ง + หมดอายุ + ใช้ได้ครั้งเดียว
 */
public final class PendingConfirmations {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long TTL_MILLIS = 60_000;

    public record Pending(UUID actor, long expiresAt, BooleanSupplier allowed, Runnable apply) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public PendingConfirmations() { this(System::currentTimeMillis); }
    PendingConfirmations(LongSupplier clock) { this.clock = clock; }

    /** actor = null สำหรับ console */
    public String create(UUID actor, Runnable apply) {
        return create(actor, () -> true, apply);
    }

    public String create(UUID actor, BooleanSupplier allowed, Runnable apply) {
        pending.values().removeIf(p -> p.expiresAt() <= clock.getAsLong());
        String token;
        do {
            StringBuilder builder = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            token = builder.toString();
        } while (pending.containsKey(token));
        pending.put(token, new Pending(actor, clock.getAsLong() + TTL_MILLIS, allowed, apply));
        return token;
    }

    public enum Outcome {
        APPLIED, UNKNOWN, EXPIRED, WRONG_ACTOR, DENIED
    }

    public Outcome confirm(String token, UUID actor) {
        Pending entry = pending.get(token.toUpperCase());
        if (entry == null) {
            return Outcome.UNKNOWN;
        }
        if (!java.util.Objects.equals(entry.actor(), actor)) {
            return Outcome.WRONG_ACTOR;
        }
        // ลบก่อนทำ เพื่อให้ใช้ได้ครั้งเดียวแม้พิมพ์ซ้ำเร็ว ๆ
        if (!pending.remove(token.toUpperCase(), entry)) {
            return Outcome.UNKNOWN;
        }
        if (entry.expiresAt() <= clock.getAsLong()) {
            return Outcome.EXPIRED;
        }
        if (!entry.allowed().getAsBoolean()) { return Outcome.DENIED; }
        entry.apply().run();
        return Outcome.APPLIED;
    }

    public Optional<Pending> peek(String token) {
        return Optional.ofNullable(pending.get(token.toUpperCase()));
    }

    public boolean cancel(String token, UUID actor) {
        Pending entry = pending.get(token.toUpperCase());
        return entry != null && java.util.Objects.equals(entry.actor(), actor) && pending.remove(token.toUpperCase(), entry);
    }
    public void clear() { pending.clear(); }
}
