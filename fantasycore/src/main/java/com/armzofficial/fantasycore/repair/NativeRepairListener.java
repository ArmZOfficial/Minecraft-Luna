package com.armzofficial.fantasycore.repair;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** กัน native result ที่ซ่อมโดยไม่ผ่าน Core; Core markers ห้ามใช้ใน anvil/grindstone/crafting จนมี adapter */
public final class NativeRepairListener implements Listener {
    private final ItemTemplateService items;
    private final RepairService repair;
    private final Messages messages;

    public NativeRepairListener(ItemTemplateService items, RepairService repair, Messages messages) {
        this.items = items;
        this.repair = repair;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void anvil(PrepareAnvilEvent event) {
        if (blocked(inputs(event.getInventory()), event.getResult())) { event.setResult(null); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void grindstone(PrepareGrindstoneEvent event) {
        if (blocked(inputs(event.getInventory()), event.getResult())) { event.setResult(null); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void crafting(PrepareItemCraftEvent event) {
        if (blocked(event.getInventory().getMatrix(), event.getInventory().getResult())) { event.getInventory().setResult(null); }
    }

    /** ตรวจซ้ำตอนหยิบ result เผื่อปลั๊กอินอื่นเปลี่ยนหลัง Prepare event */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void takeResult(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        boolean deny;
        if (event instanceof CraftItemEvent craft) {
            deny = blocked(craft.getInventory().getMatrix(), craft.getCurrentItem());
        } else if ((top.getType() == InventoryType.ANVIL || top.getType() == InventoryType.GRINDSTONE) && event.getRawSlot() == 2) {
            deny = blocked(inputs(top), event.getCurrentItem());
        } else {
            return;
        }
        if (deny) {
            event.setCancelled(true);
            messages.send(event.getWhoClicked(), "repair.native-blocked");
        }
    }

    private boolean blocked(ItemStack[] inputs, ItemStack result) {
        if (result == null || result.isEmpty()) { return false; }
        for (ItemStack source : inputs) {
            if (source == null || source.isEmpty()) { continue; }
            if (items.hasIdentityFields(source)) { return true; }
            if (repair.nativePolicyEnabled() && source.getType() == result.getType()) {
                int before = source.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
                int after = result.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
                if (after < before) { return true; }
            }
        }
        return false;
    }

    private static ItemStack[] inputs(Inventory inventory) {
        return new ItemStack[]{inventory.getItem(0), inventory.getItem(1)};
    }
}
