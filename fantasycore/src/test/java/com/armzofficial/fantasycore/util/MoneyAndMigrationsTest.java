package com.armzofficial.fantasycore.util;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyAndMigrationsTest {

    @Test
    void formatsAndParsesWholeNumbers() {
        assertEquals("1,234,567", Money.format(1_234_567));
        assertEquals(1_000, Money.parsePositive("1,000").orElseThrow());
        assertEquals(25_000, Money.parsePositive("25_000").orElseThrow());
        assertFalse(Money.parsePositive("0").isPresent());
        assertFalse(Money.parsePositive("-5").isPresent());
        assertFalse(Money.parsePositive("1.5").isPresent());
        assertFalse(Money.parsePositive("1e9").isPresent());
        assertFalse(Money.parsePositive("9999999999999999999").isPresent());
        assertTrue(Money.isAll("ALL"));
        assertTrue(Money.isAll("ทั้งหมด"));
    }

    @Test
    void migrationsAreIdempotent() throws Exception {
        Database database = TestDatabases.fresh();
        try {
            assertEquals(Migrations.latestVersion(), Migrations.apply(database));
            assertEquals(Migrations.latestVersion(), Migrations.apply(database));
        } finally {
            database.close();
        }
    }
}
