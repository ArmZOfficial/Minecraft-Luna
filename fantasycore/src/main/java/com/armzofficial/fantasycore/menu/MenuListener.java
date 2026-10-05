package com.armzofficial.fantasycore.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ยกเลิกทุกการคลิก/ลากเมื่อเมนูของ Core เปิดอยู่ (รวม shift-click, ปุ่มตัวเลข, double click, offhand, creative)
 * แล้วส่งเฉพาะคลิกซ้าย/ขวาในช่องของเมนูไปทำงานใน tick ถัดไป
 */
public final class MenuListener implements Listener {

    private static final Set<ClickType> ACCEPTED = EnumSet.of(ClickType.LEFT, ClickType.RIGHT);
    private static final long DEBOUNCE_MILLIS = 200;

    private final Plugin plugin;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != top || !ACCEPTED.contains(event.getClick())) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClick.put(player.getUniqueId(), now);
        if (previous != null && now - previous < DEBOUNCE_MILLIS) {
            return;
        }
        ClickType click = event.getClick();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder(false) == menu) {
                menu.handleClick(player, slot, click);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getOpenInventory().getTopInventory().getHolder(false) instanceof Menu menu) {
            menu.closed(event.getPlayer());
        }
        lastClick.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && event.getInventory().getHolder(false) instanceof Menu menu) {
            menu.closed(player);
        }
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) { closeAdmin(event.getEntity()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) { closeAdmin(event.getPlayer()); }
    private void closeAdmin(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof AdminPanelMenu) { player.closeInventory(); }
    }

    /** ปิดเมนูของ Core ทั้งหมดตอนปิดปลั๊กอิน เพื่อไม่ให้หน้าต่างค้างโดยไม่มี listener */
    public static void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            HumanEntity human = player;
            if (human.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                human.closeInventory();
            }
        }
    }
}
