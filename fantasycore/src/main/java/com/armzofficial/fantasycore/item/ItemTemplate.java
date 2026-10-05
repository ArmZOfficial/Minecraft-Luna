package com.armzofficial.fantasycore.item;

import org.bukkit.Material;

import java.util.List;

/**
 * แม่แบบไอเทมของ Core: ตัวตนของไอเทมคือ template ID + version + serial ใน PDC ไม่ใช่ชื่อ/lore
 */
public record ItemTemplate(String id, int version, Material material, String name, List<String> lore,
                           boolean serialized, boolean glint) {
}
