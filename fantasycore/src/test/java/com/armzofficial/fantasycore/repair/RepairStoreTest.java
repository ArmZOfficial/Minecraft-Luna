package com.armzofficial.fantasycore.repair;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.item.ItemIdentityPolicy;
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

class RepairStoreTest {
    private Database db;
    private EconomyStore money;
    private RepairStore repair;
    private ItemInstanceStore items;
    private final UUID player = UUID.randomUUID();
    private final ItemIdentityPolicy.Identity vanilla = new ItemIdentityPolicy.Identity("vanilla", null, 0, null);
    private final AuditEntry decision = new AuditEntry(null, "console", "item.repair.resolve", "player", "review", "ตรวจ playerdata แล้ว");

    @BeforeEach
    void setup() throws Exception {
        db = TestDatabases.fresh();
        money = new EconomyStore(db, () -> 1000L, 1_000_000);
        repair = new RepairStore(db, money, () -> 1000L);
        items = new ItemInstanceStore(db);
        fund(player);
    }

    @AfterEach
    void close() { db.close(); }

    private void fund(UUID owner) throws Exception {
        assertTrue(money.adjust(owner, Bucket.GOLD_WALLET, 1000, null, OpMeta.of("test.credit", "test", "fund"), null).ok());
    }

    private RepairStore.Request request(String op) {
        return request(player, op, vanilla, 500);
    }

    private RepairStore.Request request(UUID owner, String op, ItemIdentityPolicy.Identity identity, long cost) {
        return new RepairStore.Request(op, owner, identity, "IRON_SWORD", 0, 249, 250, cost, new byte[]{1, 2}, new byte[]{1, 0});
    }

    @Test
    void chargeAndCompletionAreIdempotentAndOnlyUseGoldWallet() throws Exception {
        money.adjust(player, Bucket.GOLD_BANK, 200, null, OpMeta.of("test.credit", "test", "bank"), null);
        money.adjust(player, Bucket.RED_WALLET, 30, null, OpMeta.of("test.credit", "test", "red"), null);
        assertEquals(RepairStore.Status.RESERVED, repair.reserve(request("one")).status());
        assertEquals(500, money.balances(player).gold());
        assertEquals(RepairStore.Status.DUPLICATE, repair.reserve(request("one")).status());
        assertEquals(RepairStore.Status.BUSY, repair.reserve(request("two")).status());
        assertFalse(repair.complete("one"));
        assertTrue(repair.beginApply("one"));
        assertFalse(repair.beginApply("one"));
        assertTrue(repair.complete("one"));
        assertFalse(repair.complete("one"));
        assertFalse(repair.cancelUntouched("one"));
        assertEquals(500, money.balances(player).gold());
        assertEquals(200, money.balances(player).bank());
        assertEquals(30, money.balances(player).red());
    }

    @Test
    void cancellationRefundsExactlyOnceEvenAfterWalletChanges() throws Exception {
        repair.reserve(request("one"));
        money.adjust(player, Bucket.GOLD_WALLET, -100, null, OpMeta.of("test.spend", "test", "other purchase"), null);
        assertTrue(repair.cancelUntouched("one"));
        assertFalse(repair.cancelUntouched("one"));
        assertEquals(900, money.balances(player).gold());
        assertEquals(1, money.history(player, 100).stream().filter(l -> l.opId().equals("one:refund")).count());
    }

    @Test
    void insufficientFundsAndInvalidRequestsHaveNoPaidOperation() throws Exception {
        assertEquals(RepairStore.Status.FUNDS, repair.reserve(request(player, "poor", vanilla, 1001)).status());
        assertEquals(RepairStore.Status.LIMIT, repair.reserve(request(player, "large", vanilla, 1_000_001)).status());
        assertThrows(IllegalArgumentException.class, () -> repair.reserve(request(player, "zero", vanilla, 0)));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(1, money.history(player, 100).size());
    }

    @Test
    void chargeRollsBackWhenJournalInsertFails() throws Exception {
        db.transaction(c -> {
            try (var st = c.createStatement()) { st.execute("CREATE TRIGGER fail_repair BEFORE INSERT ON repair_operations BEGIN SELECT RAISE(ABORT, 'test outage'); END"); }
            return null;
        });
        assertThrows(SQLException.class, () -> repair.reserve(request("one")));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(1, money.history(player, 100).size());
        db.transaction(c -> {
            try (var st = c.createStatement()) { st.execute("DROP TRIGGER fail_repair"); }
            return null;
        });
        assertEquals(RepairStore.Status.RESERVED, repair.reserve(request("one")).status());
    }

    @Test
    void restartRefundsReservedAndQuarantinesApplyingWithoutRefund() throws Exception {
        UUID other = UUID.randomUUID();
        fund(other);
        repair.reserve(request("reserved"));
        repair.reserve(request(other, "applying", vanilla, 500));
        repair.beginApply("applying");
        assertEquals(new RepairStore.Recovery(1, 1), repair.quarantineInterrupted());
        assertEquals(new RepairStore.Recovery(0, 0), repair.quarantineInterrupted());
        assertEquals(1000, money.balances(player).gold());
        assertEquals(500, money.balances(other).gold());
        assertEquals(1, repair.countReview());
        assertEquals(RepairStore.Status.BUSY, repair.reserve(request(other, "blocked", vanilla, 500)).status());
    }

