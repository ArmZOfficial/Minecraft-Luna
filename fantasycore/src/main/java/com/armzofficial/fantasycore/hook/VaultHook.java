package com.armzofficial.fantasycore.hook;

import com.armzofficial.fantasycore.economy.EconomyService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/**
 * แยกคลาสที่อ้าง Vault ไว้ที่นี่ เพื่อให้ FantasyCore โหลดได้แม้ไม่มี Vault
 * เรียกเฉพาะหลังตรวจว่ามี class net.milkbowl.vault.economy.Economy แล้ว
 */
public final class VaultHook {

    private static Economy registered;

    private VaultHook() {
    }

    public static void register(Plugin plugin, EconomyService economy, String currencyName) {
        registered = new VaultEconomyProvider(plugin, economy, currencyName);
        Bukkit.getServicesManager().register(Economy.class, registered, plugin, ServicePriority.Highest);
    }

    public static void unregister() {
        if (registered != null) {
            Bukkit.getServicesManager().unregister(Economy.class, registered);
            registered = null;
        }
    }

    /** คืนชื่อ provider ที่ Vault ใช้จริงตอนนี้ (null ถ้าไม่มี) — ใช้ใน /fa doctor */
    public static String activeProviderName() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider().getName() + " (" + rsp.getPlugin().getName() + ")";
    }

    public static boolean isOurs() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp != null && registered != null && rsp.getProvider() == registered;
    }
}
