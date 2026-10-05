package com.armzofficial.fantasycore.mail;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * กล่องจดหมาย: ของที่ส่งเข้า inventory ไม่ได้จะรอที่นี่ ไม่ทิ้งลงพื้น
 * <p>
 * สถานะ: PENDING → (เริ่มรับ) CLAIMING → (ใส่ inventory แล้ว) CLAIMED
 * ถ้าเซิร์ฟดับระหว่าง CLAIMING เราไม่รู้ว่าของเข้า inventory ที่ถูกบันทึกแล้วหรือยัง จึงย้ายไป REVIEW ให้ทีมงานตัดสิน
 * (ปล่อยคืน PENDING หรือปิดเป็น VOID) — ไม่คืนของอัตโนมัติเพื่อไม่ให้เกิดของซ้ำ
 */
public final class MailStore {

    public record MailItem(long id, UUID player, String source, String sourceRef, String label, byte[] data,
                           String state, long createdAt) {
    }

    private final Database database;
    private final LongSupplier clock;

    public MailStore(Database database, LongSupplier clock) {
        this.database = database;
        this.clock = clock;
    }

    /** ใส่ของลงกล่องใน transaction ที่ผู้เรียกเปิดอยู่ */
    public long enqueueIn(Connection connection, UUID player, String source, String sourceRef, String label, byte[] data)
            throws SQLException {
        long now = clock.getAsLong();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO mail(player_uuid, source, source_ref, label, item_data, state, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, player.toString());
            ps.setString(2, source);
            ps.setString(3, sourceRef);
            ps.setString(4, label);
            ps.setBytes(5, data);
            ps.setLong(6, now);
            ps.setLong(7, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("ไม่ได้ id ของจดหมาย");
                }
                return keys.getLong(1);
            }
        }
    }

    public long enqueue(UUID player, String source, String sourceRef, String label, byte[] data, AuditEntry audit)
            throws SQLException {
        return database.transaction(connection -> {
            long id = enqueueIn(connection, player, source, sourceRef, label, data);
            if (audit != null) {
                AuditStore.insert(connection, audit, sourceRef, clock.getAsLong());
            }
            return id;
        });
    }

    public List<MailItem> pending(UUID player, int limit) throws SQLException {
        return database.read(connection -> query(connection,
                "SELECT * FROM mail WHERE player_uuid = ? AND state = 'PENDING' ORDER BY id LIMIT ?", player.toString(), limit));
    }

    public int countPending(UUID player) throws SQLException {
        return database.read(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM mail WHERE player_uuid = ? AND state = 'PENDING'")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    /** จองรายการนี้เพื่อส่งเข้า inventory — คืน false ถ้าไม่ใช่ PENDING ของผู้เล่นนี้แล้ว (กันรับซ้ำ) */
    public boolean beginClaim(long id, UUID player, String claimOp) throws SQLException {
        return database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE mail SET state = 'CLAIMING', claim_op = ?, updated_at = ? "
                            + "WHERE id = ? AND player_uuid = ? AND state = 'PENDING'")) {
                ps.setString(1, claimOp);
                ps.setLong(2, clock.getAsLong());
                ps.setLong(3, id);
                ps.setString(4, player.toString());
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** ของเข้า inventory แล้ว */
    public void finishClaim(long id, String claimOp) throws SQLException {
        setStateFor(id, claimOp, "CLAIMING", "CLAIMED");
    }

    /** ใส่ inventory ไม่ได้ (เช่น เต็มระหว่างทาง) — คืนเป็น PENDING */
    public void abortClaim(long id, String claimOp) throws SQLException {
        setStateFor(id, claimOp, "CLAIMING", "PENDING");
    }

    private void setStateFor(long id, String claimOp, String from, String to) throws SQLException {
        database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE mail SET state = ?, updated_at = ? WHERE id = ? AND claim_op = ? AND state = ?")) {
                ps.setString(1, to);
                ps.setLong(2, clock.getAsLong());
                ps.setLong(3, id);
                ps.setString(4, claimOp);
                ps.setString(5, from);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("จดหมาย #" + id + " ไม่อยู่ในสถานะ " + from + " ของ op " + claimOp);
                }
            }
            return null;
        });
    }

    /** เรียกตอนเปิดปลั๊กอิน: รายการที่ค้าง CLAIMING จากรอบก่อน → REVIEW */
    public int quarantineInterrupted() throws SQLException {
        return database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE mail SET state = 'REVIEW', updated_at = ? WHERE state = 'CLAIMING'")) {
                ps.setLong(1, clock.getAsLong());
                return ps.executeUpdate();
            }
        });
    }

    public List<MailItem> review(int limit) throws SQLException {
        return database.read(connection -> query(connection,
                "SELECT * FROM mail WHERE state = 'REVIEW' ORDER BY id LIMIT ?", null, limit));
    }

    /**
     * ทีมงานตัดสินรายการ REVIEW: release=true คืนเป็น PENDING (ผู้เล่นยังไม่ได้ของ), false ปิดเป็น VOID (ได้ของไปแล้ว)
     */
    public boolean resolveReview(long id, boolean release, AuditEntry audit) throws SQLException {
        return database.transaction(connection -> {
            int changed;
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE mail SET state = ?, updated_at = ? WHERE id = ? AND state = 'REVIEW'")) {
                ps.setString(1, release ? "PENDING" : "VOID");
                ps.setLong(2, clock.getAsLong());
                ps.setLong(3, id);
                changed = ps.executeUpdate();
            }
            if (changed == 1) {
                AuditStore.insert(connection, audit, "mail#" + id, clock.getAsLong());
            }
            return changed == 1;
        });
    }

    public int countReview() throws SQLException {
        return database.read(connection -> {
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM mail WHERE state = 'REVIEW'")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private static List<MailItem> query(Connection connection, String sql, String player, int limit) throws SQLException {
        List<MailItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (player != null) {
                ps.setString(index++, player);
            }
            ps.setInt(index, Math.max(1, Math.min(limit, 200)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new MailItem(rs.getLong("id"), UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("source"), rs.getString("source_ref"), rs.getString("label"),
                            rs.getBytes("item_data"), rs.getString("state"), rs.getLong("created_at")));
                }
            }
        }
        return items;
    }
}
