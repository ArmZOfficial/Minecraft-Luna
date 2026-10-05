package com.armzofficial.fantasycore.item;

import com.armzofficial.fantasycore.exchange.ExchangeService;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class NativePotionTest {
    private YamlConfiguration defaults() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(Path.of("src/main/resources/items.yml")));
        return yaml;
    }

    private ItemTemplateService parse(YamlConfiguration yaml) {
        return new ItemTemplateService("fantasycore", yaml, template -> {});
    }

    @Test void recipesSelectActualNativeProfilesAndKeepGearArchives() throws Exception {
        var items = parse(defaults());
        assertTrue(items.problems().isEmpty());
        var recipes = new YamlConfiguration();
        recipes.loadFromString(Files.readString(Path.of("src/main/resources/alchemy.yml")));
        var expected = Map.of("lyra_moondew", PotionType.HEALING, "lyra_lifebloom", PotionType.REGENERATION,
                "lyra_starlight", PotionType.NIGHT_VISION);
        var configured = recipes.getConfigurationSection("recipes").getKeys(false);
        assertEquals(3, configured.size());
        var outputs = new HashSet<String>();
        for (String id : configured) {
            assertTrue(ExchangeService.Profile.ALCHEMY.acceptsRecipe(id));
            String root = "recipes." + id + ".";
            var template = items.template(recipes.getString(root + "output.template")).orElseThrow();
            assertTrue(outputs.add(template.id()));
            assertEquals(Material.POTION, template.material());
            assertEquals(expected.get(template.id()), template.potion().type());
            assertEquals(template.version(), recipes.getInt(root + "output.version"));
            assertTrue(template.serialized());
            assertTrue(template.enchantments().isEmpty());
            assertTrue(recipes.getInt(root + "gold-cost") > 0);
        }
        assertEquals(expected.keySet(), outputs);
        assertEquals(1, items.template("starter_runeblade", 1).orElseThrow().version());
    }

    @Test void malformedOrStrongerProfilesFailClosed() throws Exception {
        for (Object value : List.of("STRONG_HEALING", "LONG_REGENERATION", "STRENGTH", "healing", "", 1, true)) {
            var yaml = defaults(); yaml.set("templates.lyra_moondew.potion", value);
            assertTrue(parse(yaml).template("lyra_moondew").isEmpty());
            assertTrue(parse(yaml).template("starter_runeblade").isPresent());
        }
        var missing = defaults(); missing.set("templates.lyra_moondew.potion", null);
        assertTrue(parse(missing).template("lyra_moondew").isEmpty());
    }

    @Test void incompatibleMaterialsSerialPolicyAndEnchantCannotIssueMisleadingPotions() throws Exception {
        for (String material : List.of("PAPER", "SPLASH_POTION", "LINGERING_POTION", "TIPPED_ARROW")) {
            var yaml = defaults(); yaml.set("templates.lyra_moondew.material", material);
            assertTrue(parse(yaml).template("lyra_moondew").isEmpty());
        }
        var plain = defaults(); plain.set("templates.lyra_moondew.serialized", false);
        assertTrue(parse(plain).template("lyra_moondew").isEmpty());
        var enchanted = defaults(); enchanted.set("templates.lyra_moondew.enchantments.unbreaking", 1);
        assertTrue(parse(enchanted).template("lyra_moondew").isEmpty());
    }

    @Test void archivedPotionNeverInheritsANewerEffectProfile() throws Exception {
        var yaml = defaults();
        var original = yaml.getConfigurationSection("templates.lyra_moondew").getValues(false);
        yaml.createSection("templates.lyra_moondew.revisions.1", original);
        yaml.set("templates.lyra_moondew.version", 2);
        yaml.set("templates.lyra_moondew.potion", "NIGHT_VISION");
        var items = parse(yaml);
        assertEquals(NativePotion.HEALING, items.template("lyra_moondew", 1).orElseThrow().potion());
        assertEquals(NativePotion.NIGHT_VISION, items.template("lyra_moondew", 2).orElseThrow().potion());
        yaml.set("templates.lyra_moondew.revisions.1.potion", null);
        assertTrue(parse(yaml).template("lyra_moondew", 1).isEmpty());
    }

    @Test void reservedRecipeIdsKeepCraftQuotasSeparateWithoutBreakingExistingExchanges() {
        assertTrue(ExchangeService.Profile.ALCHEMY.acceptsRecipe("alchemy_moondew"));
        assertFalse(ExchangeService.Profile.CRAFT.acceptsRecipe("alchemy_moondew"));
        assertFalse(ExchangeService.Profile.ALCHEMY.acceptsRecipe("starter_runeblade"));
        assertTrue(ExchangeService.Profile.CRAFT.acceptsRecipe("starter_runeblade"));
        assertTrue(ExchangeService.Profile.EXCHANGE.acceptsRecipe("alchemy_old_custom_exchange"));
    }
}
