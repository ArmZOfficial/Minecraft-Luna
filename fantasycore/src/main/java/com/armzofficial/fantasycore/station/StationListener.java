package com.armzofficial.fantasycore.station;

import com.armzofficial.fantasycore.hook.CitizensBridge;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/**
 * คลิก NPC สถานี → เปิดบริการ; กันการเทรด/ทำร้าย/แปลงร่างของ NPC ของ Core
 * NPC ของ Citizens ที่ผูกด้วย /fa npc bind ก็เปิดบริการผ่าน action เดียวกัน (ตัว Citizens ดูแลหน้าตา/ท่าทาง)
 */
public final class StationListener implements Listener {

    private final StationService stations;
    private final ActionRegistry actions;
    private final CitizensBridge citizens;

    public StationListener(StationService stations, ActionRegistry actions, CitizensBridge citizens) {
        this.stations = stations;
        this.actions = actions;
        this.citizens = citizens;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        Optional<String> action = stations.actionOf(event.getRightClicked());
        if (action.isEmpty() && citizens != null) {
            action = citizens.npcOf(event.getRightClicked()).flatMap(npc -> stations.actionOfCitizens(npc.uuid()));
        }
        if (action.isEmpty()) {
            return;
        }
        // ยกเลิกทุกมือ (กันหน้าต่างเทรด/ป้ายชื่อ/เชือก) แต่เปิดบริการครั้งเดียวจากมือหลัก
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        actions.open(event.getPlayer(), action.get(), ActionRegistry.Source.STATION);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isStation(event.getEntity()) && event.getCause() != EntityDamageEvent.DamageCause.KILL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (isStation(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean isStation(Entity entity) {
        return stations.actionOf(entity).isPresent();
    }
}
