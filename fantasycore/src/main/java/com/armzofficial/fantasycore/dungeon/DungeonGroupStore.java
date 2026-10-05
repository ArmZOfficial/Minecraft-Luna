package com.armzofficial.fantasycore.dungeon;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;
import java.sql.*;
import java.util.*;
import java.util.function.LongSupplier;

/** v7 writer; v6 history stays intact and shares the same daily quota/return obligations. */
public final class DungeonGroupStore {
    public record Member(UUID player,DungeonStore.ReturnPoint exit,String label,byte[] reward) {
        public Member {
            if(player==null || exit==null || label==null || label.isBlank() || reward==null || reward.length==0) { throw new IllegalArgumentException("member/reward"); }
            reward=reward.clone();
        }
        @Override public byte[] reward() { return reward.clone(); }
    }
    public record Completed(boolean valid,Map<UUID,DungeonStore.Completion> members) {
        public Completed { members=Map.copyOf(members); }
    }
    private final Database db;
    private final MailStore mail;
    private final DungeonStore legacy;
    private final LongSupplier clock;
    public DungeonGroupStore(Database db,MailStore mail,DungeonStore legacy,LongSupplier clock) { this.db=db; this.mail=mail; this.legacy=legacy; this.clock=clock; }

    public Optional<String> begin(InstanceSlot slot,UUID leader,UUID party,List<Member> members,String templateHash) throws SQLException {
        List<Member> frozen=List.copyOf(members);
        Set<UUID> ids=new HashSet<>(); frozen.forEach(m -> ids.add(m.player));
        if(slot==null || leader==null || !ids.contains(leader) || ids.size()!=frozen.size() || frozen.isEmpty() || frozen.size()>4
                || (slot.party() && (frozen.size()<2 || party==null)) || (!slot.party() && frozen.size()!=1)
                || templateHash==null || !templateHash.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException("run snapshot"); }
        return db.transaction(c -> {
            try(var ps=c.prepareStatement("SELECT 1 FROM dungeon_group_runs WHERE instance_key=? AND state IN ('PREPARING','ACTIVE')")) {
                ps.setString(1,slot.key()); try(var rs=ps.executeQuery()) { if(rs.next()) { return Optional.empty(); } }
            }
            if(!slot.party()) {
                try(var st=c.createStatement(); var rs=st.executeQuery("SELECT 1 FROM dungeon_runs WHERE state IN ('PREPARING','ACTIVE')")) { if(rs.next()) { return Optional.empty(); } }
            }
            for(Member member:frozen) {
                try(var ps=c.prepareStatement("SELECT 1 FROM dungeon_group_members WHERE player_uuid=? AND needs_return=1 UNION ALL SELECT 1 FROM dungeon_runs WHERE player_uuid=? AND needs_return=1")) {
                    ps.setString(1,member.player.toString()); ps.setString(2,member.player.toString());
                    try(var rs=ps.executeQuery()) { if(rs.next()) { return Optional.empty(); } }
                }
            }
            String run=UUID.randomUUID().toString(); long now=clock.getAsLong();
            try(var ps=c.prepareStatement("INSERT INTO dungeon_group_runs(run_id,dungeon_id,instance_key,leader_uuid,party_uuid,member_count,template_hash,state,created_at,updated_at) VALUES (?,?,?,?,?,?,?,'PREPARING',?,?)")) {
                ps.setString(1,run); ps.setString(2,DungeonStore.ID); ps.setString(3,slot.key()); ps.setString(4,leader.toString());
                ps.setString(5,party==null?null:party.toString()); ps.setInt(6,frozen.size()); ps.setString(7,templateHash); ps.setLong(8,now); ps.setLong(9,now); ps.executeUpdate();
            }
            for(Member member:frozen) {
                var exit=member.exit;
                try(var ps=c.prepareStatement("INSERT INTO dungeon_group_members(run_id,player_uuid,return_world,return_x,return_y,return_z,return_yaw,return_pitch,reward_version,reward_label,reward_data,updated_at) VALUES (?,?,?,?,?,?,?,?,1,?,?,?)")) {
                    ps.setString(1,run); ps.setString(2,member.player.toString()); ps.setString(3,exit.world().toString());
                    ps.setDouble(4,exit.x()); ps.setDouble(5,exit.y()); ps.setDouble(6,exit.z()); ps.setFloat(7,exit.yaw()); ps.setFloat(8,exit.pitch());
                    ps.setString(9,member.label); ps.setBytes(10,member.reward); ps.setLong(11,now); ps.executeUpdate();
                }
            }
            AuditStore.insert(c,new AuditEntry(leader.toString(),"player","dungeon.start",slot.key(),frozen.size()+" members · "+templateHash,"frozen roster"),run,now);
            return Optional.of(run);
        });
    }
    public boolean activate(String run,UUID leader) throws SQLException {
        return db.transaction(c -> {
            try(var ps=c.prepareStatement("UPDATE dungeon_group_runs SET state='ACTIVE',updated_at=? WHERE run_id=? AND leader_uuid=? AND state='PREPARING' AND member_count=(SELECT COUNT(*) FROM dungeon_group_members m WHERE m.run_id=dungeon_group_runs.run_id AND m.needs_return=1)")) {
                ps.setLong(1,clock.getAsLong()); ps.setString(2,run); ps.setString(3,leader.toString()); return ps.executeUpdate()==1;
            }
        });
    }
    public boolean advance(String run,UUID leader,int from) throws SQLException {
        if(from<0 || from>2) { throw new IllegalArgumentException("stage"); }
        return db.transaction(c -> {
            try(var ps=c.prepareStatement("UPDATE dungeon_group_runs SET stage=stage+1,updated_at=? WHERE run_id=? AND leader_uuid=? AND state='ACTIVE' AND stage=?")) {
                ps.setLong(1,clock.getAsLong()); ps.setString(2,run); ps.setString(3,leader.toString()); ps.setInt(4,from); return ps.executeUpdate()==1;
            }
        });
    }
    /** Every frozen member receipt/mail and the run completion commit together; no partial group rewards. */
    public Completed complete(String run,UUID leader,String period) throws SQLException {
        java.time.LocalDate.parse(period);
        return db.transaction(c -> {
            String state; int stage,count;
            try(var ps=c.prepareStatement("SELECT state,stage,member_count FROM dungeon_group_runs WHERE run_id=? AND leader_uuid=?")) {
                ps.setString(1,run); ps.setString(2,leader.toString());
                try(var rs=ps.executeQuery()) { if(!rs.next()) { return new Completed(false,Map.of()); } state=rs.getString(1); stage=rs.getInt(2); count=rs.getInt(3); }
            }
            if(!state.equals("COMPLETED") && (!state.equals("ACTIVE") || stage!=3)) { return new Completed(false,Map.of()); }
            Map<UUID,DungeonStore.Completion> results=new LinkedHashMap<>();
            try(var ps=c.prepareStatement("SELECT * FROM dungeon_group_members WHERE run_id=? ORDER BY player_uuid")) {
                ps.setString(1,run);
                try(var rs=ps.executeQuery()) {
                    while(rs.next()) {
                        UUID player=UUID.fromString(rs.getString("player_uuid"));
                        if(state.equals("COMPLETED")) { results.put(player,DungeonStore.Completion.ALREADY_COMPLETED); continue; }
                        if(rs.getInt("needs_return")!=1) { throw new SQLException("member returned before completion"); }
                        if(DungeonStore.claimedIn(c,player,period)) { results.put(player,DungeonStore.Completion.DAILY_LIMIT); continue; }
                        long now=clock.getAsLong();
                        long mailId=mail.enqueueIn(c,player,"dungeon.training",run,rs.getString("reward_label"),rs.getBytes("reward_data"));
                        try(var receipt=c.prepareStatement("INSERT INTO dungeon_group_rewards VALUES (?,?,?,?,?,?,?)")) {
                            receipt.setString(1,run); receipt.setString(2,player.toString()); receipt.setString(3,DungeonStore.ID); receipt.setString(4,period);
                            receipt.setInt(5,rs.getInt("reward_version")); receipt.setLong(6,mailId); receipt.setLong(7,now); receipt.executeUpdate();
                        }
                        results.put(player,DungeonStore.Completion.REWARDED);
                    }
                }
            }
            if(results.size()!=count) { throw new SQLException("frozen member count changed"); }
            if(!state.equals("COMPLETED")) {
                try(var ps=c.prepareStatement("UPDATE dungeon_group_runs SET state='COMPLETED',updated_at=? WHERE run_id=?")) { ps.setLong(1,clock.getAsLong()); ps.setString(2,run); ps.executeUpdate(); }
                AuditStore.insert(c,new AuditEntry(leader.toString(),"system","dungeon.complete",DungeonStore.ID,results.toString(),"all encounters cleared"),run,clock.getAsLong());
            }
            return new Completed(true,results);
        });
    }
    public void abort(String run,UUID leader,String reason) throws SQLException {
        boolean group=db.transaction(c -> {
            try(var find=c.prepareStatement("SELECT leader_uuid FROM dungeon_group_runs WHERE run_id=?")) {
                find.setString(1,run); try(var rs=find.executeQuery()) { if(!rs.next()) { return false; } }
            }
            try(var member=c.prepareStatement("SELECT 1 FROM dungeon_group_members WHERE run_id=? AND player_uuid=?")) {
                member.setString(1,run); member.setString(2,leader.toString());
                try(var rs=member.executeQuery()) { if(!rs.next()) { throw new SQLException("not a frozen member"); } }
            }
            try(var ps=c.prepareStatement("UPDATE dungeon_group_runs SET state='ABORTED',reason=?,updated_at=? WHERE run_id=? AND state IN ('PREPARING','ACTIVE')")) {
                ps.setString(1,reason); ps.setLong(2,clock.getAsLong()); ps.setString(3,run);
                if(ps.executeUpdate()==1) { AuditStore.insert(c,new AuditEntry(null,"system","dungeon.abort",DungeonStore.ID,run,reason),run,clock.getAsLong()); }
            }
            return true;
        });
        if(!group) { legacy.abort(run,leader,reason); }
    }
    public int recoverInterrupted() throws SQLException {
        int old=legacy.recoverInterrupted();
        return old+db.transaction(c -> { try(var ps=c.prepareStatement("UPDATE dungeon_group_runs SET state='ABORTED',reason='server restart',updated_at=? WHERE state IN ('PREPARING','ACTIVE')")) { ps.setLong(1,clock.getAsLong()); return ps.executeUpdate(); } });
    }
    public Optional<DungeonStore.Run> pendingReturn(UUID player) throws SQLException {
        var group=db.read(c -> {
            try(var ps=c.prepareStatement("SELECT m.*,r.state,r.stage FROM dungeon_group_members m JOIN dungeon_group_runs r ON r.run_id=m.run_id WHERE m.player_uuid=? AND m.needs_return=1")) {
                ps.setString(1,player.toString());
                try(var rs=ps.executeQuery()) {
                    if(!rs.next()) { return Optional.<DungeonStore.Run>empty(); }
                    return Optional.of(new DungeonStore.Run(rs.getString("run_id"),player,rs.getString("state"),rs.getInt("stage"),true,
                            new DungeonStore.ReturnPoint(UUID.fromString(rs.getString("return_world")),rs.getDouble("return_x"),rs.getDouble("return_y"),rs.getDouble("return_z"),rs.getFloat("return_yaw"),rs.getFloat("return_pitch"))));
                }
            }
        });
        return group.isPresent()?group:legacy.pendingReturn(player);
    }
    public boolean returned(String run,UUID player) throws SQLException {
        boolean[] found={false};
        boolean result=db.transaction(c -> {
            try(var ps=c.prepareStatement("SELECT r.state FROM dungeon_group_members m JOIN dungeon_group_runs r ON r.run_id=m.run_id WHERE m.run_id=? AND m.player_uuid=?")) {
                ps.setString(1,run); ps.setString(2,player.toString());
                try(var rs=ps.executeQuery()) { if(!rs.next()) { return false; } found[0]=true; if(!Set.of("ABORTED","COMPLETED").contains(rs.getString(1))) { return false; } }
            }
            try(var ps=c.prepareStatement("UPDATE dungeon_group_members SET needs_return=0,updated_at=? WHERE run_id=? AND player_uuid=?")) {
                ps.setLong(1,clock.getAsLong()); ps.setString(2,run); ps.setString(3,player.toString()); return ps.executeUpdate()==1;
            }
        });
        return found[0]?result:legacy.returned(run,player);
    }
}
