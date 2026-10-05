package com.armzofficial.fantasycore.exchange;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.audit.AuditEntry;
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

class ExchangeStoreTest {
    private Database db;
    private MailStore mail;
    private ExchangeStore store;
    private final UUID player = UUID.randomUUID();
    private final AuditEntry decision = new AuditEntry(null, "console", "quest.exchange.resolve", "player", "review", "ตรวจ playerdata แล้ว");

    @BeforeEach
    void setup() throws Exception {
        db = TestDatabases.fresh();
        mail = new MailStore(db, () -> 1000L);
        store = new ExchangeStore(db, mail, () -> 1000L);
    }

    @AfterEach
    void close() {
        db.close();
    }

    private ExchangeStore.Request request(UUID owner, String op, String period, int version, int batch, int limit) {
        return new ExchangeStore.Request(op, owner, "food_bundle", version, batch, period, limit, "WHEAT ×32",
                new byte[]{1, 2}, List.of(new ExchangeStore.Output("bread ×8", new byte[]{3, 4})));
    }

    private ExchangeStore.Request request(String op) {
        return request(player, op, "2026-10-05", 1, 1, 16);
    }

    @Test
    void commitRequiresConsumeAndQueuesFrozenOutputExactlyOnce() throws Exception {
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request("one")));
        assertFalse(store.complete("one").changed());
        assertEquals(0, mail.countPending(player));
        assertTrue(store.beginConsume("one"));
        assertFalse(store.beginConsume("one"));
        assertTrue(store.complete("one").changed());
        assertFalse(store.complete("one").changed());
        assertEquals(1, mail.countPending(player));
        var item = mail.pending(player, 10).getFirst();
        assertArrayEquals(new byte[]{3, 4}, item.data());
        assertEquals("one", item.sourceRef());
        assertEquals(ExchangeStore.PrepareResult.DUPLICATE, store.prepare(request("one")));
    }

    @Test
    void reservationCountsTowardQuotaAcrossRecipeVersions() throws Exception {
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request(player, "one", "2026-10-05", 1, 2, 3)));
        assertEquals(ExchangeStore.PrepareResult.BUSY, store.prepare(request("two")));
        assertTrue(store.beginConsume("one"));
        store.complete("one");
        assertEquals(2, store.usage(player, "2026-10-05").get("food_bundle"));
        assertEquals(ExchangeStore.PrepareResult.QUOTA, store.prepare(request(player, "two", "2026-10-05", 2, 2, 3)));
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request(player, "three", "2026-10-06", 2, 2, 3)));
    }

    @Test
    void untouchedCancellationReleasesQuotaWithoutReward() throws Exception {
        store.prepare(request("one"));
        assertTrue(store.beginConsume("one"));
        assertTrue(store.cancelUntouched("one"));
        assertFalse(store.cancelUntouched("one"));
        assertFalse(store.complete("one").changed());
        assertTrue(store.usage(player, "2026-10-05").isEmpty());
        assertEquals(0, mail.countPending(player));
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request("two")));
    }

    @Test
    void restartCancelsPreparedAndQuarantinesConsumingWithoutAutoReward() throws Exception {
        UUID other = UUID.randomUUID();
        store.prepare(request("prepared"));
        store.prepare(request(other, "consuming", "2026-10-05", 1, 1, 16));
        store.beginConsume("consuming");
        assertEquals(new ExchangeStore.Recovery(1, 1), store.quarantineInterrupted());
        assertEquals(new ExchangeStore.Recovery(0, 0), store.quarantineInterrupted());
        assertEquals(1, store.countReview());
        assertEquals(other, store.review(20).getFirst().player());
        assertEquals(0, mail.countPending(other));
        assertEquals(ExchangeStore.PrepareResult.BUSY, store.prepare(request(other, "blocked", "2026-10-06", 1, 1, 16)));
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request("new")));
    }

    @Test
    void reviewCompletionAndCancelAreIdempotentAndRequireReason() throws Exception {
        store.prepare(request("one"));
        store.beginConsume("one");
        store.markReview("one");
        assertTrue(store.findReview("one").isPresent());
        assertThrows(IllegalArgumentException.class, () -> store.resolveReview("one", true, null));
        assertTrue(store.resolveReview("one", true, decision).changed());
        assertFalse(store.resolveReview("one", true, decision).changed());
        assertFalse(store.resolveReview("one", false, decision).changed());
        assertEquals(1, mail.countPending(player));
        assertEquals(1, store.usage(player, "2026-10-05").get("food_bundle"));
        store.prepare(request("two"));
        store.beginConsume("two");
        store.markReview("two");
        assertTrue(store.resolveReview("two", false, decision).changed());
        assertEquals(1, mail.countPending(player));
        assertEquals(1, store.usage(player, "2026-10-05").get("food_bundle"));
    }

    @Test
    void mailboxFailureRollsBackCommitAndCanBeReviewed() throws Exception {
        store.prepare(request("one"));
        store.beginConsume("one");
        db.transaction(c -> {
            try (var st = c.createStatement()) {
                st.execute("CREATE TRIGGER fail_mail BEFORE INSERT ON mail BEGIN SELECT RAISE(ABORT, 'test outage'); END");
            }
            return null;
        });
        assertThrows(SQLException.class, () -> store.complete("one"));
        assertEquals(0, mail.countPending(player));
        assertTrue(store.markReview("one"));
        assertThrows(SQLException.class, () -> store.resolveReview("one", true, decision));
        assertTrue(store.isReview("one"));
        db.transaction(c -> {
            try (var st = c.createStatement()) { st.execute("DROP TRIGGER fail_mail"); }
            return null;
        });
        assertTrue(store.resolveReview("one", true, decision).changed());
        assertEquals(1, mail.countPending(player));
    }

    @Test
    void concurrentReservationsAllowOnlyOneActiveOperationPerPlayer() throws Exception {
        List<Future<ExchangeStore.PrepareResult>> futures = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 20; i++) {
                String op = "parallel-" + i;
                futures.add(pool.submit(() -> store.prepare(request(op))));
            }
            int prepared = 0;
            for (var future : futures) {
                var result = future.get();
                assertTrue(result == ExchangeStore.PrepareResult.PREPARED || result == ExchangeStore.PrepareResult.BUSY);
                if (result == ExchangeStore.PrepareResult.PREPARED) { prepared++; }
            }
            assertEquals(1, prepared);
        }
    }

    @Test
    void migrationFromV2KeepsExistingAccountAndMail() throws Exception {
        long mailId = mail.enqueue(player, "test", "old", "bread", new byte[]{7}, null);
        db.transaction(c -> {
            try (var st = c.createStatement()) {
                st.execute("DROP TABLE exchange_outputs");
                st.execute("DROP TABLE exchange_operations");
                st.execute("DROP TABLE repair_operations");
                st.execute("UPDATE schema_version SET version = 2");
                st.execute("INSERT INTO accounts VALUES ('" + player + "', 'gold.wallet', 1234, 1000)");
            }
            return null;
        });
        assertEquals(Migrations.latestVersion(), Migrations.apply(db));
        assertEquals(1234L, db.read(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT balance FROM accounts")) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }).longValue());
        assertEquals(mailId, mail.pending(player, 10).getFirst().id());
        assertEquals(ExchangeStore.PrepareResult.PREPARED, store.prepare(request("new")));
    }

    @Test
    void malformedRequestsFailBeforeAnyReservation() {
        assertThrows(IllegalArgumentException.class, () -> store.prepare(request(player, "bad", "2026-10-05", 1, 17, 16)));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(request(player, "bad", "2026-10-05", 0, 1, 16)));
        assertThrows(IllegalArgumentException.class, () -> store.prepare(request(player, "bad", "invalid", 1, 1, 16)));
    }

    @Test
    void mailboxRemainderAndClaimStateCommitOrRollbackTogether() throws Exception {
        long id = mail.enqueue(player, "test", "original", "bread", new byte[]{1}, null);
        assertTrue(mail.beginClaim(id, player, "claim"));
        db.transaction(c -> {
            try (var st = c.createStatement()) {
                st.execute("CREATE TRIGGER fail_remainder BEFORE INSERT ON mail BEGIN SELECT RAISE(ABORT, 'test outage'); END");
            }
            return null;
        });
        assertThrows(SQLException.class, () -> mail.finishClaimWithRemainder(id, "claim", player, "test", "original", "bread",
                List.of(new byte[]{2})));
        assertEquals(0, mail.countPending(player));
        db.transaction(c -> {
            try (var st = c.createStatement()) { st.execute("DROP TRIGGER fail_remainder"); }
            return null;
        });
        mail.finishClaimWithRemainder(id, "claim", player, "test", "original", "bread", List.of(new byte[]{2}));
        assertEquals(1, mail.countPending(player));
        assertArrayEquals(new byte[]{2}, mail.pending(player, 10).getFirst().data());
        assertThrows(SQLException.class, () -> mail.finishClaimWithRemainder(id, "claim", player, "test", "original", "bread",
                List.of(new byte[]{2})));
        assertEquals(1, mail.countPending(player));
    }
}
