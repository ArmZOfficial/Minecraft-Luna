package com.armzofficial.fantasycore.item;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** ทะเบียน serial ของไอเทมสำคัญ: ISSUED → DELIVERED หรือ DELIVERY_FAILED */
public final class ItemInstanceStore {

    private final Database database;

    public ItemInstanceStore(Database database) {
        this.database = database;
    }

    public void issue(UUID serial, String templateId, int version, UUID owner, String opId, String issuedBy,
                      AuditEntry audit, long now) throws SQLException {
        database.transaction(connection -> {
            issueIn(connection, serial, templateId, version, owner, opId, issuedBy, "ISSUED", now);
            if (audit != null) {
                AuditStore.insert(connection, audit, opId, now);
            }
            return null;
        });
    }

    /** ออกทะเบียนใน transaction เดียวกับผลคราฟต์/จดหมาย — ห้ามเปิด transaction ซ้อน */
    public static void issueIn(Connection connection, UUID serial, String templateId, int version, UUID owner,
                               String opId, String issuedBy, String state, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO item_instances(serial, template_id, template_version, owner_uuid, op_id, issued_by, "
                        + "state, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, serial.toString());
            ps.setString(2, templateId);
            ps.setInt(3, version);
            ps.setString(4, owner == null ? null : owner.toString());
            ps.setString(5, opId);
            ps.setString(6, issuedBy);
            ps.setString(7, state);
            ps.setLong(8, now);
            ps.setLong(9, now);
            ps.executeUpdate();
        }
    }

    public void setState(UUID serial, String state, long now) throws SQLException {
        database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE item_instances SET state = ?, updated_at = ? WHERE serial = ?")) {
                ps.setString(1, state);
                ps.setLong(2, now);
                ps.setString(3, serial.toString());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Optional<Instance> find(UUID serial) throws SQLException {
        return database.read(connection -> findIn(connection, serial));
    }

    /** อ่านภายใน transaction ของบริการ item โดยไม่เปิด transaction ซ้อน */
    public static Optional<Instance> findIn(Connection connection, UUID serial) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT template_id, template_version, owner_uuid, state, issued_by, created_at FROM item_instances WHERE serial = ?")) {
            ps.setString(1, serial.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String owner = rs.getString(3);
                return Optional.of(new Instance(serial, rs.getString(1), rs.getInt(2),
                        owner == null ? null : UUID.fromString(owner), rs.getString(4), rs.getString(5),
                        rs.getLong(6)));
            }
        }
    }

    public record Instance(UUID serial, String templateId, int version, UUID owner, String state, String issuedBy,
                           long createdAt) {
    }
}
