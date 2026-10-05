package com.armzofficial.fantasycore.combat;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** กฎความลึกและเพดาน; ไม่อ่านโลก/entity จึงทดสอบขอบชั้นได้โดยไม่เปิด Minecraft */
public record DepthDifficulty(List<Tier> tiers, double healthCap, double rawDamageCap) {
    public record Tier(String id, String name, int minY, double healthScale, double damageScale, String color) {
        public Tier {
            if (!id.matches("[a-z0-9_]{1,32}") || name.isBlank() || name.length() > 40
                    || minY < -64 || minY > 320 || !validScale(healthScale, 4) || !validScale(damageScale, 2)
                    || !Set.of("GREEN", "YELLOW", "RED", "PURPLE").contains(color)) {
                throw new IllegalArgumentException("tier id/name/Y/scale/color ผิดช่วง");
            }
        }
    }

    public DepthDifficulty {
        tiers = List.copyOf(tiers);
        if (tiers.isEmpty() || tiers.size() > 8 || !Double.isFinite(healthCap) || healthCap < 20 || healthCap > 1000
                || !Double.isFinite(rawDamageCap) || rawDamageCap < 1 || rawDamageCap > 40) {
            throw new IllegalArgumentException("ต้องมี 1–8 tiers, health-cap 20–1000, raw-damage-cap 1–40");
        }
        Set<String> ids = new HashSet<>();
        Tier previous = null;
        for (Tier tier : tiers) {
            if (!ids.add(tier.id()) || (previous != null && (tier.minY() >= previous.minY()
                    || tier.healthScale() < previous.healthScale() || tier.damageScale() < previous.damageScale()))) {
                throw new IllegalArgumentException("tiers ต้องเรียง Y สูงไปต่ำ ไม่มี ID/Y ซ้ำ และยิ่งลึกห้ามอ่อนลง");
            }
            previous = tier;
        }
    }

    public static DepthDifficulty load(ConfigurationSection config) {
        if (config == null) { throw new IllegalArgumentException("ไม่มี config มอนสเตอร์"); }
        var section = config.getConfigurationSection("tiers");
        if (section == null) { throw new IllegalArgumentException("ไม่มี tiers"); }
        List<Tier> tiers = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            var row = section.getConfigurationSection(id);
            if (row == null || !row.isInt("min-y") || !row.isString("name") || !row.isString("color")) {
                throw new IllegalArgumentException(id + ": name/min-y/color มีชนิดข้อมูลผิด");
            }
            tiers.add(new Tier(id, row.getString("name"), row.getInt("min-y"), number(row, "health-scale"),
                    number(row, "damage-scale"), row.getString("color")));
        }
        return new DepthDifficulty(tiers, number(config, "health-cap"), number(config, "raw-damage-cap"));
    }

    private static double number(ConfigurationSection row, String key) {
        if (!(row.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue())) {
            throw new IllegalArgumentException(key + " ต้องเป็นตัวเลข finite");
        }
        return value.doubleValue();
    }

    public Tier at(int y) {
        for (Tier tier : tiers) { if (y >= tier.minY()) { return tier; } }
        return tiers.getLast();
    }

    public double health(double base, Tier tier) {
        if (!Double.isFinite(base) || base <= 0) { throw new IllegalArgumentException("base health ผิดช่วง"); }
        return Math.min(healthCap, base * tier.healthScale());
    }

    /** cap ใช้เฉพาะส่วนที่ Core เพิ่ม ไม่ลดท่าโจมตี vanilla ที่แรงกว่า cap อยู่ก่อน */
    public static double damage(double raw, double scale, double cap) {
        if (!Double.isFinite(raw) || raw < 0 || !validScale(scale, 2) || !Double.isFinite(cap) || cap < 1 || cap > 40) {
            throw new IllegalArgumentException("damage snapshot ผิดช่วง");
        }
        return Math.max(raw, Math.min(cap, raw * scale));
    }

    private static boolean validScale(double value, double max) { return Double.isFinite(value) && value >= 1 && value <= max; }
}
