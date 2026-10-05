package com.armzofficial.fantasycore.travel;

import com.armzofficial.fantasycore.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.UUID;

/** ใบรับการวาร์ปที่สำเร็จ — ใช้คิด cooldown ข้าม restart และให้แอดมินตรวจย้อนหลัง */
public final class TravelStore {

    private final Database database;

    public TravelStore(Database database) {
        this.database = database;
    }

    public void record(UUID player, String kind, String world, int x, int y, int z, long now) throws SQLException {
        database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO travel_receipts(player_uuid, kind, world_name, x, y, z, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, player.toString());
                ps.setString(2, kind);
                ps.setString(3, world);
                ps.setInt(4, x);
                ps.setInt(5, y);
                ps.setInt(6, z);
                ps.setLong(7, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public OptionalLong lastTime(UUID player, String kind) throws SQLException {
        return database.read(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT MAX(created_at) FROM travel_receipts WHERE player_uuid = ? AND kind = ?")) {
                ps.setString(1, player.toString());
                ps.setString(2, kind);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long value = rs.getLong(1);
                        return rs.wasNull() ? OptionalLong.empty() : OptionalLong.of(value);
                    }
                    return OptionalLong.empty();
                }
            }
        });
    }
}
