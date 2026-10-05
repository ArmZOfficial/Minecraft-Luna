package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Private, single-response forms for legacy clients; callbacks always return to the server thread. */
public final class AdminChatInput implements Listener {
    static final class Pending {
        final UUID world;
        final long expiresAt;
        final String permission;
        final Consumer<String> ready;
        private final AtomicBoolean claimed = new AtomicBoolean();

        Pending(UUID world, long expiresAt, String permission, Consumer<String> ready) {
            this.world = world; this.expiresAt = expiresAt; this.permission = permission; this.ready = ready;
        }
        boolean claim(long now) { return now < expiresAt && claimed.compareAndSet(false, true); }
    }

    private final Services services;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    AdminChatInput(Services services) { this.services = services; }

    public void ask(Player player, String promptKey, String permission, Consumer<String> ready) {
        cancel(player);
        if (!player.hasPermission("fantasyadmin.view") || !player.hasPermission(permission) || player.isDead()) {
            services.messages().send(player, "common.no-permission"); return;
        }
        // Close before registering: the previous confirmation menu invalidates its own nonce on close.
        player.closeInventory();
        var session = new Pending(player.getWorld().getUID(), System.currentTimeMillis() + 60_000, permission, ready);
        pending.put(player.getUniqueId(), session);
        services.messages().send(player, promptKey);
        services.messages().send(player, "admin.panel.input.hint");
        services.tasks().later(1200, () -> {
            if (pending.remove(player.getUniqueId(), session) && player.isOnline()) {
                services.messages().send(player, "admin.panel.input.timeout");
            }
        });
    }

    public void cancel(Player player) { pending.remove(player.getUniqueId()); }
    public void close() { pending.clear(); }

    static String text(String raw) {
        if (raw == null || raw.length() > 200 || raw.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("invalid form text");
        }
        String value = raw.strip();
        if (value.isEmpty()) { throw new IllegalArgumentException("empty form text"); }
        return value;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID actor = player.getUniqueId();
        Pending session = pending.get(actor);
        if (session == null) { return; }
        event.setCancelled(true);
        event.viewers().clear();
        // Keep the entry while processing so rapid follow-up messages are suppressed too.
        long now = System.currentTimeMillis();
        if (now >= session.expiresAt) {
            services.tasks().sync(() -> {
                if (pending.remove(actor, session) && player.isOnline()) { services.messages().send(player, "admin.panel.input.timeout"); }
            });
            return;
        }
        if (!session.claim(now)) { return; }
        String raw = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        services.tasks().sync(() -> {
            if (!pending.remove(actor, session) || !player.isOnline()) { return; }
            if (System.currentTimeMillis() >= session.expiresAt || player.isDead()
                    || !player.getWorld().getUID().equals(session.world)
                    || !player.hasPermission("fantasyadmin.view") || !player.hasPermission(session.permission)) {
                services.messages().send(player, "admin.panel.input.cancelled"); return;
            }
            String value;
            try { value = text(raw); }
            catch (IllegalArgumentException e) { services.messages().send(player, "admin.panel.input.invalid"); return; }
            if (value.equalsIgnoreCase("cancel") || value.equals("ยกเลิก")) {
                services.messages().send(player, "admin.panel.input.cancelled"); return;
            }
            session.ready.accept(value);
        });
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { cancel(event.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { cancel(event.getEntity()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) { cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) { cancel(player); }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (pending.remove(event.getPlayer().getUniqueId()) == null) { return; }
        if (event.getMessage().equalsIgnoreCase("/cancel")) { event.setCancelled(true); }
        services.messages().send(event.getPlayer(), "admin.panel.input.cancelled");
    }
}
