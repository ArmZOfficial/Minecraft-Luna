package com.armzofficial.fantasycore;

import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database fresh() throws Exception {
        Path dir = Files.createTempDirectory("fantasycore-test");
        Database database = Database.open(dir.resolve("test.db"));
        Migrations.apply(database);
        return database;
    }
}
