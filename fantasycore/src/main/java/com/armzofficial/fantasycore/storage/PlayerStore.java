package com.armzofficial.fantasycore.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** ทะเบียน UUID ↔ ชื่อล่าสุด; ระบบอื่นอ้างอิงด้วย UUID เท่านั้น */
public final class PlayerStore {

    private final Database database;

    public PlayerStore(Database database) {
        this.database = database;
    }

    public void touch(UUID player, String name, long now) throws SQLException {
        database.transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO players(player_uuid, last_name, first_seen, last_seen) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(player_uuid) DO UPDATE SET last_name = excluded.last_name, last_seen = excluded.last_seen")) {
                ps.setString(1, player.toString());
                ps.setString(2, name);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** หา UUID จากชื่อล่าสุดที่เคยเข้าเซิร์ฟ (ไม่สนตัวพิมพ์) */
    public Optional<Known> findByName(String name) throws SQLException {
        return database.read(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_uuid, last_name FROM players WHERE last_name = ? COLLATE NOCASE "
                            + "ORDER BY last_seen DESC LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(new Known(UUID.fromString(rs.getString(1)), rs.getString(2)))
                            : Optional.empty();
                }
            }
        });
    }

    public record Known(UUID uuid, String name) {
    }
}
