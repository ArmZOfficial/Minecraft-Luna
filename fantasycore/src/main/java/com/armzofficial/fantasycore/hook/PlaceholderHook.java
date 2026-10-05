package com.armzofficial.fantasycore.hook;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.util.Money;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * %fantasycore_gold% %fantasycore_bank% %fantasycore_red% (+ _raw) และ %fantasycore_home_limit%
 * อ่านจาก cache ที่อัปเดตทุก commit — ไม่ query ฐานข้อมูลทุกครั้งที่ TAB/scoreboard รีเฟรช
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final Services services;

    private PlaceholderHook(Services services) {
        this.services = services;
    }

    public static boolean register(Services services) {
        return new PlaceholderHook(services).register();
    }

    @Override
    public String getIdentifier() {
        return "fantasycore";
    }

    @Override
    public String getAuthor() {
        return "ArmZOfficial";
    }

    @Override
    public String getVersion() {
        return services.plugin().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        if (params.equals("home_limit")) {
            return player instanceof Player online ? String.valueOf(services.homes().limit(online)) : "";
        }
        Balances balances = services.economy().cached(player.getUniqueId()).orElse(null);
        if (balances == null) {
            return "…";
        }
        return switch (params) {
            case "gold" -> Money.format(balances.gold());
            case "gold_raw" -> String.valueOf(balances.gold());
            case "bank" -> Money.format(balances.bank());
            case "bank_raw" -> String.valueOf(balances.bank());
            case "red" -> Money.format(balances.red());
            case "red_raw" -> String.valueOf(balances.red());
            default -> null;
        };
    }
}
