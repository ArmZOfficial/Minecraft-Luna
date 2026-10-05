package com.armzofficial.fantasycore.dungeon;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class DungeonStoreTest {
    Database db;
    MailStore mail;
    DungeonStore store;
    UUID player=UUID.randomUUID();
    DungeonStore.ReturnPoint exit=new DungeonStore.ReturnPoint(UUID.randomUUID(),10.5,80,20.5,90,0);
    @BeforeEach void open() throws Exception { db=TestDatabases.fresh(); mail=new MailStore(db,() -> 1000); store=new DungeonStore(db,mail,() -> 1000); }
    @AfterEach void close() { db.close(); }
    DungeonStore.Run begin(UUID who) throws Exception { return store.begin(who,exit,"frozen shard",new byte[]{1,2,3}).orElseThrow(); }
    void clear(DungeonStore.Run run) throws Exception {
        assertTrue(store.activate(run.id(),run.player()));
        for(int stage=0;stage<3;stage++) { assertTrue(store.advance(run.id(),run.player(),stage)); }
    }
    long count(String table) throws Exception { return db.read(c -> { try(var st=c.createStatement(); var rs=st.executeQuery("SELECT COUNT(*) FROM "+table)) { rs.next(); return rs.getLong(1); } }); }

    @Test void ownerAndOrderedProgressAreRequired() throws Exception {
        var run=begin(player); UUID stranger=UUID.randomUUID();
        assertFalse(store.activate(run.id(),stranger)); assertFalse(store.advance(run.id(),player,0));
        assertEquals(DungeonStore.Completion.INVALID,store.complete(run.id(),player,"2026-10-05"));
        assertFalse(store.returned(run.id(),player)); assertTrue(store.activate(run.id(),player));
        assertFalse(store.activate(run.id(),player)); assertFalse(store.advance(run.id(),player,1));
        assertFalse(store.advance(run.id(),stranger,0)); assertTrue(store.advance(run.id(),player,0));
        assertFalse(store.advance(run.id(),player,0)); assertEquals(1,store.pendingReturn(player).orElseThrow().stage());
        assertEquals(0,mail.countPending(player));
    }
    @Test void completionIsIdempotentAndRewardBytesAreFrozen() throws Exception {
        var run=begin(player); clear(run);
        assertEquals(DungeonStore.Completion.REWARDED,store.complete(run.id(),player,"2026-10-05"));
        assertEquals(DungeonStore.Completion.ALREADY_COMPLETED,store.complete(run.id(),player,"2026-10-05"));
        store.abort(run.id(),player,"late disconnect");
        assertEquals("COMPLETED",store.pendingReturn(player).orElseThrow().state());
        assertEquals(1,count("dungeon_rewards")); assertEquals(1,mail.countPending(player));
        assertArrayEquals(new byte[]{1,2,3},mail.pending(player,10).getFirst().data());
        assertEquals(run.id(),mail.pending(player,10).getFirst().sourceRef());
        assertTrue(store.returned(run.id(),player)); assertTrue(store.returned(run.id(),player)); assertFalse(store.activate(run.id(),player));
        assertEquals(1,(int)db.read(c -> { try(var st=c.createStatement(); var rs=st.executeQuery("SELECT COUNT(*) FROM audit_log WHERE action='dungeon.complete'")) { rs.next(); return rs.getInt(1); } }));
    }
    @Test void dailyRewardQuotaSurvivesNewRunsButResetsOnNextThaiDate() throws Exception {
        var first=begin(player); clear(first); store.complete(first.id(),player,"2026-10-05"); store.returned(first.id(),player);
        var second=begin(player); clear(second);
        assertEquals(DungeonStore.Completion.DAILY_LIMIT,store.complete(second.id(),player,"2026-10-05"));
        assertTrue(store.returned(second.id(),player)); assertEquals(1,mail.countPending(player));
        var third=begin(player); clear(third);
        assertEquals(DungeonStore.Completion.REWARDED,store.complete(third.id(),player,"2026-10-06"));
        assertEquals(2,mail.countPending(player)); assertEquals(2,count("dungeon_rewards"));
    }
    @Test void failedReceiptRollsBackMailCompletionAndAuditAndCanRetry() throws Exception {
        var run=begin(player); clear(run);
        db.read(c -> { try(var st=c.createStatement()) { st.execute("CREATE TRIGGER reject_dungeon BEFORE INSERT ON dungeon_rewards BEGIN SELECT RAISE(ABORT,'disk fixture'); END"); } return null; });
        assertThrows(SQLException.class,() -> store.complete(run.id(),player,"2026-10-05"));
        assertEquals(0,mail.countPending(player)); assertEquals(0,count("dungeon_rewards"));
        assertEquals("ACTIVE",store.pendingReturn(player).orElseThrow().state());
        db.read(c -> { try(var st=c.createStatement()) { st.execute("DROP TRIGGER reject_dungeon"); } return null; });
        db.read(c -> { try(var st=c.createStatement()) { st.execute("CREATE TRIGGER reject_finish_audit BEFORE INSERT ON audit_log WHEN NEW.action='dungeon.complete' BEGIN SELECT RAISE(ABORT,'late audit fixture'); END"); } return null; });
        assertThrows(SQLException.class,() -> store.complete(run.id(),player,"2026-10-05"));
        assertEquals("ACTIVE",store.pendingReturn(player).orElseThrow().state()); assertEquals(0,mail.countPending(player)); assertEquals(0,count("dungeon_rewards"));
        db.read(c -> { try(var st=c.createStatement()) { st.execute("DROP TRIGGER reject_finish_audit"); } return null; });
        assertEquals(DungeonStore.Completion.REWARDED,store.complete(run.id(),player,"2026-10-05")); assertEquals(1,mail.countPending(player));
    }
    @Test void restartAbortsCombatPreservesExitAndCommittedReward() throws Exception {
        var completed=begin(player); clear(completed); store.complete(completed.id(),player,"2026-10-05"); store.returned(completed.id(),player);
        var interrupted=begin(player); assertTrue(store.activate(interrupted.id(),player)); store.advance(interrupted.id(),player,0);
        assertEquals(1,store.recoverInterrupted()); assertEquals(0,store.recoverInterrupted());
        var pending=store.pendingReturn(player).orElseThrow(); assertEquals("ABORTED",pending.state()); assertEquals(exit,pending.exit());
        assertEquals(DungeonStore.Completion.INVALID,store.complete(interrupted.id(),player,"2026-10-06"));
        assertTrue(store.begin(player,exit,"new",new byte[]{8}).isEmpty());
        assertFalse(store.returned(interrupted.id(),UUID.randomUUID())); assertTrue(store.returned(interrupted.id(),player));
        assertEquals(1,mail.countPending(player)); assertTrue(store.begin(player,exit,"new",new byte[]{8}).isPresent());
    }
    @Test void onlyOneConcurrentStartAndOneConcurrentCompletionWins() throws Exception {
        try(var pool=Executors.newFixedThreadPool(12)) {
            var starts=pool.invokeAll(java.util.stream.IntStream.range(0,12).<java.util.concurrent.Callable<java.util.Optional<DungeonStore.Run>>>mapToObj(i -> () -> store.begin(UUID.randomUUID(),exit,"shard",new byte[]{1})).toList());
            var runs=new java.util.ArrayList<DungeonStore.Run>(); for(var f:starts) { f.get().ifPresent(runs::add); }
            assertEquals(1,runs.size()); var run=runs.getFirst(); clear(run);
            var finishes=pool.invokeAll(java.util.stream.IntStream.range(0,12).<java.util.concurrent.Callable<DungeonStore.Completion>>mapToObj(i -> () -> store.complete(run.id(),run.player(),"2026-10-05")).toList());
            int rewarded=0; for(var f:finishes) { if(f.get()==DungeonStore.Completion.REWARDED) { rewarded++; } }
            assertEquals(1,rewarded); assertEquals(1,mail.countPending(run.player()));
        }
    }
    @Test void v5MigrationPreservesAccountsMailAndHomesAndRejectsDowngrade() throws Exception {
        long oldMail=mail.enqueue(player,"old","ref","old",new byte[]{9},null);
        db.transaction(c -> {
            TestDatabases.removeDungeonTables(c);
            try(var st=c.createStatement()) {
                st.execute("UPDATE schema_version SET version=5");
                st.execute("INSERT INTO accounts VALUES ('"+player+"','gold.wallet',999,1000)");
                st.execute("INSERT INTO homes VALUES ('"+player+"','home','บ้าน','world','luma_housing',1,80,2,90,0,'ps_test',1000,1000)");
            } return null;
        });
        assertEquals(Migrations.latestVersion(),Migrations.apply(db)); assertEquals(Migrations.latestVersion(),Migrations.apply(db));
        assertEquals(oldMail,mail.pending(player,1).getFirst().id()); assertEquals(1,count("homes"));
        assertEquals(999,(int)db.read(c -> { try(var st=c.createStatement(); var rs=st.executeQuery("SELECT balance FROM accounts")) { rs.next(); return rs.getInt(1); } }));
        assertTrue(store.begin(player,exit,"new",new byte[]{1}).isPresent());
        db.read(c -> { try(var st=c.createStatement()) { st.execute("UPDATE schema_version SET version=8"); } return null; });
        assertThrows(SQLException.class,() -> Migrations.apply(db));
    }
    @Test void returnObligationAndFrozenPayloadSurviveDatabaseReopen() throws Exception {
        var run=begin(player); store.abort(run.id(),player,"quit");
        var file=db.read(c -> { try(var st=c.createStatement(); var rs=st.executeQuery("PRAGMA database_list")) { rs.next(); return java.nio.file.Path.of(rs.getString("file")); } });
        db.close(); db=Database.open(file); Migrations.apply(db); store=new DungeonStore(db,new MailStore(db,() -> 2000),() -> 2000);
        var restored=store.pendingReturn(player).orElseThrow(); assertEquals(run.id(),restored.id()); assertEquals(exit,restored.exit());
        assertEquals("ABORTED",restored.state()); assertTrue(store.returned(run.id(),player));
    }
}
