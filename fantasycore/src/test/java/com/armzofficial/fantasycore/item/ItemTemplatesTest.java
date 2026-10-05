package com.armzofficial.fantasycore.item;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemTemplatesTest {
    private YamlConfiguration defaults() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(Path.of("src/main/resources/items.yml")));
        return yaml;
    }

    private ItemTemplateService parse(YamlConfiguration yaml) {
        // Registry/ItemMeta ต้องพิสูจน์บน staging; unit test นี้ครอบคลุม parser/selection/snapshot จริง
        return new ItemTemplateService("fantasycore", yaml, template -> {});
    }

    @Test void starterProfilesAreRealEnchantDataAndOldVersionsAreStillSelectable() throws Exception {
        var catalog = parse(defaults());
        assertTrue(catalog.problems().isEmpty());
        assertEquals(6, catalog.templates().size());
        for (String id : new String[]{"starter_runeblade", "moonstone_pickaxe", "sentinel_chestplate"}) {
            var current = catalog.template(id).orElseThrow();
            var old = catalog.template(id, 1).orElseThrow();
            assertEquals(2, current.version());
            assertEquals(1, old.version());
            assertTrue(old.enchantments().isEmpty());
            assertTrue(old.lore().getLast().endsWith("v1"));
            assertEquals(current.material(), old.material());
            assertEquals(2, current.enchantments().get("minecraft:unbreaking"));
            assertTrue(catalog.template(id, 99).isEmpty());
        }
        assertEquals(2, catalog.template("starter_runeblade").orElseThrow().enchantments().get("minecraft:sharpness"));
        assertEquals(3, catalog.template("moonstone_pickaxe").orElseThrow().enchantments().get("minecraft:efficiency"));
        assertEquals(2, catalog.template("sentinel_chestplate").orElseThrow().enchantments().get("minecraft:protection"));
        assertTrue(catalog.template("missing", 1).isEmpty());
        assertTrue(catalog.template(null, 1).isEmpty());
    }

    @Test void invalidCurrentEnchantsDoNotStrandValidArchivedItemsOrOtherTemplates() throws Exception {
        var yaml = defaults();
        yaml.set("templates.starter_runeblade.enchantments.sharpness", 100);
        var catalog = parse(yaml);
        assertTrue(catalog.template("starter_runeblade").isEmpty());
        assertTrue(catalog.template("starter_runeblade", 2).isEmpty());
        assertTrue(catalog.template("starter_runeblade", 1).isPresent());
        assertEquals(5, catalog.templates().size());
        assertEquals(1, catalog.problems().size());
    }

    @Test void archiveMustBeCompleteAndNeverInheritsNewLoreOrFallsBackToCurrent() throws Exception {
        var yaml = defaults();
        yaml.set("templates.starter_runeblade.revisions.1.lore", null);
        var catalog = parse(yaml);
        assertTrue(catalog.template("starter_runeblade").isPresent());
        assertTrue(catalog.template("starter_runeblade", 1).isEmpty());
        assertEquals(1, catalog.problems().size());
    }

    @Test void duplicateCurrentVersionAndWrongArchiveVersionAreRejected() throws Exception {
        var yaml = defaults();
        var old = yaml.getConfigurationSection("templates.starter_runeblade.revisions.1").getValues(false);
        yaml.createSection("templates.starter_runeblade.revisions.2", old);
        yaml.set("templates.starter_runeblade.revisions.1.version", 7);
        var catalog = parse(yaml);
        assertEquals(2, catalog.template("starter_runeblade", 2).orElseThrow().version());
        assertTrue(catalog.template("starter_runeblade", 1).isEmpty());
        assertTrue(catalog.template("starter_runeblade", 7).isEmpty());
        assertEquals(2, catalog.problems().size());
    }

    @Test void registryValidationFailureQuarantinesOnlyAffectedRevision() throws Exception {
        var catalog = new ItemTemplateService("fantasycore", defaults(), template -> {
            if (template.material() == Material.IRON_PICKAXE && template.version() == 2) {
                throw new IllegalArgumentException("Paper registry says unsupported material");
            }
        });
        assertTrue(catalog.template("moonstone_pickaxe").isEmpty());
        assertTrue(catalog.template("moonstone_pickaxe", 1).isPresent());
        assertEquals(5, catalog.templates().size());
        assertTrue(catalog.problems().getFirst().contains("unsupported material"));
    }

    @Test void legacyMinimalConfigKeepsItsDefaultsAndNoInventedEnchants() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.set("templates.legacy.material", "IRON_SWORD");
        var template = parse(yaml).template("legacy").orElseThrow();
        assertEquals(1, template.version());
        assertEquals("legacy", template.name());
        assertTrue(template.serialized());
        assertFalse(template.glint());
        assertTrue(template.enchantments().isEmpty());
    }

    @Test void malformedValuesCannotSilentlyRemoveEnchantsOrCoerceVersions() throws Exception {
        for (var entry : Map.<String, Object>of("version", "2", "enchantments", "sharpness: 1", "serialized", "true").entrySet()) {
            var yaml = defaults();
            yaml.set("templates.starter_runeblade." + entry.getKey(), entry.getValue());
            assertTrue(parse(yaml).template("starter_runeblade").isEmpty(), entry.getKey());
        }
    }

    @Test void snapshotsAndLabelsDoNotDriftWhenSourceMapOrConfigChanges() throws Exception {
        var yaml = defaults();
        var catalog = parse(yaml);
        var blade = catalog.template("starter_runeblade").orElseThrow();
        yaml.set("templates.starter_runeblade.enchantments.sharpness", 3);
        assertEquals(2, blade.enchantments().get("minecraft:sharpness"));
        assertThrows(UnsupportedOperationException.class, () -> blade.enchantments().put("minecraft:sharpness", 3));
        assertThrows(UnsupportedOperationException.class, () -> blade.lore().clear());
        assertTrue(blade.describeEnchantments().contains("คมรูน (Sharpness II)"));
        assertTrue(blade.describeEnchantments().contains("Unbreaking II"));
        assertTrue(blade.describeEnchantments().contains("Sweeping Edge I"));
        assertEquals("ไม่มี enchant เริ่มต้น", catalog.template("starter_runeblade", 1).orElseThrow().describeEnchantments());
    }

    @Test void balanceParserRejectsDuplicateAliasesForbiddenEnchantAndNonIntegerLevels() {
        var duplicate = new LinkedHashMap<String, Object>();
        duplicate.put("sharpness", 1);
        duplicate.put("minecraft:sharpness", 1);
        assertThrows(IllegalArgumentException.class, () -> NativeEnchants.parse(duplicate));
        for (String id : new String[]{"mending", "foo:sharpness", "soul_speed", "SHARPNESS", "unknown"}) {
            assertThrows(IllegalArgumentException.class, () -> NativeEnchants.parse(Map.of(id, 1)), id);
        }
        for (Object level : new Object[]{0, -1, 5, 100, 1.0, "1", true, 1L}) {
            assertThrows(IllegalArgumentException.class, () -> NativeEnchants.parse(Map.of("sharpness", level)));
        }
        assertEquals(Map.of("minecraft:efficiency", 4), NativeEnchants.parse(Map.of("minecraft:efficiency", 4)));
    }

    @Test void draftBalanceProfilesRespectFactoryCapsAndStarterYamlMatchesTheDesign() throws Exception {
        var balance = JsonParser.parseString(Files.readString(Path.of("../server/content/library/balance.json"))).getAsJsonObject();
        var profiles = balance.getAsJsonObject("profiles");
        for (var profile : profiles.entrySet()) {
            for (String role : new String[]{"sword", "axe", "bow", "crossbow", "trident", "armor", "bootsExtra", "tool", "fishing"}) {
                Map<String, Object> configured = new LinkedHashMap<>();
                for (var enchant : profile.getValue().getAsJsonObject().getAsJsonObject(role).entrySet()) {
                    configured.put(enchant.getKey(), enchant.getValue().getAsBigDecimal().toBigIntegerExact().intValueExact());
                }
                assertDoesNotThrow(() -> NativeEnchants.parse(configured), profile.getKey() + " " + role);
            }
        }
        var catalog = parse(defaults());
        for (var entry : Map.of("starter_runeblade", "sword", "moonstone_pickaxe", "tool", "sentinel_chestplate", "armor").entrySet()) {
            Map<String, Object> planned = new LinkedHashMap<>();
            profiles.getAsJsonObject("adventurer").getAsJsonObject(entry.getValue()).entrySet()
                    .forEach(enchant -> planned.put(enchant.getKey(), enchant.getValue().getAsInt()));
            assertEquals(NativeEnchants.parse(planned), catalog.template(entry.getKey()).orElseThrow().enchantments());
        }
    }
}
