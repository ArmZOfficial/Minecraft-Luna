package com.armzofficial.fantasycore;

import com.armzofficial.fantasycore.config.Messages;
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
        // แจ้งของค้างในกล่องและรางวัลที่รับได้วันนี้ (ไม่แจกอัตโนมัติ — ผู้เล่นกดรับเอง)
        services.tasks().then(services.mail().countPending(id), (count, error) -> {
            if (error == null && count != null && count > 0 && player.isOnline()) {
                services.messages().send(player, "mail.join-notice", Messages.p("count", count));
            }
        });
        if (services.rewards().enabled() && player.hasPermission("fantasy.rewards")) {
            services.tasks().then(services.rewards().status(id), (status, error) -> {
                if (error == null && status != null && !status.claimedThisPeriod() && player.isOnline()) {
                    services.messages().send(player, "reward.join-notice");
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        services.economy().forget(id);
        services.rtp().forget(id);
        services.homes().forget(id);
    }
}
