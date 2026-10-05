package com.armzofficial.fantasycore.util;

import org.bukkit.Bukkit;

/** การอ่าน flag ที่ Paper 26.2 ยังไม่มีใน ServerConfiguration API */
public final class PlayerDataSaving {
    private PlayerDataSaving() {
    }

    // Paper 26.2 ยังรองรับ bridge นี้แต่ mark removal. แยกไว้จุดเดียวจนมี typed replacement;
    // ถ้า runtime ใหม่ไม่มี API/อ่านไม่ได้ให้ fail closed ไม่เดาว่า saveData ใช้ได้
    @SuppressWarnings("removal")
    public static boolean enabled() {
        try {
            return !Bukkit.spigot().getConfig().getBoolean("players.disable-saving", false);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
