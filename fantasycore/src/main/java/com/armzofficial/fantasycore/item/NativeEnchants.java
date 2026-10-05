package com.armzofficial.fantasycore.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** เพดานของ native enchants ตาม BALANCE-th; ไม่เพิ่ม stat/trait จาก lore หรือเงินสด */
public final class NativeEnchants {
    private record Rule(String name, String english, int cap) {}

    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("sweeping_edge", new Rule("วงคมจันทร์", "Sweeping Edge", 3)),
            Map.entry("fortune", new Rule("พรแร่ดารา", "Fortune", 3)),
            Map.entry("thorns", new Rule("หนามผู้พิทักษ์", "Thorns", 2)),
            Map.entry("sharpness", new Rule("คมรูน", "Sharpness", 4)),
            Map.entry("unbreaking", new Rule("ผนึกความทนทาน", "Unbreaking", 3)),
            Map.entry("power", new Rule("สายธนูดารา", "Power", 4)),
            Map.entry("quick_charge", new Rule("กลไกเร่งรูน", "Quick Charge", 3)),
            Map.entry("loyalty", new Rule("พันธะผู้ถือ", "Loyalty", 3)),
            Map.entry("protection", new Rule("เกราะผนึกภัย", "Protection", 4)),
            Map.entry("feather_falling", new Rule("ย่างเท้าขนนก", "Feather Falling", 4)),
            Map.entry("efficiency", new Rule("แรงช่างรูน", "Efficiency", 5)),
            Map.entry("luck_of_the_sea", new Rule("พรแห่งวารี", "Luck of the Sea", 2)),
            Map.entry("lure", new Rule("เสียงเรียกฝูงปลา", "Lure", 2)));

    private NativeEnchants() {}

    public static Map<String, Integer> parse(Map<String, Object> values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            String id = entry.getKey().startsWith("minecraft:") ? entry.getKey().substring(10) : entry.getKey();
            Rule rule = RULES.get(id);
            if (rule == null) {
                throw new IllegalArgumentException("enchant ไม่อยู่ในชุดบาลานซ์: " + entry.getKey());
            }
            if (!(entry.getValue() instanceof Integer level) || level < 1 || level > rule.cap()) {
                throw new IllegalArgumentException(id + " level ต้องเป็นจำนวนเต็ม 1–" + rule.cap());
            }
            if (result.putIfAbsent("minecraft:" + id, level) != null) {
                throw new IllegalArgumentException("enchant ซ้ำ: " + id);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static String describe(Map<String, Integer> enchantments) {
        if (enchantments.isEmpty()) { return "ไม่มี enchant เริ่มต้น"; }
        return String.join(" + ", enchantments.entrySet().stream().map(entry -> {
            Rule rule = RULES.get(entry.getKey().substring(10));
            return rule.name() + " (" + rule.english() + " " + switch (entry.getValue()) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> throw new IllegalArgumentException("enchant level นอกเพดาน");
            } + ")";
        }).toList());
    }
}
