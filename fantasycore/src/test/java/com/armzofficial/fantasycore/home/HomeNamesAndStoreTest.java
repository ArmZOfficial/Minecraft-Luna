package com.armzofficial.fantasycore.home;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.storage.Database;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeNamesAndStoreTest {

    @Test
    void acceptsThaiAndAsciiNames() {
        assertEquals("บ้านริมน้ำ", HomeNames.parse("บ้านริมน้ำ").orElseThrow().key());
        assertEquals("farm_2", HomeNames.parse("Farm_2").orElseThrow().key());
        assertEquals("Farm_2", HomeNames.parse("Farm_2").orElseThrow().display());
        assertTrue(HomeNames.parse("a-b").isPresent());
    }

    @Test
    void rejectsUnsafeOrLongNames() {
        assertFalse(HomeNames.parse("").isPresent());
        assertFalse(HomeNames.parse("a b").isPresent());
        assertFalse(HomeNames.parse("x'; DROP TABLE homes;--").isPresent());
        assertFalse(HomeNames.parse("../etc").isPresent());
        assertFalse(HomeNames.parse("§cred").isPresent());
        assertFalse(HomeNames.parse("12345678901234567").isPresent());
        assertTrue(HomeNames.parse("1234567890123456").isPresent());
    }

    private static HomeRecord home(UUID owner, String key, double x) {
        return new HomeRecord(owner, key, key, UUID.randomUUID(), "luma_housing", x, 70, 0, 0, 0, "ps1x2y3z", 1, 1);
    }

    @Test
    void enforcesLimitAndConfirmation() throws Exception {
        Database database = TestDatabases.fresh();
        try {
            HomeStore store = new HomeStore(database);
            UUID owner = UUID.randomUUID();
            assertEquals(HomeStore.SaveResult.CREATED, store.save(home(owner, "home", 1), 1, false));
            assertEquals(HomeStore.SaveResult.LIMIT_REACHED, store.save(home(owner, "farm", 2), 1, false));
            assertEquals(HomeStore.SaveResult.NEEDS_CONFIRM, store.save(home(owner, "home", 5), 1, false));
            assertEquals(1.0, store.get(owner, "home").orElseThrow().x());
            assertEquals(HomeStore.SaveResult.UPDATED, store.save(home(owner, "home", 5), 1, true));
            assertEquals(5.0, store.get(owner, "home").orElseThrow().x());
            assertEquals(1, store.count(owner));
            assertTrue(store.delete(owner, "home"));
            assertFalse(store.delete(owner, "home"));
            assertEquals(0, store.list(owner).size());
        } finally {
            database.close();
        }
    }
}
