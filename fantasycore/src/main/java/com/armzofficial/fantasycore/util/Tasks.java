package com.armzofficial.fantasycore.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/** ตัวช่วยย้ายผลจาก thread ฐานข้อมูลกลับ main thread อย่างปลอดภัย */
public final class Tasks {

    private final Plugin plugin;

    public Tasks(Plugin plugin) {
        this.plugin = plugin;
    }

    public void sync(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void later(long ticks, Runnable runnable) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, Math.max(1, ticks));
        }
    }

    /**
     * รอผล future แล้วเรียก callback บน main thread; error ถูกแกะจาก CompletionException และ log ไว้
     */
    public <T> void then(CompletableFuture<T> future, BiConsumer<T, Throwable> callback) {
        future.whenComplete((value, error) -> {
            Throwable cause = unwrap(error);
            if (cause != null) {
                plugin.getLogger().log(Level.SEVERE, "งานฐานข้อมูลล้มเหลว", cause);
            }
            sync(() -> callback.accept(value, cause));
        });
    }

    public static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
