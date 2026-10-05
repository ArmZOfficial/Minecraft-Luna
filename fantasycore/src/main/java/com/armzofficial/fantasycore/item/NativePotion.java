package com.armzofficial.fantasycore.item;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/** Three drinkable vanilla profiles; effect strength/duration comes from the server potion type. */
public enum NativePotion {
    HEALING(PotionType.HEALING, 0xEF6471, "ฟื้นพลัง I · ผลมาตรฐาน Minecraft"),
    REGENERATION(PotionType.REGENERATION, 0x47C8D3, "ฟื้นฟูต่อเนื่อง I · ระยะเวลามาตรฐาน Minecraft"),
    NIGHT_VISION(PotionType.NIGHT_VISION, 0xAC79FF, "มองกลางคืน · ระยะเวลามาตรฐาน Minecraft");

    private final PotionType type;
    private final int rgb;
    private final String description;

    NativePotion(PotionType type, int rgb, String description) {
        this.type = type;
        this.rgb = rgb;
        this.description = description;
    }

    public PotionType type() { return type; }
    public String description() { return description; }

    public void apply(PotionMeta meta) {
        meta.setBasePotionType(type);
        meta.setColor(Color.fromRGB(rgb));
    }

    public static NativePotion parse(Object value) {
        if (!(value instanceof String name)) { throw new IllegalArgumentException("potion ต้องเป็น HEALING/REGENERATION/NIGHT_VISION"); }
        try { return valueOf(name); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("potion รองรับ HEALING/REGENERATION/NIGHT_VISION เท่านั้น"); }
    }

    static void validate(Material material, boolean serialized, NativePotion potion) {
        if (potion != null) {
            if (material != Material.POTION || !serialized) {
                throw new IllegalArgumentException("potion ต้องใช้ material POTION และ serialized: true");
            }
        } else if (material == Material.POTION || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION || material == Material.TIPPED_ARROW) {
            throw new IllegalArgumentException("แม่แบบยาต้องระบุ potion; รุ่นนี้รองรับยาดื่ม POTION เท่านั้น");
        }
    }
}
