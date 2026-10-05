package com.armzofficial.fantasycore.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** schema ของ FantasyCore; เพิ่ม version ใหม่ต่อท้ายเท่านั้น ห้ามแก้ version ที่ปล่อยแล้ว */
public final class Migrations {

    private static final List<String[]> VERSIONS = List.<String[]>of(
            // version 1 — economy, ledger, audit, players, homes, items, stations, travel receipts
            new String[]{
                    """
                    CREATE TABLE players (
                        player_uuid TEXT PRIMARY KEY,
                        last_name   TEXT NOT NULL,
                        first_seen  INTEGER NOT NULL,
                        last_seen   INTEGER NOT NULL
                    )""",
                    """
                    CREATE TABLE accounts (
                        player_uuid TEXT NOT NULL,
                        bucket      TEXT NOT NULL,
                        balance     INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),
                        updated_at  INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, bucket)
                    )""",
                    """
                    CREATE TABLE operations (
                        op_id       TEXT PRIMARY KEY,
                        kind        TEXT NOT NULL,
                        player_uuid TEXT,
                        actor       TEXT NOT NULL,
                        status      TEXT NOT NULL,
                        detail      TEXT,
                        created_at  INTEGER NOT NULL
                    )""",
                    """
                    CREATE TABLE ledger (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        op_id         TEXT NOT NULL REFERENCES operations(op_id),
                        player_uuid   TEXT NOT NULL,
                        bucket        TEXT NOT NULL,
                        delta         INTEGER NOT NULL,
                        balance_after INTEGER NOT NULL,
                        reason        TEXT NOT NULL,
                        ref           TEXT,
                        created_at    INTEGER NOT NULL
                    )""",
                    "CREATE INDEX ledger_player ON ledger(player_uuid, id DESC)",
                    """
                    CREATE TABLE audit_log (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        actor_uuid  TEXT,
                        actor_name  TEXT NOT NULL,
                        action      TEXT NOT NULL,
                        target      TEXT,
                        detail      TEXT,
                        reason      TEXT,
                        op_id       TEXT,
                        created_at  INTEGER NOT NULL
                    )""",
                    "CREATE INDEX audit_target ON audit_log(target, id DESC)",
                    """
                    CREATE TABLE homes (
                        player_uuid TEXT NOT NULL,
                        home_key    TEXT NOT NULL,
                        display     TEXT NOT NULL,
                        world_uuid  TEXT NOT NULL,
                        world_name  TEXT NOT NULL,
                        x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                        yaw REAL NOT NULL, pitch REAL NOT NULL,
                        region_id   TEXT,
                        created_at  INTEGER NOT NULL,
                        updated_at  INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, home_key)
                    )""",
                    """
                    CREATE TABLE item_instances (
                        serial           TEXT PRIMARY KEY,
                        template_id      TEXT NOT NULL,
                        template_version INTEGER NOT NULL,
                        owner_uuid       TEXT,
                        op_id            TEXT NOT NULL UNIQUE,
                        issued_by        TEXT NOT NULL,
                        state            TEXT NOT NULL,
                        created_at       INTEGER NOT NULL,
                        updated_at       INTEGER NOT NULL
                    )""",
                    """
                    CREATE TABLE stations (
                        station_id  TEXT PRIMARY KEY,
                        action_id   TEXT NOT NULL,
                        kind        TEXT NOT NULL,
                        world_uuid  TEXT NOT NULL,
                        world_name  TEXT NOT NULL,
                        x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        entity_uuid TEXT,
                        label       TEXT,
                        created_by  TEXT NOT NULL,
                        created_at  INTEGER NOT NULL
                    )""",
                    """
                    CREATE TABLE travel_receipts (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        kind        TEXT NOT NULL,
                        world_name  TEXT NOT NULL,
                        x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,
                        created_at  INTEGER NOT NULL
                    )""",
                    "CREATE INDEX travel_player ON travel_receipts(player_uuid, kind, created_at DESC)"
            },
            // version 2 — กล่องจดหมาย + สิทธิ์รับรางวัล (CASUAL-SURVIVAL §4, SERVER-SYSTEMS §8 Mailbox)
            new String[]{
                    """
                    CREATE TABLE mail (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        source      TEXT NOT NULL,
                        source_ref  TEXT,
                        label       TEXT NOT NULL,
                        item_data   BLOB NOT NULL,
                        state       TEXT NOT NULL CHECK (state IN ('PENDING','CLAIMING','CLAIMED','REVIEW','VOID')),
                        claim_op    TEXT,
                        created_at  INTEGER NOT NULL,
                        updated_at  INTEGER NOT NULL
                    )""",
                    "CREATE INDEX mail_player ON mail(player_uuid, state, id)",
                    """
                    CREATE TABLE reward_claims (
                        player_uuid TEXT NOT NULL,
                        program     TEXT NOT NULL,
                        period      TEXT NOT NULL,
                        cycle_index INTEGER NOT NULL,
                        op_id       TEXT NOT NULL UNIQUE,
                        created_at  INTEGER NOT NULL,
                        PRIMARY KEY (player_uuid, program, period)
                    )"""
            },
            // version 3 — exchange journal; เก็บสูตร/ของ/slot ก่อนตัดไว้ให้ recovery ไม่อาศัย config ปัจจุบัน
            new String[]{
                    """
                    CREATE TABLE exchange_operations (
                        op_id TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        recipe_id TEXT NOT NULL,
                        recipe_version INTEGER NOT NULL CHECK (recipe_version > 0),
                        batch INTEGER NOT NULL CHECK (batch BETWEEN 1 AND 16),
                        period TEXT NOT NULL,
                        state TEXT NOT NULL CHECK (state IN ('PREPARED','CONSUMING','COMMITTED','CANCELLED','REVIEW')),
                        inputs TEXT NOT NULL,
                        slot_snapshot BLOB NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )""",
                    "CREATE INDEX exchange_quota ON exchange_operations(player_uuid, recipe_id, period, state)",
                    "CREATE UNIQUE INDEX exchange_active_player ON exchange_operations(player_uuid) WHERE state IN ('PREPARED','CONSUMING','REVIEW')",
                    """
                    CREATE TABLE exchange_outputs (
                        op_id TEXT NOT NULL REFERENCES exchange_operations(op_id),
                        ordinal INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        item_data BLOB NOT NULL,
                        mail_id INTEGER REFERENCES mail(id),
                        PRIMARY KEY (op_id, ordinal)
                    )"""
            },
            // version 4 — repair: จองค่าซ่อมก่อนแก้ item; รายการที่เริ่มแก้แล้วต้องตรวจเมื่อไม่ทราบผล
            new String[]{
                    """
                    CREATE TABLE repair_operations (
                        op_id TEXT PRIMARY KEY,
                        player_uuid TEXT NOT NULL,
                        provider TEXT NOT NULL CHECK (provider IN ('vanilla','core')),
                        template_id TEXT,
                        template_version INTEGER NOT NULL,
                        serial TEXT,
                        material TEXT NOT NULL,
                        slot INTEGER NOT NULL CHECK (slot BETWEEN 0 AND 8),
                        damage INTEGER NOT NULL CHECK (damage > 0),
                        max_damage INTEGER NOT NULL CHECK (max_damage > damage),
                        price INTEGER NOT NULL CHECK (price > 0),
                        before_data BLOB NOT NULL,
                        repaired_data BLOB NOT NULL,
                        state TEXT NOT NULL CHECK (state IN ('RESERVED','APPLYING','COMMITTED','CANCELLED','REVIEW')),
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )""",
                    "CREATE UNIQUE INDEX repair_active_player ON repair_operations(player_uuid) WHERE state IN ('RESERVED','APPLYING','REVIEW')",
                    "CREATE UNIQUE INDEX repair_active_serial ON repair_operations(serial) WHERE serial IS NOT NULL AND state IN ('RESERVED','APPLYING','REVIEW')"
            }
    );

    private Migrations() {
    }

    public static int latestVersion() {
        return VERSIONS.size();
    }

    /** คืนเลข version หลัง migrate */
    public static int apply(Database database) throws SQLException {
        return database.transaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
            }
            int current = currentVersion(connection);
            if (current > VERSIONS.size()) {
                throw new SQLException("ฐานข้อมูลเป็น schema v" + current + " ใหม่กว่าปลั๊กอินนี้ (v" + VERSIONS.size()
                        + ") — ห้ามย้อนเวอร์ชันปลั๊กอินโดยไม่กู้ backup");
            }
            for (int version = current + 1; version <= VERSIONS.size(); version++) {
                try (Statement statement = connection.createStatement()) {
                    for (String sql : VERSIONS.get(version - 1)) {
                        statement.execute(sql);
                    }
                    statement.execute("DELETE FROM schema_version");
                    statement.execute("INSERT INTO schema_version(version) VALUES (" + version + ")");
                }
            }
            return VERSIONS.size();
        });
    }

    static int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
