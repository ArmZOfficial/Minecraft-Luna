package com.armzofficial.fantasycore.combat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepthDifficultyTest {
    private YamlConfiguration config() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(Path.of("src/main/resources/monsters.yml")));
        return yaml;
    }

    @Test void boundariesUseTheCorrectTierAndKeepLegacyReachableDepths() throws Exception {
        var rules = DepthDifficulty.load(config());
        assertEquals("surface", rules.at(100).id());
        assertEquals("surface", rules.at(64).id());
        assertEquals("cavern", rules.at(63).id());
        assertEquals("cavern", rules.at(32).id());
        assertEquals("deep", rules.at(31).id());
        assertEquals("deep", rules.at(16).id());
        assertEquals("abyss", rules.at(15).id());
        assertEquals("abyss", rules.at(0).id());
        assertEquals("abyss", rules.at(-64).id());
    }

    @Test void deeperHasMoreHealthAndDamageWithoutExponentialCompounding() throws Exception {
        var rules = DepthDifficulty.load(config());
        assertEquals(20, rules.health(20, rules.at(64)));
        assertEquals(30, rules.health(20, rules.at(32)));
        assertEquals(44, rules.health(20, rules.at(16)));
        assertEquals(64, rules.health(20, rules.at(0)));
        assertEquals(200, rules.health(100, rules.at(0)));
        assertEquals(7.2, DepthDifficulty.damage(4, rules.at(0).damageScale(), rules.rawDamageCap()), 0.000001);
        assertEquals(12, DepthDifficulty.damage(10, 1.8, 12));
        assertEquals(15, DepthDifficulty.damage(15, 1.8, 12)); // ไม่ลดท่า vanilla ที่แรงอยู่ก่อน
        assertEquals(0, DepthDifficulty.damage(0, 1.8, 12));
    }

    @Test void duplicateYIdsReversedOrderAndWeakerDeepTierAreRejected() throws Exception {
        var rules = DepthDifficulty.load(config());
        var top = rules.tiers().getFirst();
        for (var bad : List.of(
                new DepthDifficulty.Tier("other", "อื่น", 64, 1.5, 1.2, "RED"),
                new DepthDifficulty.Tier("other", "อื่น", 70, 1.5, 1.2, "RED"),
                new DepthDifficulty.Tier("surface", "อื่น", 32, 1.5, 1.2, "RED"))) {
            assertThrows(IllegalArgumentException.class, () -> new DepthDifficulty(List.of(top, bad), 200, 12));
        }
        var cavern = rules.tiers().get(1);
        var weak = new DepthDifficulty.Tier("weak", "อ่อน", 16, 1.1, 1.1, "RED");
        assertThrows(IllegalArgumentException.class, () -> new DepthDifficulty(List.of(cavern, weak), 200, 12));
    }

    @Test void nonFiniteScalesAndUnsafeCapsCannotEnterDamageOrHealth() throws Exception {
        var rules = DepthDifficulty.load(config());
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            assertThrows(IllegalArgumentException.class, () -> rules.health(value, rules.at(0)));
            assertThrows(IllegalArgumentException.class, () -> DepthDifficulty.damage(value, 1.8, 12));
        }
        assertThrows(IllegalArgumentException.class, () -> rules.health(0, rules.at(0)));
        for (double scale : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0.5, 3}) {
            assertThrows(IllegalArgumentException.class, () -> DepthDifficulty.damage(4, scale, 12));
        }
        assertThrows(IllegalArgumentException.class, () -> new DepthDifficulty(rules.tiers(), 0, 12));
        assertThrows(IllegalArgumentException.class, () -> new DepthDifficulty(rules.tiers(), 200, 100));
        assertThrows(IllegalArgumentException.class, () -> new DepthDifficulty(List.of(), 200, 12));
    }

    @Test void configDoesNotCoerceStringsFractionsOrUnknownColors() throws Exception {
        for (var values : List.of(new Object[]{"health-cap", "200"}, new Object[]{"tiers.deep.min-y", 16.5},
                new Object[]{"tiers.deep.health-scale", 5}, new Object[]{"tiers.deep.color", "BLACK"})) {
            var yaml = config();
            yaml.set((String) values[0], values[1]);
            assertThrows(IllegalArgumentException.class, () -> DepthDifficulty.load(yaml));
        }
    }
}
