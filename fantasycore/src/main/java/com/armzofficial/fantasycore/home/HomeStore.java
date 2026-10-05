package com.armzofficial.fantasycore.home;

import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HomeStore {

    public enum SaveResult {
        CREATED,
        UPDATED,
        /** มีชื่อนี้แล้ว ต้องยืนยันก่อนย้าย */
        NEEDS_CONFIRM,
        LIMIT_REACHED
    }

    private final Database database;

    public HomeStore(Database database) {
        this.database = database;
    }

    public List<HomeRecord> list(UUID owner) throws SQLException {
        return database.read(connection -> {
            List<HomeRecord> homes = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM homes WHERE player_uuid = ? ORDER BY created_at, home_key")) {
                ps.setString(1, owner.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        homes.add(map(rs));
                    }
                }
            }
            return homes;
        });
    }

    public Optional<HomeRecord> get(UUID owner, String key) throws SQLException {
        return database.read(connection -> find(connection, owner, key));
    }

    /**
     * บันทึกใน transaction เดียว: ตรวจชื่อซ้ำ/โควตาแล้วเขียน — คำสั่งพร้อมกันสองครั้งเกินโควตาไม่ได้
     */
    public SaveResult save(HomeRecord home, int limit, boolean confirmOverwrite) throws SQLException {
        return database.transaction(connection -> {
            Optional<HomeRecord> existing = find(connection, home.owner(), home.key());
            if (existing.isPresent()) {
                if (!confirmOverwrite) {
                    return SaveResult.NEEDS_CONFIRM;
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE homes SET display = ?, world_uuid = ?, world_name = ?, x = ?, y = ?, z = ?, yaw = ?, "
                                + "pitch = ?, region_id = ?, updated_at = ? WHERE player_uuid = ? AND home_key = ?")) {
                    ps.setString(1, home.display());
                    ps.setString(2, home.worldId().toString());
                    ps.setString(3, home.worldName());
                    ps.setDouble(4, home.x());
                    ps.setDouble(5, home.y());
                    ps.setDouble(6, home.z());
                    ps.setFloat(7, home.yaw());
                    ps.setFloat(8, home.pitch());
                    ps.setString(9, home.regionId());
                    ps.setLong(10, home.updatedAt());
                    ps.setString(11, home.owner().toString());
                    ps.setString(12, home.key());
                    ps.executeUpdate();
                }
                return SaveResult.UPDATED;
            }
            if (count(connection, home.owner()) >= limit) {
                return SaveResult.LIMIT_REACHED;
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO homes(player_uuid, home_key, display, world_uuid, world_name, x, y, z, yaw, pitch, "
                            + "region_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, home.owner().toString());
                ps.setString(2, home.key());
                ps.setString(3, home.display());
                ps.setString(4, home.worldId().toString());
                ps.setString(5, home.worldName());
                ps.setDouble(6, home.x());
                ps.setDouble(7, home.y());
                ps.setDouble(8, home.z());
                ps.setFloat(9, home.yaw());
                ps.setFloat(10, home.pitch());
                ps.setString(11, home.regionId());
                ps.setLong(12, home.createdAt());
                ps.setLong(13, home.updatedAt());
                ps.executeUpdate();
            }
            return SaveResult.CREATED;
        });
    }

    public boolean delete(UUID owner, String key) throws SQLException {
        return database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM homes WHERE player_uuid = ? AND home_key = ?")) {
                ps.setString(1, owner.toString());
                ps.setString(2, key);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public int count(UUID owner) throws SQLException {
        return database.read(connection -> count(connection, owner));
    }

    private static int count(Connection connection, UUID owner) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM homes WHERE player_uuid = ?")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static Optional<HomeRecord> find(Connection connection, UUID owner, String key) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM homes WHERE player_uuid = ? AND home_key = ?")) {
            ps.setString(1, owner.toString());
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static HomeRecord map(ResultSet rs) throws SQLException {
        return new HomeRecord(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("home_key"),
                rs.getString("display"),
                UUID.fromString(rs.getString("world_uuid")),
                rs.getString("world_name"),
                rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"),
                rs.getString("region_id"),
                rs.getLong("created_at"),
                rs.getLong("updated_at"));
    }
}