    @Test
    void reviewDecisionIsReasonedIdempotentAndDoesNotRepairOrMintItems() throws Exception {
        repair.reserve(request("one"));
        repair.beginApply("one");
        repair.markReview("one");
        assertThrows(IllegalArgumentException.class, () -> repair.resolveReview("one", false, null));
        assertTrue(repair.resolveReview("one", false, decision));
        assertFalse(repair.resolveReview("one", false, decision));
        assertFalse(repair.resolveReview("one", true, decision));
        assertEquals(1000, money.balances(player).gold());
        repair.reserve(request("two"));
        repair.beginApply("two");
        repair.markReview("two");
        assertTrue(repair.resolveReview("two", true, decision));
        assertEquals(500, money.balances(player).gold());
        assertEquals(0, new MailStore(db, () -> 1000L).countPending(player));
    }

    @Test
    void refundAndReviewStateRollBackTogetherIfWalletCannotReceiveCredit() throws Exception {
        repair.reserve(request("one"));
        repair.beginApply("one");
        repair.markReview("one");
        db.transaction(c -> {
            try (var st = c.createStatement()) { st.execute("UPDATE accounts SET balance = " + Long.MAX_VALUE + " WHERE bucket = 'gold.wallet'"); }
            return null;
        });
        assertThrows(SQLException.class, () -> repair.resolveReview("one", false, decision));
        assertTrue(repair.findReview("one").isPresent());
        assertEquals(Long.MAX_VALUE, money.balances(player).gold());
    }

    @Test
    void coreRegistryIsRecheckedBeforeApplyAndPendingMailCannotBeRepaired() throws Exception {
        UUID serial = UUID.randomUUID();
        items.issue(serial, "starter_runeblade", 1, player, "issued", "test", null, 1000);
        var identity = new ItemIdentityPolicy.Identity("core", "starter_runeblade", 1, serial);
        assertEquals(RepairStore.Status.INVALID_ITEM, repair.reserve(request(player, "not-delivered", identity, 500)).status());
        items.setState(serial, "MAILED", 1000);
        MailStore mail = new MailStore(db, () -> 1000L);
        long mailId = mail.enqueue(player, "admin.item", "issued", "sword", new byte[]{1}, null);
        assertFalse(repair.validIdentity(player, identity));
        mail.beginClaim(mailId, player, "claim");
        mail.finishClaim(mailId, "claim");
        assertTrue(repair.validIdentity(player, identity));
        assertEquals(RepairStore.Status.RESERVED, repair.reserve(request(player, "valid", identity, 500)).status());
        items.setState(serial, "DELIVERY_FAILED", 1000);
        assertFalse(repair.beginApply("valid"));
        assertTrue(repair.cancelUntouched("valid"));
        assertEquals(1000, money.balances(player).gold());
    }

    @Test
    void concurrentReservationsChargeOneRequestOnly() throws Exception {
        List<Future<RepairStore.Reservation>> work = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 20; i++) {
                String op = "parallel-" + i;
                work.add(pool.submit(() -> repair.reserve(request(op))));
            }
            int reserved = 0;
            for (var f : work) {
                var status = f.get().status();
                assertTrue(status == RepairStore.Status.RESERVED || status == RepairStore.Status.BUSY);
                if (status == RepairStore.Status.RESERVED) { reserved++; }
            }
            assertEquals(1, reserved);
            assertEquals(500, money.balances(player).gold());
        }
    }

    @Test
    void migrationFromV3PreservesMoneyAndExistingExchangeReceipts() throws Exception {
        var mail = new MailStore(db, () -> 1000L);
        var exchange = new com.armzofficial.fantasycore.exchange.ExchangeStore(db, mail, () -> 1000L);
        exchange.prepare(new com.armzofficial.fantasycore.exchange.ExchangeStore.Request("old", player, "food_bundle", 1, 1,
                "2026-10-05", 16, "WHEAT ×32", new byte[]{1}, List.of(new com.armzofficial.fantasycore.exchange.ExchangeStore.Output("bread", new byte[]{2}))));
        exchange.beginConsume("old");
        exchange.complete("old");
        db.transaction(c -> {
            TestDatabases.removeCraftColumns(c);
            try (var st = c.createStatement()) {
                st.execute("DROP TABLE repair_operations");
                st.execute("UPDATE schema_version SET version = 3");
            }
            return null;
        });
        assertEquals(Migrations.latestVersion(), Migrations.apply(db));
        assertEquals(1000, money.balances(player).gold());
        assertEquals(1, mail.countPending(player));
        assertEquals(1, exchange.usage(player, "2026-10-05").get("food_bundle"));
        assertEquals(RepairStore.Status.RESERVED, repair.reserve(request("new" )).status());
    }
}
