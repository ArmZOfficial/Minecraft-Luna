package com.armzofficial.fantasycore.travel;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** ล็อกการวาร์ปหลังโดนตี/ตีคนอื่น (ค่าเริ่มต้น 15 วินาที) */
public final class CombatTracker implements Listener {

    private final Map<UUID, Long> taggedUntil = new ConcurrentHashMap<>();
    private final long lockMillis;

    public CombatTracker(int lockSeconds) {
        this.lockMillis = lockSeconds * 1000L;
    }

    public long remainingSeconds(Player player) {
        Long until = taggedUntil.get(player.getUniqueId());
        if (until == null) {
            return 0;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            taggedUntil.remove(player.getUniqueId());
            return 0;
        }
        return (remaining + 999) / 1000;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (lockMillis <= 0) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            tag(victim);
        }
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            tag(shooter);
        } else if (damager instanceof Player attacker) {
            tag(attacker);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        taggedUntil.remove(event.getPlayer().getUniqueId());
    }

    private void tag(Player player) {
        taggedUntil.put(player.getUniqueId(), System.currentTimeMillis() + lockMillis);
    }
}
