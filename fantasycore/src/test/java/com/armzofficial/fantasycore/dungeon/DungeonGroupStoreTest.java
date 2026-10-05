package com.armzofficial.fantasycore.dungeon;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.*;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DungeonGroupStoreTest {
    Database db; MailStore mail; DungeonStore legacy; DungeonGroupStore groups;
    UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID();
    DungeonStore.ReturnPoint exit=new DungeonStore.ReturnPoint(UUID.randomUUID(),10.5,80,20.5,90,0);
    @BeforeEach void open() throws Exception { db=TestDatabases.fresh(); services(); }
    void services() { mail=new MailStore(db,() -> 1000); legacy=new DungeonStore(db,mail,() -> 1000); groups=new DungeonGroupStore(db,mail,legacy,() -> 1000); }
    @AfterEach void close() { db.close(); }
    List<DungeonGroupStore.Member> members(UUID...ids) { return Arrays.stream(ids).map(id -> new DungeonGroupStore.Member(id,exit,"shard",new byte[]{1,2})).toList(); }
    String begin(InstanceSlot slot,UUID...ids) throws Exception { return groups.begin(slot,ids[0],slot.party()?UUID.randomUUID():null,members(ids),MoonfallMap.fingerprint()).orElseThrow(); }
    void clear(String run,UUID leader) throws Exception { assertTrue(groups.activate(run,leader)); for(int stage=0;stage<3;stage++) { assertTrue(groups.advance(run,leader,stage)); } }
    long count(String table) throws Exception { return db.read(conn -> { try(var st=conn.createStatement();var rs=st.executeQuery("SELECT COUNT(*) FROM "+table)) { rs.next(); return rs.getLong(1); } }); }
    void sql(String sql) throws Exception { db.read(conn -> { try(var st=conn.createStatement()) { st.execute(sql); } return null; }); }
    @Test void independentInstancesBlockOverlappingMembersAndSlotReuseUntilReturn() throws Exception {
        String one=begin(InstanceSlot.PARTY1,a,b),two=begin(InstanceSlot.PARTY2,c,d);
        assertEquals(2,count("dungeon_group_runs"));
        assertTrue(groups.begin(InstanceSlot.PARTY1,c,UUID.randomUUID(),members(c,d),MoonfallMap.fingerprint()).isEmpty());
        groups.abort(two,d,"member left"); assertFalse(groups.returned(two,a));
        assertTrue(groups.returned(two,c)); assertTrue(groups.returned(two,d));
        assertTrue(groups.begin(InstanceSlot.PARTY2,a,UUID.randomUUID(),members(a,c),MoonfallMap.fingerprint()).isEmpty());
        groups.abort(one,b,"member quit"); assertTrue(groups.returned(one,a)); assertTrue(groups.returned(one,b));
        assertNotNull(begin(InstanceSlot.PARTY1,a,c)); assertNotNull(begin(InstanceSlot.PARTY2,b,d));
    }
    @Test void ownershipOrderedStagesAndFrozenRewardBytes() throws Exception {
        byte[] data={8,9}; var member=new DungeonGroupStore.Member(a,exit,"first",data); data[0]=4; member.reward()[1]=7;
        var list=new ArrayList<>(List.of(member,new DungeonGroupStore.Member(b,new DungeonStore.ReturnPoint(exit.world(),9,81,11,180,2),"second",new byte[]{3})));
        String run=groups.begin(InstanceSlot.PARTY1,a,UUID.randomUUID(),list,MoonfallMap.fingerprint()).orElseThrow(); list.clear();
        assertFalse(groups.activate(run,b)); assertFalse(groups.advance(run,a,0)); assertFalse(groups.complete(run,a,"2026-10-05").valid());
        assertFalse(groups.returned(run,a)); assertThrows(SQLException.class,() -> groups.abort(run,c,"intruder"));
        assertTrue(groups.activate(run,a)); assertFalse(groups.advance(run,a,1)); assertFalse(groups.advance(run,b,0));
        for(int i=0;i<3;i++) { assertTrue(groups.advance(run,a,i)); assertFalse(groups.advance(run,a,i)); }
        var results=groups.complete(run,a,"2026-10-05"); assertTrue(results.valid()); assertEquals(2,results.members().size());
        assertArrayEquals(new byte[]{8,9},mail.pending(a,10).getFirst().data());
        assertArrayEquals(new byte[]{3},mail.pending(b,10).getFirst().data());
        assertEquals(81,groups.pendingReturn(b).orElseThrow().exit().y());
        assertEquals(2,count("dungeon_group_rewards"));
        groups.abort(run,b,"late disconnect"); assertEquals("COMPLETED",groups.pendingReturn(b).orElseThrow().state());
        assertEquals(DungeonStore.Completion.ALREADY_COMPLETED,groups.complete(run,a,"2026-10-06").members().get(a)); assertEquals(1,mail.countPending(a));
    }
    @Test void allMemberRewardsAndFinalAuditRollBackTogetherAndRetry() throws Exception {
        String run=begin(InstanceSlot.PARTY1,a,b); clear(run,a);
        sql("CREATE TRIGGER reject_second BEFORE INSERT ON dungeon_group_rewards WHEN (SELECT COUNT(*) FROM dungeon_group_rewards)=1 BEGIN SELECT RAISE(ABORT,'second member fixture'); END");
        assertThrows(SQLException.class,() -> groups.complete(run,a,"2026-10-05"));
        assertEquals(0,count("mail")); assertEquals(0,count("dungeon_group_rewards")); assertEquals("ACTIVE",groups.pendingReturn(b).orElseThrow().state());
        sql("DROP TRIGGER reject_second");
        sql("CREATE TRIGGER reject_audit BEFORE INSERT ON audit_log WHEN NEW.action='dungeon.complete' BEGIN SELECT RAISE(ABORT,'audit fixture'); END");
        assertThrows(SQLException.class,() -> groups.complete(run,a,"2026-10-05")); assertEquals(0,count("mail"));
        sql("DROP TRIGGER reject_audit"); assertTrue(groups.complete(run,a,"2026-10-05").valid());
        assertEquals(2,count("mail")); assertEquals(2,count("dungeon_group_rewards"));
    }
    @Test void legacyAndGroupRewardsShareQuotaInBothDirections() throws Exception {
        var old=legacy.begin(a,exit,"old",new byte[]{9}).orElseThrow(); legacy.activate(old.id(),a);
        for(int i=0;i<3;i++) { legacy.advance(old.id(),a,i); } legacy.complete(old.id(),a,"2026-10-05"); legacy.returned(old.id(),a);
        String run=begin(InstanceSlot.PARTY1,a,b); clear(run,a); var result=groups.complete(run,a,"2026-10-05");
        assertEquals(DungeonStore.Completion.DAILY_LIMIT,result.members().get(a)); assertEquals(DungeonStore.Completion.REWARDED,result.members().get(b));
        groups.returned(run,a); groups.returned(run,b);
        var later=legacy.begin(b,exit,"old writer",new byte[]{6}).orElseThrow(); legacy.activate(later.id(),b);
        for(int i=0;i<3;i++) { legacy.advance(later.id(),b,i); }
        assertEquals(DungeonStore.Completion.DAILY_LIMIT,legacy.complete(later.id(),b,"2026-10-05")); legacy.returned(later.id(),b);
        String next=begin(InstanceSlot.PARTY2,a,b); clear(next,a);
        assertTrue(groups.complete(next,a,"2026-10-06").members().values().stream().allMatch(v -> v==DungeonStore.Completion.REWARDED));
        assertEquals(2,mail.countPending(a)); assertEquals(2,mail.countPending(b));
    }
    @Test void oldAndNewSoloLaneAndPendingReturnCannotOverlap() throws Exception {
        var old=legacy.begin(a,exit,"old",new byte[]{1}).orElseThrow();
        assertTrue(groups.begin(InstanceSlot.TRAINING,b,null,members(b),MoonfallMap.fingerprint()).isEmpty());
        assertTrue(groups.begin(InstanceSlot.PARTY1,a,UUID.randomUUID(),members(a,b),MoonfallMap.fingerprint()).isEmpty());
        legacy.abort(old.id(),a,"upgrade"); legacy.returned(old.id(),a);
        String run=begin(InstanceSlot.TRAINING,a);
        assertTrue(legacy.begin(b,exit,"new",new byte[]{1}).isEmpty());
        groups.abort(run,a,"stop"); assertTrue(legacy.begin(a,exit,"new",new byte[]{1}).isEmpty());
        groups.returned(run,a); assertTrue(legacy.begin(a,exit,"new",new byte[]{1}).isPresent());
    }
    @Test void concurrentStartsAndFinishesProduceOnlyOneGroupReceiptSet() throws Exception {
        try(var pool=Executors.newFixedThreadPool(12)) {
            var starts=pool.invokeAll(java.util.stream.IntStream.range(0,12).<Callable<Optional<String>>>mapToObj(i -> () -> groups.begin(InstanceSlot.PARTY1,a,UUID.randomUUID(),members(a,b),MoonfallMap.fingerprint())).toList());
            var runs=new ArrayList<String>(); for(var f:starts) { f.get().ifPresent(runs::add); } assertEquals(1,runs.size());
            String run=runs.getFirst(); clear(run,a);
            var finishes=pool.invokeAll(java.util.stream.IntStream.range(0,12).<Callable<DungeonGroupStore.Completed>>mapToObj(i -> () -> groups.complete(run,a,"2026-10-05")).toList());
            int rewarded=0; for(var f:finishes) { if(f.get().members().get(a)==DungeonStore.Completion.REWARDED) { rewarded++; } }
            assertEquals(1,rewarded); assertEquals(2,count("mail")); assertEquals(2,count("dungeon_group_rewards"));
        }
    }
    @Test void restartAndReopenPreserveEachExitAndCommittedMail() throws Exception {
        String done=begin(InstanceSlot.PARTY1,a,b); clear(done,a); groups.complete(done,a,"2026-10-05"); groups.returned(done,a); groups.returned(done,b);
        String interrupted=begin(InstanceSlot.PARTY2,c,d); groups.activate(interrupted,c);
        var path=db.read(conn -> { try(var st=conn.createStatement();var rs=st.executeQuery("PRAGMA database_list")) { rs.next(); return java.nio.file.Path.of(rs.getString("file")); } });
        db.close(); db=Database.open(path); Migrations.apply(db); services();
        assertEquals(1,groups.recoverInterrupted()); assertEquals(0,groups.recoverInterrupted());
        assertEquals(exit,groups.pendingReturn(d).orElseThrow().exit()); assertEquals("ABORTED",groups.pendingReturn(d).orElseThrow().state());
        assertFalse(groups.complete(interrupted,c,"2026-10-05").valid()); groups.abort(interrupted,d,"return recovery");
        assertTrue(groups.returned(interrupted,d)); assertTrue(groups.returned(interrupted,d)); assertTrue(groups.pendingReturn(d).isEmpty());
        assertEquals(1,mail.countPending(a)); assertEquals(1,mail.countPending(b)); assertEquals(0,mail.countPending(d));
    }
    @Test void v6MigrationPreservesLegacyRewardAndReturnAndIsRepeatable() throws Exception {
        var old=legacy.begin(a,exit,"legacy frozen",new byte[]{9,8}).orElseThrow(); legacy.activate(old.id(),a);
        for(int i=0;i<3;i++) { legacy.advance(old.id(),a,i); } legacy.complete(old.id(),a,"2026-10-05");
        var before=mail.pending(a,10).getFirst(); long audit=count("audit_log");
        sql("DROP TABLE dungeon_group_rewards"); sql("DROP TABLE dungeon_group_members"); sql("DROP TABLE dungeon_group_runs"); sql("UPDATE schema_version SET version=6");
        assertEquals(7,Migrations.apply(db)); assertEquals(7,Migrations.apply(db));
        assertEquals(before.id(),mail.pending(a,10).getFirst().id()); assertArrayEquals(before.data(),mail.pending(a,10).getFirst().data());
        assertEquals(audit,count("audit_log")); assertEquals(1,count("dungeon_rewards")); assertEquals(0,count("dungeon_group_runs"));
        assertEquals(exit,groups.pendingReturn(a).orElseThrow().exit()); groups.abort(old.id(),a,"return recovery"); assertTrue(groups.returned(old.id(),a));
        String run=begin(InstanceSlot.PARTY1,a,b); clear(run,a);
        assertEquals(DungeonStore.Completion.DAILY_LIMIT,groups.complete(run,a,"2026-10-05").members().get(a));
    }
    @Test void invalidFrozenRunsRejectedWithoutWriting() {
        assertThrows(IllegalArgumentException.class,() -> groups.begin(InstanceSlot.PARTY1,a,null,members(a,b),MoonfallMap.fingerprint()));
        assertThrows(IllegalArgumentException.class,() -> groups.begin(InstanceSlot.PARTY1,a,UUID.randomUUID(),members(a,a),MoonfallMap.fingerprint()));
        assertThrows(IllegalArgumentException.class,() -> groups.begin(InstanceSlot.TRAINING,a,null,members(a,b),MoonfallMap.fingerprint()));
        assertThrows(IllegalArgumentException.class,() -> groups.begin(InstanceSlot.TRAINING,a,null,members(a),"path-from-user"));
        assertTrue(InstanceSlot.parse("../world").isEmpty()); assertEquals("luma_moonfall_party_2",InstanceSlot.PARTY2.world());
    }
}
