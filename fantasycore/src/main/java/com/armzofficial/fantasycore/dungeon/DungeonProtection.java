package com.armzofficial.fantasycore.dungeon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.block.BlockState;

/** Protection applies only to the fixed owned training world; no staff edit bypass during runs. */
public final class DungeonProtection implements Listener {
    private final DungeonService dungeon;
    public DungeonProtection(DungeonService dungeon) { this.dungeon=dungeon; }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void breakBlock(BlockBreakEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void place(BlockPlaceEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void fill(PlayerBucketFillEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void empty(PlayerBucketEmptyEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interact(PlayerInteractEvent e) {
        if(dungeon.owns(e.getPlayer().getWorld()) && e.getClickedBlock()!=null) { e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void entityInteract(PlayerInteractEntityEvent e) { if(dungeon.owns(e.getRightClicked().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void flow(BlockFromToEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void extend(BlockPistonExtendEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void retract(BlockPistonRetractEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void ignite(BlockIgniteEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void burn(BlockBurnEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void spread(BlockSpreadEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void explode(EntityExplodeEvent e) { if(dungeon.owns(e.getEntity().getWorld())) { e.blockList().clear(); e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void blockExplosion(BlockExplodeEvent e) { if(dungeon.owns(e.getBlock().getWorld())) { e.blockList().clear(); e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void change(EntityChangeBlockEvent e) { if(dungeon.owns(e.getEntity().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hanging(HangingBreakEvent e) { if(dungeon.owns(e.getEntity().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void spawn(CreatureSpawnEvent e) { if(dungeon.owns(e.getLocation().getWorld()) && !dungeon.encounter(e.getEntity())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void itemSpawn(ItemSpawnEvent e) { if(dungeon.owns(e.getLocation().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drop(PlayerDropItemEvent e) { if(dungeon.owns(e.getPlayer().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void pickup(EntityPickupItemEvent e) { if(dungeon.owns(e.getEntity().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void vehicle(VehicleEnterEvent e) { if(dungeon.owns(e.getVehicle().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void portal(EntityPortalEvent e) { if(dungeon.owns(e.getEntity().getWorld())) { e.setCancelled(true); } }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void container(InventoryOpenEvent e) {
        InventoryHolder holder=e.getInventory().getHolder(false);
        if(holder instanceof BlockState state && dungeon.owns(state.getWorld())) { e.setCancelled(true); }
    }
}
