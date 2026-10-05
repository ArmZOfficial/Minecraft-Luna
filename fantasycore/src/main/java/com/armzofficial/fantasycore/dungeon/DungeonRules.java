package com.armzofficial.fantasycore.dungeon;

import org.bukkit.configuration.ConfigurationSection;

public record DungeonRules(boolean enabled,boolean partyEnabled,int timeoutSeconds,double hallHealth,double reliquaryHealth,double bossHealth) {
    public static DungeonRules load(ConfigurationSection c) {
        if (!c.isBoolean("enabled") || !(c.get("timeout-seconds") instanceof Integer seconds) || seconds<60 || seconds>1800) {
            throw new IllegalArgumentException("enabled ต้องเป็น boolean, timeout-seconds ต้องเป็น integer 60..1800");
        }
        double hall=health(c,"hall-health"), relic=health(c,"reliquary-health"), boss=health(c,"boss-health");
        if (hall>relic || relic>boss) { throw new IllegalArgumentException("HP ต้องเพิ่มตามความลึก"); }
        if(c.contains("party-enabled") && !c.isBoolean("party-enabled")) { throw new IllegalArgumentException("party-enabled ต้องเป็น boolean"); }
        return new DungeonRules(c.getBoolean("enabled"),c.getBoolean("party-enabled",false),seconds,hall,relic,boss);
    }
    private static double health(ConfigurationSection c,String key) {
        if (!(c.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<20 || n.doubleValue()>500) {
            throw new IllegalArgumentException(key+" ต้องเป็นตัวเลข 20..500");
        }
        return n.doubleValue();
    }
    public static double scaledHealth(double base,int members) {
        validateScale(base,members); return Math.min(1000,base*(1+0.6*(members-1)));
    }
    public static double scaledAttack(double base,int members) { validateScale(base,members); return base*(1+0.12*(members-1)); }
    private static void validateScale(double base,int members) {
        if(!Double.isFinite(base) || base<=0 || members<1 || members>4) { throw new IllegalArgumentException("encounter scale"); }
    }
}
