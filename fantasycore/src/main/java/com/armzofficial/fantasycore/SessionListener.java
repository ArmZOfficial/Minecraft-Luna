package com.armzofficial.fantasycore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** บันทึก UUID ↔ ชื่อ และโหลด/ล้าง cache ยอดเงินตอนเข้า/ออก */
public final class SessionListener implements Listener {

    private final Services services;

    public SessionListener(Services services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        touch(event.getPlayer());
    }

    public void touch(Player player) {
        UUID id = player.getUniqueId();
        String name = player.getName();
        long now = System.currentTimeMillis();
        services.tasks().then(services.database().async(() -> {
            services.players().touch(id, name, now);
            return null;
        }), (ignored, error) -> {
        });
        services.tasks().then(services.economy().load(id), (ignored, error) -> {
        });
        services.tasks().then(services.homes().list(id), (ignored, error) -> {
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        services.economy().forget(id);
        services.rtp().forget(id);
        services.homes().forget(id);
    }
}
