package com.armzofficial.fantasycore.repair;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.item.ItemIdentityPolicy;
import com.armzofficial.fantasycore.item.ItemInstanceStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** เงินจอง/คืนกับ state และ audit atomic ใน SQLite; inventory มี journal แต่ไม่ใช่ SQL transaction */
public final class RepairStore {
    public record Request(String opId, UUID player, ItemIdentityPolicy.Identity identity, String material,
                          int slot, int damage, int maxDamage, long price, byte[] before, byte[] repaired) {
    }

    public enum Status { RESERVED, DUPLICATE, BUSY, INVALID_ITEM, FUNDS, LIMIT }

    public record Reservation(Status status, Balances after) {
    }

    public record Review(String opId, UUID player, String material, String provider, String serial,
                         int slot, int damage, int maxDamage, long price, String state) {
    }

    public record Recovery(int refunded, int review) {
    }

    private final Database database;
    private final EconomyStore economy;
    private final LongSupplier clock;

    public RepairStore(Database database, EconomyStore economy, LongSupplier clock) {
        this.database = database;
        this.economy = economy;
        this.clock = clock;
    }

    public boolean validIdentity(UUID player, ItemIdentityPolicy.Identity identity) throws SQLException {
        return database.read(c -> validIn(c, player, identity));
    }

