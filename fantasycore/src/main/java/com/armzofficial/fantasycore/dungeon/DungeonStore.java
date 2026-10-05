package com.armzofficial.fantasycore.dungeon;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** World combat is transient; only confirmed room progression and mail receipts are durable. */
public final class DungeonStore {
    public static final String ID = "moonfall_training";
    public record ReturnPoint(UUID world, double x, double y, double z, float yaw, float pitch) {
        public ReturnPoint {
            if (world == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) { throw new IllegalArgumentException("return point"); }
        }
    }
    public record Run(String id, UUID player, String state, int stage, boolean needsReturn, ReturnPoint exit) {}
    public enum Completion { REWARDED, DAILY_LIMIT, ALREADY_COMPLETED, INVALID }
    private final Database database;
    private final MailStore mail;
    private final LongSupplier clock;

    public DungeonStore(Database database, MailStore mail, LongSupplier clock) {
        this.database = database; this.mail = mail; this.clock = clock;
    }

    /** Empty means occupied or this player still owes a return; constraints cover races too. */
    public Optional<Run> begin(UUID player, ReturnPoint exit, String label, byte[] reward) throws SQLException {
        if (label == null || label.isBlank() || reward == null || reward.length == 0) { throw new IllegalArgumentException("reward"); }
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("SELECT 1 FROM dungeon_runs WHERE state IN ('PREPARING','ACTIVE') OR (player_uuid=? AND needs_return=1) UNION ALL SELECT 1 FROM dungeon_group_runs WHERE instance_key='training' AND state IN ('PREPARING','ACTIVE') UNION ALL SELECT 1 FROM dungeon_group_members WHERE player_uuid=? AND needs_return=1")) {
                ps.setString(1, player.toString()); ps.setString(2,player.toString());
                try (var rs = ps.executeQuery()) { if (rs.next()) { return Optional.empty(); } }
            }
            String id = UUID.randomUUID().toString(); long now = clock.getAsLong();
            try (var ps = c.prepareStatement("""
                    INSERT INTO dungeon_runs(run_id,dungeon_id,player_uuid,state,return_world,return_x,return_y,return_z,
                    return_yaw,return_pitch,reward_version,reward_label,reward_data,created_at,updated_at)
                    VALUES (?,?,?,'PREPARING',?,?,?,?,?,?,1,?,?,?,?)""")) {
                ps.setString(1, id); ps.setString(2, ID); ps.setString(3, player.toString()); ps.setString(4, exit.world().toString());
                ps.setDouble(5, exit.x()); ps.setDouble(6, exit.y()); ps.setDouble(7, exit.z());
                ps.setFloat(8, exit.yaw()); ps.setFloat(9, exit.pitch()); ps.setString(10, label); ps.setBytes(11, reward);
                ps.setLong(12, now); ps.setLong(13, now); ps.executeUpdate();
            }
            AuditStore.insert(c, new AuditEntry(player.toString(), "player", "dungeon.start", ID, id, "solo training"), id, now);
            return Optional.of(new Run(id, player, "PREPARING", 0, true, exit));
        });
    }

    public boolean activate(String id, UUID player) throws SQLException {
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("UPDATE dungeon_runs SET state='ACTIVE',updated_at=? WHERE run_id=? AND player_uuid=? AND state='PREPARING' AND needs_return=1")) {
                ps.setLong(1, clock.getAsLong()); ps.setString(2, id); ps.setString(3, player.toString()); return ps.executeUpdate() == 1;
            }
        });
    }

    public boolean advance(String id, UUID player, int from) throws SQLException {
        if (from < 0 || from > 2) { throw new IllegalArgumentException("stage"); }
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("UPDATE dungeon_runs SET stage=stage+1,updated_at=? WHERE run_id=? AND player_uuid=? AND state='ACTIVE' AND stage=? AND needs_return=1")) {
                ps.setLong(1, clock.getAsLong()); ps.setString(2, id); ps.setString(3, player.toString()); ps.setInt(4, from);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Completion + daily entitlement + frozen mail bytes + audit share one transaction. */
    public Completion complete(String id, UUID player, String period) throws SQLException {
        if (period == null || !period.matches("\\d{4}-\\d{2}-\\d{2}")) { throw new IllegalArgumentException("period"); }
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM dungeon_runs WHERE run_id=? AND player_uuid=?")) {
                ps.setString(1, id); ps.setString(2, player.toString());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) { return Completion.INVALID; }
                    if (rs.getString("state").equals("COMPLETED")) { return Completion.ALREADY_COMPLETED; }
                    if (!rs.getString("state").equals("ACTIVE") || rs.getInt("stage") != 3 || rs.getInt("needs_return") != 1) { return Completion.INVALID; }
                    boolean rewarded=!claimedIn(c,player,period);
                    long now = clock.getAsLong();
                    if (rewarded) {
                        long mailId = mail.enqueueIn(c, player, "dungeon.training", id, rs.getString("reward_label"), rs.getBytes("reward_data"));
                        try (var receipt = c.prepareStatement("INSERT INTO dungeon_rewards VALUES (?,?,?,?,?,?,?)")) {
                            receipt.setString(1, id); receipt.setString(2, player.toString()); receipt.setString(3, ID);
                            receipt.setString(4, period); receipt.setInt(5, rs.getInt("reward_version")); receipt.setLong(6, mailId); receipt.setLong(7, now);
                            receipt.executeUpdate();
                        }
                    }
                    try (var update = c.prepareStatement("UPDATE dungeon_runs SET state='COMPLETED',updated_at=? WHERE run_id=?")) {
                        update.setLong(1, now); update.setString(2, id); update.executeUpdate();
                    }
                    AuditStore.insert(c, new AuditEntry(player.toString(), "system", "dungeon.complete", ID,
                            rewarded ? "mail granted" : "daily reward already claimed", "all three encounters cleared"), id, now);
                    return rewarded ? Completion.REWARDED : Completion.DAILY_LIMIT;
                }
            }
        });
    }

    public void abort(String id, UUID player, String reason) throws SQLException {
        database.transaction(c -> {
            try (var ps = c.prepareStatement("UPDATE dungeon_runs SET state='ABORTED',reason=?,updated_at=? WHERE run_id=? AND player_uuid=? AND state IN ('PREPARING','ACTIVE')")) {
                ps.setString(1, reason); ps.setLong(2, clock.getAsLong()); ps.setString(3, id); ps.setString(4, player.toString());
                if (ps.executeUpdate() == 1) { AuditStore.insert(c, new AuditEntry(null, "system", "dungeon.abort", ID, id, reason), id, clock.getAsLong()); }
            }
            return null;
        });
    }

    static boolean claimedIn(Connection c,UUID player,String period) throws SQLException {
        try(var ps=c.prepareStatement("SELECT 1 FROM dungeon_rewards WHERE player_uuid=? AND dungeon_id=? AND period=? UNION ALL SELECT 1 FROM dungeon_group_rewards WHERE player_uuid=? AND dungeon_id=? AND period=? LIMIT 1")) {
            for(int start:new int[]{1,4}) { ps.setString(start,player.toString()); ps.setString(start+1,ID); ps.setString(start+2,period); }
            try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }

    public int recoverInterrupted() throws SQLException {
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("UPDATE dungeon_runs SET state='ABORTED',reason='server restart',updated_at=? WHERE state IN ('PREPARING','ACTIVE')")) {
                ps.setLong(1, clock.getAsLong()); return ps.executeUpdate();
            }
        });
    }

    public Optional<Run> pendingReturn(UUID player) throws SQLException {
        return database.read(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM dungeon_runs WHERE player_uuid=? AND needs_return=1")) {
                ps.setString(1, player.toString());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) { return Optional.empty(); }
                    return Optional.of(new Run(rs.getString("run_id"), player, rs.getString("state"), rs.getInt("stage"), true,
                            new ReturnPoint(UUID.fromString(rs.getString("return_world")), rs.getDouble("return_x"), rs.getDouble("return_y"),
                                    rs.getDouble("return_z"), rs.getFloat("return_yaw"), rs.getFloat("return_pitch"))));
                }
            }
        });
    }

    public boolean returned(String id, UUID player) throws SQLException {
        return database.transaction(c -> {
            try (var ps = c.prepareStatement("UPDATE dungeon_runs SET needs_return=0,updated_at=? WHERE run_id=? AND player_uuid=? AND state IN ('COMPLETED','ABORTED') AND needs_return=1")) {
                ps.setLong(1, clock.getAsLong()); ps.setString(2, id); ps.setString(3, player.toString());
                if(ps.executeUpdate()==1) { return true; }
                try(var done=c.prepareStatement("SELECT 1 FROM dungeon_runs WHERE run_id=? AND player_uuid=? AND state IN ('COMPLETED','ABORTED') AND needs_return=0")) {
                    done.setString(1,id); done.setString(2,player.toString()); try(var rs=done.executeQuery()) { return rs.next(); }
                }
            }
        });
    }
}
