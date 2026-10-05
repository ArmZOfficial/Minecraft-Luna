package com.armzofficial.fantasycore.travel;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.util.Tasks;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * วาร์ปแบบอุ่นเครื่อง: นับถอยหลัง → ยกเลิกเมื่อขยับ/โดนดาเมจ/ตาย/ออก/ย้ายโลก → ตรวจปลายทางซ้ำ → teleportAsync
 * ผลลัพธ์ "สำเร็จ" ส่งเมื่อ teleport สำเร็จจริงเท่านั้น (event ที่ปลั๊กอินอื่น cancel จะไม่นับ)
 */
public final class TeleportService implements Listener {

    /** ปลายทางที่คำนวณ/ตรวจซ้ำตอนจบการอุ่นเครื่อง (บน main thread) */
    public record Resolution(Location location, Component failure) {
        public static Resolution ok(Location location) {
            return new Resolution(location, null);
        }

        public static Resolution fail(Component failure) {
            return new Resolution(null, failure);
        }
    }

    private record Pending(BukkitTask task, int blockX, int blockY, int blockZ, UUID worldId) {
    }

    private final Plugin plugin;
    private final Messages messages;
    private final CombatTracker combat;
    private final Tasks tasks;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public TeleportService(Plugin plugin, Messages messages, CombatTracker combat, Tasks tasks) {
        this.plugin = plugin;
        this.messages = messages;
        this.combat = combat;
        this.tasks = tasks;
    }

    public boolean isPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    /** คืน false ถ้าเริ่มไม่ได้ (กำลังต่อสู้/มีวาร์ปค้าง) — ข้อความแจ้งผู้เล่นแล้ว */
    public boolean canStart(Player player) {
        long combatLeft = combat.remainingSeconds(player);
        if (combatLeft > 0) {
            messages.send(player, "travel.combat", Messages.p("seconds", combatLeft));
            return false;
        }
        if (isPending(player)) {
            messages.send(player, "travel.already-pending");
            return false;
        }
        return true;
    }

    /**
     * @param resolver  เรียกตอนครบเวลา เพื่อตรวจปลายทางล่าสุด
     * @param onArrived เรียกหลัง teleport สำเร็จจริง (main thread)
     */
    public void begin(Player player, int seconds, Component label, Function<Player, Resolution> resolver,
                      Consumer<Location> onArrived) {
        if (!canStart(player)) {
            return;
        }
        UUID id = player.getUniqueId();
        Location start = player.getLocation();
        if (seconds <= 0) {
            finish(player, resolver, onArrived);
            return;
        }
        messages.send(player, "travel.warmup-start", Messages.c("destination", label), Messages.p("seconds", seconds));
        final int[] remaining = {seconds};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null || !pending.containsKey(id)) {
                cancelTask(id);
                return;
            }
            if (remaining[0] <= 0) {
                cancelTask(id);
                finish(online, resolver, onArrived);
                return;
            }
            online.sendActionBar(messages.plain("travel.countdown", Messages.p("seconds", remaining[0])));
            remaining[0]--;
        }, 0L, 20L);
        pending.put(id, new Pending(task, start.getBlockX(), start.getBlockY(), start.getBlockZ(), start.getWorld().getUID()));
    }

    public void cancel(Player player, String reasonKey) {
        if (cancelTask(player.getUniqueId()) && reasonKey != null) {
            messages.send(player, reasonKey);
        }
    }

    public void cancelAll() {
        for (UUID id : pending.keySet()) {
            cancelTask(id);
        }
    }

    private boolean cancelTask(UUID id) {
        Pending removed = pending.remove(id);
        if (removed != null) {
            removed.task().cancel();
            return true;
        }
        return false;
    }

    private void finish(Player player, Function<Player, Resolution> resolver, Consumer<Location> onArrived) {
        long combatLeft = combat.remainingSeconds(player);
        if (combatLeft > 0) {
            messages.send(player, "travel.combat", Messages.p("seconds", combatLeft));
            return;
        }
        Resolution resolution;
        try {
            resolution = resolver.apply(player);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("ตรวจปลายทางวาร์ปล้มเหลว: " + e);
            messages.send(player, "travel.failed");
            return;
        }
        if (resolution.failure() != null) {
            player.sendMessage(resolution.failure());
            return;
        }
        player.teleportAsync(resolution.location(), PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((ok, error) ->
                tasks.sync(() -> {
                    if (error == null && Boolean.TRUE.equals(ok)) {
                        onArrived.accept(resolution.location());
                    } else {
                        messages.send(player, "travel.failed");
                    }
                }));
    }

    // ------------------------------------------------------------ cancel triggers

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Pending p = pending.get(event.getPlayer().getUniqueId());
        if (p == null) {
            return;
        }
        Location to = event.getTo();
        if (to.getBlockX() != p.blockX() || to.getBlockY() != p.blockY() || to.getBlockZ() != p.blockZ()
                || !to.getWorld().getUID().equals(p.worldId())) {
            cancel(event.getPlayer(), "travel.cancel-moved");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isPending(player)) {
            cancel(player, "travel.cancel-damaged");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancel(event.getEntity(), null);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancel(event.getPlayer(), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelTask(event.getPlayer().getUniqueId());
    }
}
