package com.armzofficial.fantasycore.economy;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ชั้นบริการเหนือ {@link EconomyStore}: งาน async, cache สำหรับแสดงผล และกันกดซ้ำต่อผู้เล่น
 * <p>
 * cache ใช้แสดงผลเท่านั้น (scoreboard/placeholder/เมนู) — การตัดสินใจเรื่องเงินอ่านจากฐานข้อมูลเสมอ
 */
public final class EconomyService {

    private final Database database;
    private final EconomyStore store;
    private final Map<UUID, Balances> cache = new ConcurrentHashMap<>();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();

    public EconomyService(Database database, EconomyStore store) {
        this.database = database;
        this.store = store;
    }

    public EconomyStore store() {
        return store;
    }

    public Optional<Balances> cached(UUID player) {
        return Optional.ofNullable(cache.get(player));
    }

    public void forget(UUID player) {
        cache.remove(player);
        busy.remove(player);
    }

    public void remember(UUID player, Balances balances) {
        if (balances != null) {
            cache.put(player, balances);
        }
    }

    public CompletableFuture<Balances> load(UUID player) {
        return database.async(() -> {
            Balances balances = store.balances(player);
            remember(player, balances);
            return balances;
        });
    }

    /** ผู้เล่นกำลังมีธุรกรรมค้างอยู่หรือไม่ (ใช้กันดับเบิลคลิกในเมนู) */
    public boolean isBusy(UUID player) {
        return busy.contains(player);
    }

    public CompletableFuture<TxResult> deposit(UUID player, long amount) {
        return guarded(player, () -> store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, amount,
                OpMeta.of("bank.deposit", player.toString(), "ฝากเงิน")));
    }

    public CompletableFuture<TxResult> withdraw(UUID player, long amount) {
        return guarded(player, () -> store.transfer(player, Bucket.GOLD_BANK, Bucket.GOLD_WALLET, amount,
                OpMeta.of("bank.withdraw", player.toString(), "ถอนเงิน")));
    }

    public CompletableFuture<TxResult> deathLoss(UUID player, int percent, String worldName) {
        // ตายเป็น event ครั้งเดียว ไม่ต้อง guard; op ID ใหม่ทุกครั้งที่ตาย
        return database.async(() -> {
            TxResult result = store.deathLoss(player, percent,
                    new OpMeta(OpMeta.newOpId(), "death.loss", "system", percent + "% ในโลก " + worldName, worldName));
            remember(player, result.after());
            return result;
        });
    }

    /** แอดมินปรับยอด — ต้องมี expectedBefore จากขั้น preview */
    public CompletableFuture<TxResult> adminAdjust(UUID player, Bucket bucket, long delta, long expectedBefore,
                                                   String opId, AuditEntry audit) {
        return database.async(() -> {
            TxResult result = store.adjust(player, bucket, delta, expectedBefore,
                    new OpMeta(opId, "admin.adjust", audit.actorUuid() == null ? audit.actorName() : audit.actorUuid(),
                            audit.reason(), null), audit);
            remember(player, result.after());
            return result;
        });
    }

    public CompletableFuture<Balances> balances(UUID player) {
        return database.async(() -> {
            Balances balances = store.balances(player);
            remember(player, balances);
            return balances;
        });
    }

    public CompletableFuture<List<LedgerEntry>> history(UUID player, int limit) {
        return database.async(() -> store.history(player, limit));
    }

    private CompletableFuture<TxResult> guarded(UUID player, Database.SqlCallable<TxResult> work) {
        if (!busy.add(player)) {
            return CompletableFuture.completedFuture(null);
        }
        return database.async(() -> {
            try {
                TxResult result = work.call();
                remember(player, result.after());
                return result;
            } finally {
                busy.remove(player);
            }
        });
    }

    /** ใช้โดย Vault provider (synchronous ตามสัญญา Vault) */
    public TxResult adjustSync(UUID player, long delta, String kind) throws SQLException {
        TxResult result = store.adjust(player, Bucket.GOLD_WALLET, delta, null,
                OpMeta.of(kind, "vault", "ผ่าน Vault API"), null);
        remember(player, result.after());
        return result;
    }

    public Balances balancesSync(UUID player) throws SQLException {
        Balances balances = store.balances(player);
        remember(player, balances);
        return balances;
    }
}
