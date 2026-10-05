package com.armzofficial.fantasycore.item;

import org.bukkit.Material;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * แม่แบบไอเทมของ Core: ตัวตนของไอเทมคือ template ID + version + serial ใน PDC ไม่ใช่ชื่อ/lore
 */
public record ItemTemplate(String id, int version, Material material, String name, List<String> lore,
                           boolean serialized, boolean glint, Map<String, Integer> enchantments) {
    public ItemTemplate {
        lore = List.copyOf(lore);
        enchantments = NativeEnchants.parse(new LinkedHashMap<>(enchantments));
    }

    public String describeEnchantments() {
        return NativeEnchants.describe(enchantments);
    }
}
