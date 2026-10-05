package com.armzofficial.fantasycore.dungeon;

import org.bukkit.configuration.ConfigurationSection;

public record DungeonRules(boolean enabled,int timeoutSeconds,double hallHealth,double reliquaryHealth,double bossHealth) {
    public static DungeonRules load(ConfigurationSection c) {
        if (!c.isBoolean("enabled") || !(c.get("timeout-seconds") instanceof Integer seconds) || seconds<60 || seconds>1800) {
            throw new IllegalArgumentException("enabled ต้องเป็น boolean, timeout-seconds ต้องเป็น integer 60..1800");
        }
        double hall=health(c,"hall-health"), relic=health(c,"reliquary-health"), boss=health(c,"boss-health");
        if (hall>relic || relic>boss) { throw new IllegalArgumentException("HP ต้องเพิ่มตามความลึก"); }
        return new DungeonRules(c.getBoolean("enabled"),seconds,hall,relic,boss);
    }
    private static double health(ConfigurationSection c,String key) {
        if (!(c.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<20 || n.doubleValue()>500) {
            throw new IllegalArgumentException(key+" ต้องเป็นตัวเลข 20..500");
        }
        return n.doubleValue();
    }
}
