package com.armzofficial.fantasycore.item;

import com.armzofficial.fantasycore.config.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;

/** Vanilla brewing must not mutate the effect on a versioned Core potion. */
public final class NativePotionListener implements Listener {
    private final ItemTemplateService items;
    private final Messages messages;

    public NativePotionListener(ItemTemplateService items, Messages messages) {
        this.items = items;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        for (ItemStack item : event.getContents().getContents()) {
            if (item == null || (item.getType() != Material.POTION && item.getType() != Material.SPLASH_POTION
                    && item.getType() != Material.LINGERING_POTION) || !items.hasIdentityFields(item)) { continue; }
            event.setCancelled(true);
            for (var viewer : event.getContents().getViewers()) {
                if (viewer instanceof Player player) { messages.send(player, "alchemy.brew-blocked"); }
            }
            return;
        }
    }
}
