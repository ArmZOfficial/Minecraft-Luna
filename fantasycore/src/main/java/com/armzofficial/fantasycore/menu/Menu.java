package com.armzofficial.fantasycore.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * เมนู inventory ของ Core — ระบุตัวตนด้วย holder ไม่ใช่ชื่อหน้าต่าง (ADMIN-PANEL-SPEC: Inventory implementation)
 * ไอเทมในเมนูเป็นภาพแสดงผลเท่านั้น {@link MenuListener} ยกเลิกทุกการย้ายของ
 */
public abstract class Menu implements InventoryHolder {

    @FunctionalInterface
    public interface Action {
        void run(Player player, ClickType click);
    }

    private final Inventory inventory;
    private final Map<Integer, Action> actions = new HashMap<>();
    protected final UUID viewer;
    private boolean busy;

    protected Menu(UUID viewer, int rows, Component title) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** วาดเมนูใหม่ทั้งหน้า (เรียกบน main thread) */
    public abstract void render();

    public void open(Player player) {
        render();
        player.openInventory(inventory);
    }

    protected void clear() {
        inventory.clear();
        actions.clear();
    }

    protected void set(int slot, ItemStack icon) {
        inventory.setItem(slot, icon);
        actions.remove(slot);
    }

    protected void set(int slot, ItemStack icon, Action action) {
        inventory.setItem(slot, icon);
        actions.put(slot, action);
    }

    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /** ระหว่างรองานฐานข้อมูล ปุ่มทั้งหมดไม่ตอบสนอง (กันดับเบิลคลิก) */
    protected void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isBusy() {
        return busy;
    }

    /** Cancel uncommitted UI work when this specific holder closes. */
    public void closed(Player player) { }

    void handleClick(Player player, int slot, ClickType click) {
        if (busy || !player.getUniqueId().equals(viewer)) {
            return;
        }
        Action action = actions.get(slot);
        if (action != null) {
            action.run(player, click);
        }
    }

    /** หน้าต่างนี้ยังเปิดอยู่กับผู้เล่นหรือไม่ — ใช้ก่อนวาดผลที่มาจาก async */
    protected boolean isOpenFor(Player player) {
        return player != null && player.isOnline() && player.getOpenInventory().getTopInventory().getHolder(false) == this;
    }
}