    public Reservation reserve(Request r) throws SQLException {
        if (r.opId() == null || r.opId().isBlank() || r.opId().length() > 80 || r.player() == null || r.identity() == null
                || r.material() == null || r.material().isBlank() || r.slot() < 0 || r.slot() > 8
                || r.damage() < 1 || r.maxDamage() <= r.damage() || r.price() < 1
                || r.before() == null || r.repaired() == null || r.before().length < 1 || r.repaired().length < 1
                || r.before().length > 1_048_576 || r.repaired().length > 1_048_576) {
            throw new IllegalArgumentException("invalid repair request");
        }
        return database.transaction(c -> {
            Balances before = economy.balancesIn(c, r.player());
            if (exists(c, "SELECT 1 FROM repair_operations WHERE op_id = ?", r.opId())) {
                return new Reservation(Status.DUPLICATE, before);
            }
            if (exists(c, "SELECT 1 FROM repair_operations WHERE player_uuid = ? AND state IN ('RESERVED','APPLYING','REVIEW')", r.player().toString())
                    || (r.identity().serial() != null && exists(c,
                    "SELECT 1 FROM repair_operations WHERE serial = ? AND state IN ('RESERVED','APPLYING','REVIEW')", r.identity().serial().toString()))) {
                return new Reservation(Status.BUSY, before);
            }
            if (!validIn(c, r.player(), r.identity())) {
                return new Reservation(Status.INVALID_ITEM, before);
            }
            TxResult paid = economy.adjustIn(c, r.player(), Bucket.GOLD_WALLET, -r.price(), null,
                    new OpMeta(r.opId() + ":charge", "item.repair.charge", r.player().toString(), "จองค่าซ่อม " + r.material(), r.opId()), null);
            if (!paid.ok()) {
                return new Reservation(paid.status() == TxResult.Status.INSUFFICIENT_FUNDS ? Status.FUNDS : Status.LIMIT, paid.after());
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO repair_operations(op_id, player_uuid, provider, template_id, template_version, serial,
                    material, slot, damage, max_damage, price, before_data, repaired_data, state, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RESERVED', ?, ?)
                    """)) {
                ps.setString(1, r.opId());
                ps.setString(2, r.player().toString());
                ps.setString(3, r.identity().provider());
                ps.setString(4, r.identity().template());
                ps.setInt(5, r.identity().version());
                ps.setString(6, r.identity().serial() == null ? null : r.identity().serial().toString());
                ps.setString(7, r.material());
                ps.setInt(8, r.slot());
                ps.setInt(9, r.damage());
                ps.setInt(10, r.maxDamage());
                ps.setLong(11, r.price());
                ps.setBytes(12, r.before());
                ps.setBytes(13, r.repaired());
                ps.setLong(14, clock.getAsLong());
                ps.setLong(15, clock.getAsLong());
                ps.executeUpdate();
            }
            audit(c, r.opId(), r.player(), "item.repair.reserve", r.material() + " damage " + r.damage() + "/" + r.maxDamage(), "player confirm");
            return new Reservation(Status.RESERVED, paid.after());
        });
    }

    /** ตรวจทะเบียนซ้ำก่อน persist APPLYING; ยังไม่เปลี่ยน inventory */
    public boolean beginApply(String opId) throws SQLException {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repair_operations WHERE op_id = ? AND state = 'RESERVED'")) {
                ps.setString(1, opId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    String serial = rs.getString("serial");
                    var identity = new ItemIdentityPolicy.Identity(rs.getString("provider"), rs.getString("template_id"),
                            rs.getInt("template_version"), serial == null ? null : UUID.fromString(serial));
                    if (!validIn(c, UUID.fromString(rs.getString("player_uuid")), identity)) {
                        return false;
                    }
                }
            }
            return changeIn(c, opId, "RESERVED", "APPLYING");
        });
    }

    /** เรียกเฉพาะเมื่อทราบว่ายังไม่แก้ inventory; คืนค่าจองกับ state ใน transaction เดียว */
    public boolean cancelUntouched(String opId) throws SQLException {
        return database.transaction(c -> cancelIn(c, opId, false, null));
    }

    public boolean complete(String opId) throws SQLException {
        return database.transaction(c -> {
            if (!changeIn(c, opId, "APPLYING", "COMMITTED")) {
                return false;
            }
            audit(c, opId, null, "item.repair.commit", "damage repaired in place", "saveData returned; disk health requires monitoring");
            return true;
        });
    }

    public boolean markReview(String opId) throws SQLException {
        return database.transaction(c -> changeIn(c, opId, "APPLYING", "REVIEW"));
    }

    public Recovery quarantineInterrupted() throws SQLException {
        return database.transaction(c -> {
            List<String> reserved = new ArrayList<>();
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT op_id FROM repair_operations WHERE state = 'RESERVED'")) {
                while (rs.next()) { reserved.add(rs.getString(1)); }
            }
            for (String op : reserved) { cancelIn(c, op, false, null); }
            int review;
            try (PreparedStatement ps = c.prepareStatement("UPDATE repair_operations SET state = 'REVIEW', updated_at = ? WHERE state = 'APPLYING'")) {
                ps.setLong(1, clock.getAsLong());
                review = ps.executeUpdate();
            }
            return new Recovery(reserved.size(), review);
        });
    }

    public boolean resolveReview(String opId, boolean complete, AuditEntry entry) throws SQLException {
        if (entry == null || entry.reason() == null || entry.reason().trim().length() < 3) {
            throw new IllegalArgumentException("reason required");
        }
        return database.transaction(c -> {
            if (!complete) {
                return cancelIn(c, opId, true, entry);
            }
            if (!changeIn(c, opId, "REVIEW", "COMMITTED")) {
                return false;
            }
            AuditStore.insert(c, entry, opId, clock.getAsLong());
            return true;
        });
    }

    public List<Review> review(int limit) throws SQLException {
        return database.read(c -> {
            List<Review> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repair_operations WHERE state = 'REVIEW' ORDER BY created_at LIMIT ?")) {
                ps.setInt(1, Math.max(1, Math.min(100, limit)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { result.add(row(rs)); }
                }
            }
            return List.copyOf(result);
        });
    }

    public Optional<Review> findReview(String opId) throws SQLException {
        return database.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repair_operations WHERE op_id = ? AND state = 'REVIEW'")) {
                ps.setString(1, opId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(row(rs)) : Optional.empty(); }
            }
        });
    }

    public int countReview() throws SQLException {
        return database.read(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM repair_operations WHERE state = 'REVIEW'")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private static Review row(ResultSet rs) throws SQLException {
        return new Review(rs.getString("op_id"), UUID.fromString(rs.getString("player_uuid")), rs.getString("material"),
                rs.getString("provider"), rs.getString("serial"), rs.getInt("slot"), rs.getInt("damage"),
                rs.getInt("max_damage"), rs.getLong("price"), rs.getString("state"));
    }

    private boolean cancelIn(Connection c, String opId, boolean reviewOnly, AuditEntry entry) throws SQLException {
        Review record;
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM repair_operations WHERE op_id = ?")) {
            ps.setString(1, opId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { return false; }
                record = row(rs);
            }
        }
        if (reviewOnly ? !record.state().equals("REVIEW") : (!record.state().equals("RESERVED") && !record.state().equals("APPLYING"))) {
            return false;
        }
        if (!changeIn(c, opId, record.state(), "CANCELLED")) { return false; }
        TxResult refund = economy.adjustIn(c, record.player(), Bucket.GOLD_WALLET, record.price(), null,
                new OpMeta(opId + ":refund", "item.repair.refund", "system", "คืนค่าจองซ่อม", opId), null);
        if (!refund.ok()) {
            throw new SQLException("คืนค่าซ่อมไม่สำเร็จ: " + refund.status() + " op " + opId);
        }
        if (entry != null) {
            AuditStore.insert(c, entry, opId, clock.getAsLong());
        } else {
            audit(c, opId, record.player(), "item.repair.cancel", "refund " + record.price(), "inventory untouched");
        }
        return true;
    }

    private static boolean validIn(Connection c, UUID player, ItemIdentityPolicy.Identity identity) throws SQLException {
        if (identity.provider().equals("vanilla")) { return true; }
        var registered = ItemInstanceStore.findIn(c, identity.serial()).orElse(null);
        if (!ItemIdentityPolicy.registeredFor(identity, player, registered)) { return false; }
        return !exists(c, """
                SELECT 1 FROM mail WHERE source_ref = (SELECT op_id FROM item_instances WHERE serial = ?)
                AND state IN ('PENDING','CLAIMING','REVIEW')
                """, identity.serial().toString());
    }

    private boolean changeIn(Connection c, String opId, String from, String to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE repair_operations SET state = ?, updated_at = ? WHERE op_id = ? AND state = ?")) {
            ps.setString(1, to);
            ps.setLong(2, clock.getAsLong());
            ps.setString(3, opId);
            ps.setString(4, from);
            return ps.executeUpdate() == 1;
        }
    }

    private static boolean exists(Connection c, String sql, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void audit(Connection c, String opId, UUID player, String action, String detail, String reason) throws SQLException {
        AuditStore.insert(c, new AuditEntry(null, "system", action, player == null ? opId : player.toString(), detail, reason),
                opId, clock.getAsLong());
    }
}
