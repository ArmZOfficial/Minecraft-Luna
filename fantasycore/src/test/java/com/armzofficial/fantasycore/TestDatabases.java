package com.armzofficial.fantasycore;

import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database fresh() throws Exception {
        Path dir = Files.createTempDirectory("fantasycore-test");
        Database database = Database.open(dir.resolve("test.db"));
        Migrations.apply(database);
        return database;
    }

    /** สร้าง fixture schema ก่อน v5 โดยรักษา rows เดิมไว้ */
    public static void removeCraftColumns(Connection c) throws SQLException {
        removeDungeonTables(c);
        try (var st = c.createStatement()) {
            st.execute("DROP INDEX exchange_output_serial");
            st.execute("ALTER TABLE exchange_outputs DROP COLUMN serial");
            st.execute("ALTER TABLE exchange_outputs DROP COLUMN template_id");
            st.execute("ALTER TABLE exchange_outputs DROP COLUMN template_version");
            st.execute("ALTER TABLE exchange_operations DROP COLUMN kind");
            st.execute("ALTER TABLE exchange_operations DROP COLUMN gold_cost");
        }
    }
    public static void removeDungeonTables(Connection c) throws SQLException {
        try(var st=c.createStatement()) { st.execute("DROP TABLE dungeon_group_rewards"); st.execute("DROP TABLE dungeon_group_members"); st.execute("DROP TABLE dungeon_group_runs"); st.execute("DROP TABLE dungeon_rewards"); st.execute("DROP TABLE dungeon_runs"); }
    }
}
