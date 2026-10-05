package com.armzofficial.fantasycore.audit;

import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public final class AuditStore {

    private final Database database;
    private final LongSupplier clock;

    public AuditStore(Database database, LongSupplier clock) {
        this.database = database;
        this.clock = clock;
    }

    public void record(AuditEntry entry, String opId) throws SQLException {
        long now = clock.getAsLong();
        database.transaction(connection -> {
            insert(connection, entry, opId, now);
            return null;
        });
    }

    /** ใช้ภายใน transaction ของงานอื่น เพื่อให้ audit กับผลงาน commit พร้อมกัน */
    public static void insert(Connection connection, AuditEntry entry, String opId, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO audit_log(actor_uuid, actor_name, action, target, detail, reason, op_id, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, entry.actorUuid());
            ps.setString(2, entry.actorName());
            ps.setString(3, entry.action());
            ps.setString(4, entry.target());
            ps.setString(5, entry.detail());
            ps.setString(6, entry.reason());
            ps.setString(7, opId);
            ps.setLong(8, now);
            ps.executeUpdate();
        }
    }

    public List<Row> recent(String target, int limit) throws SQLException {
        return database.read(connection -> {
            String sql = target == null
                    ? "SELECT id, actor_name, action, target, detail, reason, op_id, created_at FROM audit_log ORDER BY id DESC LIMIT ?"
                    : "SELECT id, actor_name, action, target, detail, reason, op_id, created_at FROM audit_log WHERE target = ? ORDER BY id DESC LIMIT ?";
            List<Row> rows = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int index = 1;
                if (target != null) {
                    ps.setString(index++, target);
                }
                ps.setInt(index, Math.max(1, Math.min(limit, 50)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getString(5), rs.getString(6), rs.getString(7), rs.getLong(8)));
                    }
                }
            }
            return rows;
        });
    }

    public record Row(long id, String actorName, String action, String target, String detail, String reason,
                      String opId, long createdAt) {
    }
}
