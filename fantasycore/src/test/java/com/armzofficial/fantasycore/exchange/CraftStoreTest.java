package com.armzofficial.fantasycore.exchange;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.item.ItemInstanceStore;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CraftStoreTest {
    private Database db;
    private MailStore mail;
    private EconomyStore money;
    private ExchangeStore craft;
    private ExchangeStore exchange;
    private final UUID player = UUID.randomUUID();
    private final AuditEntry decision = new AuditEntry(null, "console", "craft.resolve", "player", "frozen output", "ตรวจ playerdata แล้ว");

    @BeforeEach void setup() throws Exception {
        db = TestDatabases.fresh();
        mail = new MailStore(db, () -> 1000L);
        money = new EconomyStore(db, () -> 1000L, Long.MAX_VALUE);
        craft = new ExchangeStore(db, mail, () -> 1000L, money, ExchangeStore.Kind.CRAFT);
        exchange = new ExchangeStore(db, mail, () -> 1000L);
        adjust(1000);
    }

    @AfterEach void close() { db.close(); }

    private void adjust(long delta) throws Exception {
        assertTrue(money.adjust(player, Bucket.GOLD_WALLET, delta, null,
                OpMeta.of("admin.adjust", "console", "test funding"), null).ok());
    }

    private ExchangeStore.Request request(String op) {
        return request(op, UUID.randomUUID(), 200, 3);
    }

    private ExchangeStore.Request request(String op, UUID serial, long price, int limit) {
        return new ExchangeStore.Request(op, player, "starter_runeblade", 2, 1, "2026-10-05", limit,
                "IRON_INGOT ×12", new byte[]{1, 2},
                List.of(new ExchangeStore.Output("blade v1", new byte[]{3, 4}, serial, "starter_runeblade", 1)), price);
    }

    private String state(String op) throws Exception {
        return db.read(c -> {
            try (var ps = c.prepareStatement("SELECT state FROM exchange_operations WHERE op_id = ?")) {
                ps.setString(1, op);
                try (var rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
            }
        });
    }

    @Test void chargeAndCompletionRegisterOwnerAndMailExactlyOnce() throws Exception {
        var r = request("one");
        UUID serial = r.outputs().getFirst().serial();
        assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(r));
        assertEquals(800, money.balances(player).gold());
        assertTrue(new ItemInstanceStore(db).find(serial).isEmpty());
        assertFalse(craft.complete("one").changed());
        assertTrue(craft.beginConsume("one"));
        var completion = craft.complete("one");
        assertTrue(completion.changed());
        assertFalse(craft.complete("one").changed());
        assertEquals(1, mail.countPending(player));
        assertArrayEquals(new byte[]{3, 4}, mail.pending(player, 10).getFirst().data());
        var instance = new ItemInstanceStore(db).find(serial).orElseThrow();
        assertEquals(player, instance.owner());
        assertEquals("starter_runeblade", instance.templateId());
        assertEquals(1, instance.version());
        assertEquals("MAILED", instance.state());
        assertEquals("craft", mail.pending(player, 10).getFirst().source());
        assertEquals(800, money.balances(player).gold());
    }

    @Test void insufficientWalletNeverUsesBankOrCreatesJournal() throws Exception {
        assertTrue(money.adjust(player, Bucket.GOLD_BANK, 5000, null, OpMeta.of("admin.adjust", "console", "bank funding"), null).ok());
        assertEquals(ExchangeStore.PrepareResult.FUNDS, craft.prepare(request("poor", UUID.randomUUID(), 1001, 3)));
        assertNull(state("poor"));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(5000, money.balances(player).bank());
        assertEquals(0, mail.countPending(player));
    }

    @Test void priceOverTransactionLimitDoesNotReserve() throws Exception {
        var limited = new ExchangeStore(db, mail, () -> 1000L, new EconomyStore(db, () -> 1000L, 100), ExchangeStore.Kind.CRAFT);
        assertEquals(ExchangeStore.PrepareResult.LIMIT, limited.prepare(request("limit")));
        assertNull(state("limit"));
        assertEquals(1000, money.balances(player).gold());
    }

    @Test void duplicatePrepareNeverChargesAgain() throws Exception {
        assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(request("duplicate")));
        assertEquals(ExchangeStore.PrepareResult.DUPLICATE, craft.prepare(request("duplicate")));
        assertEquals(800, money.balances(player).gold());
        assertEquals(1, craft.usage(player, "2026-10-05").get("starter_runeblade"));
    }

    @Test void cancelRefundAddsToCurrentWalletAndFreesQuotaOnce() throws Exception {
        craft.prepare(request("cancel"));
        adjust(75);
        assertTrue(craft.cancelUntouched("cancel"));
        assertFalse(craft.cancelUntouched("cancel"));
        assertEquals(1075, money.balances(player).gold());
        assertTrue(craft.usage(player, "2026-10-05").isEmpty());
        assertEquals(0, mail.countPending(player));
    }

    @Test void startupRefundsPreparedButQuarantinesConsumingWithoutAutomaticItemsOrRefund() throws Exception {
        craft.prepare(request("untouched"));
        var recovered = craft.quarantineInterrupted();
        assertEquals(1, recovered.cancelled());
        assertEquals(1000, money.balances(player).gold());
        craft.prepare(request("uncertain"));
        craft.beginConsume("uncertain");
        assertEquals(1, craft.quarantineInterrupted().review());
        assertEquals("REVIEW", state("uncertain"));
        assertEquals(800, money.balances(player).gold());
        assertEquals(0, mail.countPending(player));
        assertEquals(0, craft.quarantineInterrupted().cancelled());
        assertEquals(1, craft.countReview());
        assertEquals(0, exchange.countReview());
    }

    @Test void reviewCompletionUsesStoredBytesAndSerialWithNoAdditionalCharge() throws Exception {
        var r = request("review");
        craft.prepare(r);
        craft.beginConsume("review");
        craft.markReview("review");
        assertEquals(200, craft.findReview("review").orElseThrow().goldCost());
        assertTrue(craft.findReview("review").orElseThrow().outputs().contains(r.outputs().getFirst().serial().toString()));
        assertTrue(craft.resolveReview("review", true, decision).changed());
        assertFalse(craft.resolveReview("review", true, decision).changed());
        assertEquals(1, mail.countPending(player));
        assertArrayEquals(r.outputs().getFirst().data(), mail.pending(player, 10).getFirst().data());
        assertTrue(new ItemInstanceStore(db).find(r.outputs().getFirst().serial()).isPresent());
        assertEquals(800, money.balances(player).gold());
    }

    @Test void reviewCancellationRefundsOnceWithoutGrantingOutput() throws Exception {
        var r = request("cancel_review");
        craft.prepare(r);
        craft.beginConsume(r.opId());
        craft.markReview(r.opId());
        assertTrue(craft.resolveReview(r.opId(), false, decision).changed());
        assertFalse(craft.resolveReview(r.opId(), false, decision).changed());
        assertEquals(1000, money.balances(player).gold());
        assertEquals(0, mail.countPending(player));
        assertTrue(new ItemInstanceStore(db).find(r.outputs().getFirst().serial()).isEmpty());
    }

    @Test void exchangeCannotTransitionResolveOrRecoverCraftRows() throws Exception {
        craft.prepare(request("separate"));
        assertFalse(exchange.beginConsume("separate"));
        assertFalse(exchange.cancelUntouched("separate"));
        assertEquals(0, exchange.quarantineInterrupted().cancelled());
        assertEquals("PREPARED", state("separate"));
        craft.beginConsume("separate");
        craft.markReview("separate");
        assertTrue(exchange.findReview("separate").isEmpty());
        assertFalse(exchange.resolveReview("separate", true, decision).changed());
        assertEquals(800, money.balances(player).gold());
    }

    @Test void quotaIsSeparateFromExchangeAndBusyGuardCoversBothServices() throws Exception {
        var legacy = new ExchangeStore.Request("legacy", player, "starter_runeblade", 1, 1, "2026-10-05", 1,
                "IRON_INGOT ×1", new byte[]{1}, List.of(new ExchangeStore.Output("iron", new byte[]{2})));
        exchange.prepare(legacy);
        assertEquals(ExchangeStore.PrepareResult.BUSY, craft.prepare(request("busy")));
        exchange.beginConsume("legacy");
        exchange.complete("legacy");
        assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(request("first", UUID.randomUUID(), 200, 1)));
        craft.beginConsume("first"); craft.complete("first");
        assertEquals(ExchangeStore.PrepareResult.QUOTA, craft.prepare(request("second", UUID.randomUUID(), 200, 1)));
        assertEquals(800, money.balances(player).gold());
    }

    @Test void concurrentConfirmsReserveOnlyOneCostAndOutput() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Future<ExchangeStore.PrepareResult>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String op = "parallel_" + i;
                results.add(pool.submit(() -> craft.prepare(request(op))));
            }
            int prepared = 0;
            for (var future : results) { if (future.get() == ExchangeStore.PrepareResult.PREPARED) { prepared++; } }
            assertEquals(1, prepared);
            assertEquals(800, money.balances(player).gold());
        }
    }

    @Test void outputSerialConflictRollsBackChargeAndJournal() throws Exception {
        UUID serial = UUID.randomUUID();
        craft.prepare(request("old_serial", serial, 200, 3));
        craft.cancelUntouched("old_serial");
        assertThrows(SQLException.class, () -> craft.prepare(request("same_serial", serial, 200, 3)));
        assertNull(state("same_serial"));
        assertEquals(1000, money.balances(player).gold());
    }

    @Test void registryFailureRollsBackMailAndCommitStateForManualReview() throws Exception {
        var r = request("registry_failure");
        craft.prepare(r); craft.beginConsume(r.opId());
        new ItemInstanceStore(db).issue(r.outputs().getFirst().serial(), "other", 1, player, "conflicting_admin_issue", "test", null, 1000);
        assertThrows(SQLException.class, () -> craft.complete(r.opId()));
        assertEquals("CONSUMING", state(r.opId()));
        assertEquals(0, mail.countPending(player));
        assertEquals(800, money.balances(player).gold());
        assertTrue(craft.markReview(r.opId()));
    }

    @Test void auditFailureRollsBackRegistryMailAndCommitTogether() throws Exception {
        var r = request("audit_failure");
        craft.prepare(r); craft.beginConsume(r.opId());
        db.transaction(c -> {
            try (var st = c.createStatement()) {
                st.execute("CREATE TRIGGER reject_craft_audit BEFORE INSERT ON audit_log WHEN NEW.action = 'craft.commit' BEGIN SELECT RAISE(ABORT, 'test audit unavailable'); END");
            }
            return null;
        });
        assertThrows(SQLException.class, () -> craft.complete(r.opId()));
        assertEquals("CONSUMING", state(r.opId()));
        assertTrue(new ItemInstanceStore(db).find(r.outputs().getFirst().serial()).isEmpty());
        assertEquals(0, mail.countPending(player));
        assertEquals(800, money.balances(player).gold());
    }

    @Test void snapshotAndResultBytesAreFrozenAcrossCallerMutation() throws Exception {
        byte[] snapshot = {1};
        byte[] result = {2};
        var r = new ExchangeStore.Request("frozen", player, "blade", 1, 1, "2026-10-05", 3, "IRON_INGOT ×12", snapshot,
                List.of(new ExchangeStore.Output("blade", result, UUID.randomUUID(), "starter_runeblade", 1)), 200);
        snapshot[0] = 9; result[0] = 9;
        r.snapshot()[0] = 9; r.outputs().getFirst().data()[0] = 9;
        assertArrayEquals(new byte[]{1}, r.snapshot());
        craft.prepare(r); craft.beginConsume(r.opId()); craft.complete(r.opId());
        assertArrayEquals(new byte[]{2}, mail.pending(player, 10).getFirst().data());
    }

    @Test void frozenOldAndNewTemplateVersionsKeepDistinctSerialsAndShareRecipeQuota() throws Exception {
        var registry = new ItemInstanceStore(db);
        for (int version = 1; version <= 2; version++) {
            UUID serial = UUID.randomUUID();
            var r = new ExchangeStore.Request("revision_" + version, player, "starter_runeblade", version, 1,
                    "2026-10-05", 2, "IRON_INGOT ×12", new byte[]{1},
                    List.of(new ExchangeStore.Output("blade v" + version, new byte[]{(byte) version}, serial, "starter_runeblade", version)), 200);
            assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(r));
            assertTrue(craft.beginConsume(r.opId()));
            assertTrue(craft.complete(r.opId()).changed());
            var registered = registry.find(serial).orElseThrow();
            assertEquals(version, registered.version());
            assertEquals(serial, registered.serial());
            assertEquals(player, registered.owner());
        }
        assertEquals(2, mail.countPending(player));
        var versions = mail.pending(player, 10).stream().map(row -> (int) row.data()[0]).sorted().toList();
        assertEquals(List.of(1, 2), versions);
        assertEquals(2, craft.usage(player, "2026-10-05").get("starter_runeblade"));
        assertEquals(ExchangeStore.PrepareResult.QUOTA, craft.prepare(request("third_revision", UUID.randomUUID(), 200, 2)));
        assertEquals(600, money.balances(player).gold());
    }

    @Test void refundOverflowKeepsReservationAndFailsClosed() throws Exception {
        craft.prepare(request("overflow"));
        adjust(Long.MAX_VALUE - 800);
        assertThrows(SQLException.class, () -> craft.cancelUntouched("overflow"));
        assertEquals("PREPARED", state("overflow"));
        assertEquals(Long.MAX_VALUE, money.balances(player).gold());
        assertThrows(SQLException.class, () -> craft.quarantineInterrupted());
        assertEquals("PREPARED", state("overflow"));
    }

    @Test void rejectsNonSerializedBatchOutputAndAllowsZeroCostWithIngredients() throws Exception {
        var bad = new ExchangeStore.Request("bad", player, "bad", 1, 2, "2026-10-05", 3,
                "IRON_INGOT ×1", new byte[]{1}, List.of(new ExchangeStore.Output("iron", new byte[]{2})), 10);
        assertThrows(IllegalArgumentException.class, () -> craft.prepare(bad));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(request("free", UUID.randomUUID(), 0, 3)));
        craft.beginConsume("free"); craft.complete("free");
        assertEquals(1000, money.balances(player).gold());
        assertEquals(1, mail.countPending(player));
    }

    @Test void migrationFromV4PreservesExistingMoneyMailExchangeAndRepair() throws Exception {
        exchange.prepare(new ExchangeStore.Request("prior", player, "food", 1, 1, "2026-10-05", 10,
                "WHEAT ×1", new byte[]{1}, List.of(new ExchangeStore.Output("bread", new byte[]{2}))));
        exchange.beginConsume("prior"); exchange.complete("prior");
        db.transaction(c -> {
            TestDatabases.removeCraftColumns(c);
            try (var st = c.createStatement()) {
                st.execute("UPDATE schema_version SET version = 4");
                st.execute("INSERT INTO repair_operations VALUES ('old_repair', '" + player + "', 'vanilla', NULL, 0, NULL, 'IRON_SWORD', 0, 1, 250, 50, X'01', X'02', 'COMMITTED', 1, 1)");
            }
            return null;
        });
        assertEquals(5, Migrations.apply(db));
        assertEquals(5, Migrations.apply(db));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(1, mail.countPending(player));
        assertEquals(1, exchange.usage(player, "2026-10-05").get("food"));
        assertEquals(1, (int) db.read(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM repair_operations WHERE op_id = 'old_repair'")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }));
        assertEquals(ExchangeStore.PrepareResult.PREPARED, craft.prepare(request("after_upgrade")));
    }
}
