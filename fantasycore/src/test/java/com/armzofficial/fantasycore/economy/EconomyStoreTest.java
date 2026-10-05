package com.armzofficial.fantasycore.economy;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyStoreTest {

    private Database database;
    private EconomyStore store;
    private final AtomicLong clock = new AtomicLong(1_000);
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() throws Exception {
        database = TestDatabases.fresh();
        store = new EconomyStore(database, clock::incrementAndGet, 1_000_000_000L);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private void give(long amount) throws Exception {
        TxResult result = store.adjust(player, Bucket.GOLD_WALLET, amount, null, OpMeta.of("test.give", "test", "seed"), null);
        assertEquals(TxResult.Status.OK, result.status());
    }

    @Test
    void newPlayerHasZeroBalances() throws Exception {
        assertEquals(Balances.ZERO, store.balances(player));
    }

    @Test
    void depositMovesWithoutCreatingMoney() throws Exception {
        give(1_000);
        TxResult result = store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 300, OpMeta.of("bank.deposit", "p", "t"));
        assertEquals(TxResult.Status.OK, result.status());
        assertEquals(new Balances(700, 300, 0), result.after());
        assertEquals(1_000, result.after().gold() + result.after().bank());
    }

    @Test
    void cannotWithdrawMoreThanBank() throws Exception {
        give(500);
        store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 200, OpMeta.of("bank.deposit", "p", "t"));
        TxResult result = store.transfer(player, Bucket.GOLD_BANK, Bucket.GOLD_WALLET, 201, OpMeta.of("bank.withdraw", "p", "t"));
        assertEquals(TxResult.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(new Balances(300, 200, 0), store.balances(player));
    }

    @Test
    void depositAllUsesBalanceAtCommitTime() throws Exception {
        give(1_234);
        TxResult result = store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, EconomyStore.ALL, OpMeta.of("bank.deposit", "p", "t"));
        assertEquals(1_234, result.amount());
        assertEquals(new Balances(0, 1_234, 0), result.after());
        TxResult again = store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, EconomyStore.ALL, OpMeta.of("bank.deposit", "p", "t"));
        assertEquals(TxResult.Status.NOTHING, again.status());
    }

    @Test
    void sameOperationIdIsAppliedOnce() throws Exception {
        give(1_000);
        OpMeta meta = new OpMeta("fixed-op-1", "bank.deposit", "p", "double click", null);
        assertEquals(TxResult.Status.OK, store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 100, meta).status());
        TxResult second = store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 100, meta);
        assertEquals(TxResult.Status.DUPLICATE, second.status());
        assertEquals(new Balances(900, 100, 0), store.balances(player));
    }

    @Test
    void invalidAndOversizedAmountsAreRejected() throws Exception {
        give(100);
        assertEquals(TxResult.Status.INVALID_AMOUNT,
                store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 0, OpMeta.of("x", "p", "t")).status());
        assertEquals(TxResult.Status.INVALID_AMOUNT,
                store.adjust(player, Bucket.GOLD_WALLET, 0, null, OpMeta.of("x", "p", "t"), null).status());
        assertEquals(TxResult.Status.LIMIT_EXCEEDED,
                store.adjust(player, Bucket.GOLD_WALLET, 2_000_000_000L, null, OpMeta.of("x", "p", "t"), null).status());
        assertEquals(TxResult.Status.INSUFFICIENT_FUNDS,
                store.adjust(player, Bucket.GOLD_WALLET, -101, null, OpMeta.of("x", "p", "t"), null).status());
        assertEquals(new Balances(100, 0, 0), store.balances(player));
    }

    @Test
    void deathLossOnlyTouchesWalletAndRoundsDown() throws Exception {
        give(1_001);
        store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 1, OpMeta.of("bank.deposit", "p", "t"));
        store.adjust(player, Bucket.RED_WALLET, 7, null, OpMeta.of("reward", "system", "t"), null);
        TxResult result = store.deathLoss(player, 30, OpMeta.of("death.loss", "system", "test"));
        // 1000 × 30% = 300
        assertEquals(300, result.amount());
        assertEquals(new Balances(700, 1, 7), result.after());
        // 700 × 30% = 210; 3 × 30% = 0.9 → 0
        assertEquals(210, store.deathLoss(player, 30, OpMeta.of("death.loss", "system", "t")).amount());
        store.adjust(player, Bucket.GOLD_WALLET, -487, null, OpMeta.of("x", "p", "t"), null);
        TxResult tiny = store.deathLoss(player, 30, OpMeta.of("death.loss", "system", "t"));
        assertEquals(TxResult.Status.NOTHING, tiny.status());
        assertEquals(3, store.balances(player).gold());
    }

    @Test
    void adminAdjustRequiresUnchangedBalanceAndWritesAudit() throws Exception {
        give(50);
        AuditEntry audit = new AuditEntry(null, "console", "economy.adjust", player.toString(), "gold +10", "ชดเชยบัค");
        TxResult stale = store.adjust(player, Bucket.GOLD_WALLET, 10, 40L, OpMeta.of("admin.adjust", "console", "x"), audit);
        assertEquals(TxResult.Status.BALANCE_CHANGED, stale.status());
        TxResult ok = store.adjust(player, Bucket.GOLD_WALLET, 10, 50L, new OpMeta("admin-op-1", "admin.adjust", "console", "ชดเชยบัค", null), audit);
        assertEquals(TxResult.Status.OK, ok.status());
        assertEquals(60, ok.after().gold());
        List<AuditStore.Row> rows = new AuditStore(database, clock::get).recent(player.toString(), 10);
        assertEquals(1, rows.size());
        assertEquals("admin-op-1", rows.getFirst().opId());
    }

    @Test
    void ledgerRecordsEverySideOfATransfer() throws Exception {
        give(100);
        store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 40, OpMeta.of("bank.deposit", "p", "t"));
        List<LedgerEntry> history = store.history(player, 10);
        assertEquals(3, history.size());
        assertEquals(Bucket.GOLD_BANK, history.get(0).bucket());
        assertEquals(40, history.get(0).delta());
        assertEquals(Bucket.GOLD_WALLET, history.get(1).bucket());
        assertEquals(-40, history.get(1).delta());
        assertEquals(60, history.get(1).balanceAfter());
    }

    @Test
    void concurrentTransfersNeverOverdrawOrLoseMoney() throws Exception {
        give(1_000);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<TxResult>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            boolean deposit = i % 2 == 0;
            futures.add(pool.submit(() -> deposit
                    ? store.transfer(player, Bucket.GOLD_WALLET, Bucket.GOLD_BANK, 7, OpMeta.of("bank.deposit", "p", "t"))
                    : store.transfer(player, Bucket.GOLD_BANK, Bucket.GOLD_WALLET, 5, OpMeta.of("bank.withdraw", "p", "t"))));
        }
        for (Future<TxResult> future : futures) {
            TxResult result = future.get();
            assertTrue(result.status() == TxResult.Status.OK || result.status() == TxResult.Status.INSUFFICIENT_FUNDS);
        }
        pool.shutdown();
        Balances after = store.balances(player);
        assertTrue(after.gold() >= 0 && after.bank() >= 0);
        assertEquals(1_000, after.gold() + after.bank());
    }
}
