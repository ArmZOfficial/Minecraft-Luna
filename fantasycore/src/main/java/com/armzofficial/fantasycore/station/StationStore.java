package com.armzofficial.fantasycore.station;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StationStore {

    private final Database database;

    public StationStore(Database database) {
        this.database = database;
    }

    public List<StationRecord> all() throws SQLException {
        return database.read(connection -> {
            List<StationRecord> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM stations ORDER BY created_at");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String entity = rs.getString("entity_uuid");
                    list.add(new StationRecord(
                            UUID.fromString(rs.getString("station_id")),
                            rs.getString("action_id"),
                            StationRecord.Kind.valueOf(rs.getString("kind")),
                            UUID.fromString(rs.getString("world_uuid")),
                            rs.getString("world_name"),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            entity == null ? null : UUID.fromString(entity),
                            rs.getString("label"),
                            rs.getString("created_by"),
                            rs.getLong("created_at")));
                }
            }
            return list;
        });
    }

    public void insert(StationRecord station, AuditEntry audit) throws SQLException {
        database.transaction(connection -> {
            insertIn(connection, station, audit);
            return null;
        });
    }

    private static void insertIn(Connection connection, StationRecord station, AuditEntry audit) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO stations(station_id, action_id, kind, world_uuid, world_name, x, y, z, yaw, entity_uuid, "
                        + "label, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, station.id().toString());
            ps.setString(2, station.actionId());
            ps.setString(3, station.kind().name());
            ps.setString(4, station.worldId().toString());
            ps.setString(5, station.worldName());
            ps.setDouble(6, station.x());
            ps.setDouble(7, station.y());
            ps.setDouble(8, station.z());
            ps.setFloat(9, station.yaw());
            ps.setString(10, station.entityId() == null ? null : station.entityId().toString());
            ps.setString(11, station.label());
            ps.setString(12, station.createdBy());
            ps.setLong(13, station.createdAt());
            ps.executeUpdate();
        }
        AuditStore.insert(connection, audit, station.id().toString(), station.createdAt());
    }

    /** ผูก NPC ของ Citizens กับ action — NPC หนึ่งตัวมีได้ action เดียว ผูกใหม่จะแทนของเดิมใน transaction เดียว */
    public void replaceCitizensBinding(StationRecord station, AuditEntry audit) throws SQLException {
        database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM stations WHERE kind = 'CITIZENS' AND entity_uuid = ?")) {
                ps.setString(1, station.entityId().toString());
                ps.executeUpdate();
            }
            insertIn(connection, station, audit);
            return null;
        });
    }

    public int deleteCitizensBinding(UUID npcUuid, AuditEntry audit, long now) throws SQLException {
        return database.transaction(connection -> {
            int changed;
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM stations WHERE kind = 'CITIZENS' AND entity_uuid = ?")) {
                ps.setString(1, npcUuid.toString());
                changed = ps.executeUpdate();
            }
            if (changed > 0) {
                AuditStore.insert(connection, audit, npcUuid.toString(), now);
            }
            return changed;
        });
    }

    public boolean delete(UUID stationId, AuditEntry audit, long now) throws SQLException {
        return database.transaction(connection -> {
            int changed;
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM stations WHERE station_id = ?")) {
                ps.setString(1, stationId.toString());
                changed = ps.executeUpdate();
            }
            if (changed > 0) {
                AuditStore.insert(connection, audit, stationId.toString(), now);
            }
            return changed > 0;
        });
    }
}
