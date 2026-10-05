package com.armzofficial.fantasycore.config;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * snapshot ของ config.yml ที่ตรวจค่าแล้ว — โค้ดส่วนอื่นอ่านจากที่นี่ ไม่อ่าน FileConfiguration ตรง
 */
public record Settings(
        String databaseFile,
        long maxTransaction,
        List<Long> bankQuickAmounts,
        DeathRule defaultDeathRule,
        Map<String, DeathRule> deathRules,
        Map<String, WorldSpec> worlds,
        String spawnWorld,
        int warmupSeconds,
        int combatLockSeconds,
        int safeMinY,
        int safeMaxY,
        List<String> homeWorlds,
        boolean homeRequiresClaim,
        int homeDefaultLimit,
        Map<String, Integer> homeLimitPermissions,
        int homeMaxLimit,
        String claimProvider,
        List<String> playerRegionPrefixes,
        int rtpClaimBuffer,
        int rtpCooldownSeconds,
        int rtpFailureRetrySeconds,
        int rtpMaxCandidates,
        int rtpTimeoutSeconds,
        int rtpMaxChunkJobsPerWorld,
        Map<String, RtpProfile> rtpProfiles,
        double stationRadius) {

    public record DeathRule(int goldLossPercent, boolean keepInventory) {
    }

    public record WorldSpec(String name, boolean create, World.Environment environment, Long seed,
                            int borderCenterX, int borderCenterZ, int borderRadius) {
    }

    public record RtpProfile(String id, String world, String display, int centerX, int centerZ,
                             int minRadius, int maxRadius) {
    }

    public DeathRule deathRule(String worldName) {
        return deathRules.getOrDefault(worldName, defaultDeathRule);
    }

    public static Settings load(FileConfiguration c, Logger log) {
        List<Long> quick = new ArrayList<>();
        for (Object value : c.getList("economy.bank.quick-amounts", List.of(100, 1000, 10000))) {
            if (value instanceof Number number && number.longValue() > 0) {
                quick.add(number.longValue());
            }
        }
        if (quick.isEmpty()) {
            quick = List.of(100L, 1000L, 10000L);
        }

        DeathRule defaultRule = deathRule(c.getConfigurationSection("death.default"), new DeathRule(0, false), log, "default");
        Map<String, DeathRule> deathRules = new LinkedHashMap<>();
        ConfigurationSection deathWorlds = c.getConfigurationSection("death.worlds");
        if (deathWorlds != null) {
            for (String world : deathWorlds.getKeys(false)) {
                deathRules.put(world, deathRule(deathWorlds.getConfigurationSection(world), defaultRule, log, world));
            }
        }

        Map<String, WorldSpec> worlds = new LinkedHashMap<>();
        ConfigurationSection worldSection = c.getConfigurationSection("worlds");
        if (worldSection != null) {
            for (String name : worldSection.getKeys(false)) {
                ConfigurationSection w = worldSection.getConfigurationSection(name);
                if (w == null) {
                    continue;
                }
                World.Environment env;
                try {
                    env = World.Environment.valueOf(w.getString("environment", "NORMAL").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    log.warning("worlds." + name + ".environment ไม่ถูกต้อง ใช้ NORMAL แทน");
                    env = World.Environment.NORMAL;
                }
                String seedText = w.getString("seed", "");
                Long seed = null;
                if (seedText != null && !seedText.isBlank()) {
                    try {
                        seed = Long.parseLong(seedText.trim());
                    } catch (NumberFormatException e) {
                        seed = (long) seedText.trim().hashCode();
                    }
                }
                worlds.put(name, new WorldSpec(name, w.getBoolean("create", true), env, seed,
                        w.getInt("border.center-x", 0), w.getInt("border.center-z", 0),
                        Math.max(0, w.getInt("border.radius", 0))));
            }
        }

        Map<String, Integer> limitPerms = new LinkedHashMap<>();
        ConfigurationSection limits = c.getConfigurationSection("homes.limits");
        if (limits != null) {
            // คีย์ใน YAML ใช้จุดไม่ได้ จึงเก็บเป็น "fantasy_home_limit_3" แล้วแปลง _ เป็น .
            for (String key : limits.getKeys(false)) {
                limitPerms.put(key.replace('_', '.'), Math.max(0, limits.getInt(key)));
            }
        }

        int safeMin = c.getInt("travel.safe-y.min", 16);
        int safeMax = c.getInt("travel.safe-y.max", 200);
        if (safeMax <= safeMin) {
            log.warning("travel.safe-y ไม่ถูกต้อง ใช้ 16..200");
            safeMin = 16;
            safeMax = 200;
        }

        Map<String, RtpProfile> profiles = new LinkedHashMap<>();
        ConfigurationSection rtp = c.getConfigurationSection("rtp.profiles");
        if (rtp != null) {
            for (String id : rtp.getKeys(false)) {
                ConfigurationSection p = rtp.getConfigurationSection(id);
                if (p == null) {
                    continue;
                }
                int min = Math.max(0, p.getInt("min-radius", 500));
                int max = p.getInt("max-radius", 4000);
                if (max <= min) {
                    log.warning("rtp.profiles." + id + " max-radius ต้องมากกว่า min-radius — ข้ามโปรไฟล์นี้");
                    continue;
                }
                String world = p.getString("world", "");
                WorldSpec spec = worlds.get(world);
                if (spec != null && spec.borderRadius() > 0) {
                    int centerOffset = (int) Math.ceil(Math.hypot(
                            p.getInt("center-x", 0) - spec.borderCenterX(), p.getInt("center-z", 0) - spec.borderCenterZ()));
                    if (max + centerOffset + 2 >= spec.borderRadius()) {
                        log.warning("rtp.profiles." + id + " max-radius ชน world border ของ " + world
                                + " — จุดใกล้ขอบจะถูกตัดทิ้งตอนสุ่ม");
                    }
                }
                profiles.put(id.toLowerCase(Locale.ROOT), new RtpProfile(id.toLowerCase(Locale.ROOT), world,
                        p.getString("display", id), p.getInt("center-x", 0), p.getInt("center-z", 0), min, max));
            }
        }

        return new Settings(
                c.getString("database.file", "fantasycore.db"),
                Math.max(1, c.getLong("economy.max-transaction", 1_000_000_000L)),
                Collections.unmodifiableList(quick),
                defaultRule,
                Collections.unmodifiableMap(deathRules),
                Collections.unmodifiableMap(worlds),
                c.getString("spawn.world", ""),
                clamp(c.getInt("travel.warmup-seconds", 3), 0, 30),
                clamp(c.getInt("travel.combat-lock-seconds", 15), 0, 300),
                safeMin,
                safeMax,
                List.copyOf(c.getStringList("homes.allowed-worlds")),
                c.getBoolean("homes.require-claim", true),
                Math.max(0, c.getInt("homes.default-limit", 1)),
                Collections.unmodifiableMap(limitPerms),
                Math.max(1, c.getInt("homes.max-limit", 5)),
                c.getString("claims.provider", "auto").toLowerCase(Locale.ROOT),
                List.copyOf(c.getStringList("claims.player-region-prefixes")),
                clamp(c.getInt("claims.rtp-claim-buffer", 8), 0, 128),
                clamp(c.getInt("rtp.cooldown-seconds", 120), 0, 86_400),
                clamp(c.getInt("rtp.failure-retry-seconds", 15), 0, 3_600),
                clamp(c.getInt("rtp.max-candidates", 24), 1, 64),
                clamp(c.getInt("rtp.timeout-seconds", 10), 2, 60),
                clamp(c.getInt("rtp.max-concurrent-chunk-jobs-per-world", 2), 1, 8),
                Collections.unmodifiableMap(profiles),
                Math.max(1.0, c.getDouble("stations.remote-radius", 6.0)));
    }

    private static DeathRule deathRule(ConfigurationSection section, DeathRule fallback, Logger log, String name) {
        if (section == null) {
            return fallback;
        }
        int percent = section.getInt("gold-loss-percent", fallback.goldLossPercent());
        if (percent < 0 || percent > 100) {
            log.warning("death." + name + ".gold-loss-percent ต้องอยู่ระหว่าง 0–100 — ใช้ " + fallback.goldLossPercent());
            percent = fallback.goldLossPercent();
        }
        return new DeathRule(percent, section.getBoolean("keep-inventory", fallback.keepInventory()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
