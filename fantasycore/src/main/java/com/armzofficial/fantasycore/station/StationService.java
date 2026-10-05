package com.armzofficial.fantasycore.station;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.storage.Database;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * จุดบริการ:
 * - NPC ของ Core = Villager ที่ไม่มี AI/อมตะ/เงียบ ติด PDC action ID — คลิกแล้วเปิดบริการ
 * - ANCHOR = พิกัดที่ผูก action (ใช้คู่ NPC ของ Citizens ที่เรียก /fc action &lt;id&gt;)
 */
public final class StationService {

    private final Plugin plugin;
    private final Database database;
    private final StationStore store;
    private final double radius;
    private final NamespacedKey stationKey;
    private final NamespacedKey actionKey;
    private final List<StationRecord> records = new CopyOnWriteArrayList<>();

    public StationService(Plugin plugin, Database database, StationStore store, double radius) {
        this.plugin = plugin;
        this.database = database;
        this.store = store;
        this.radius = radius;
        this.stationKey = new NamespacedKey(plugin, "station_id");
        this.actionKey = new NamespacedKey(plugin, "station_action");
    }

    public CompletableFuture<Integer> load() {
        return database.async(() -> {
            List<StationRecord> all = store.all();
            records.clear();
            records.addAll(all);
            return all.size();
        });
    }

    public List<StationRecord> records() {
        return List.copyOf(records);
    }

    public double radius() {
        return radius;
    }

    /** ผู้เล่นอยู่ในระยะของจุดบริการ action นี้ในโลกเดียวกันหรือไม่ */
    public boolean isNear(Player player, String actionId) {
        Location location = player.getLocation();
        UUID world = location.getWorld().getUID();
        double max = radius * radius;
        for (StationRecord record : records) {
            if (!record.actionId().equals(actionId) || !record.worldId().equals(world)) {
                continue;
            }
            double dx = record.x() - location.getX();
            double dy = record.y() - location.getY();
            double dz = record.z() - location.getZ();
            if (dx * dx + dy * dy + dz * dz <= max) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> actionOf(Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING));
    }

    public Optional<UUID> stationIdOf(Entity entity) {
        String value = entity.getPersistentDataContainer().get(stationKey, PersistentDataType.STRING);
        return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
    }

    /** สร้าง NPC ที่ตำแหน่ง/ทิศของแอดมิน (main thread) แล้วบันทึกลงฐานข้อมูล */
    public CompletableFuture<StationRecord> spawnNpc(Player admin, String actionId, Component label, String labelText) {
        Location location = admin.getLocation().clone();
        location.setPitch(0);
        World world = location.getWorld();
        UUID stationId = UUID.randomUUID();
        Villager villager = world.spawn(location, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setCollidable(false);
            v.setCanPickupItems(false);
            v.customName(label);
            v.setCustomNameVisible(true);
            v.setProfession(professionFor(actionId));
            PersistentDataContainer pdc = v.getPersistentDataContainer();
            pdc.set(stationKey, PersistentDataType.STRING, stationId.toString());
            pdc.set(actionKey, PersistentDataType.STRING, actionId);
        });
        if (!villager.isValid()) {
            // ถูกปลั๊กอินอื่นยกเลิกการเกิด (เช่น WorldGuard mob-spawning deny + block-plugin-spawning)
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "spawn ถูกยกเลิก — ตรวจ flag mob-spawning/deny-spawn ของ WorldGuard ที่ตำแหน่งนี้"));
        }
        StationRecord record = new StationRecord(stationId, actionId, StationRecord.Kind.NPC, world.getUID(), world.getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), villager.getUniqueId(), labelText,
                admin.getUniqueId().toString(), System.currentTimeMillis());
        AuditEntry audit = new AuditEntry(admin.getUniqueId().toString(), admin.getName(), "station.spawn", actionId,
                "NPC " + stationId + " @ " + world.getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(),
                null);
        return database.async(() -> {
            store.insert(record, audit);
            records.add(record);
            return record;
        }).whenComplete((ok, error) -> {
            if (error != null) {
                // บันทึกไม่ได้ → ลบ NPC ทิ้ง ไม่ให้มีจุดบริการที่ระบบไม่รู้จัก
                plugin.getServer().getScheduler().runTask(plugin, villager::remove);
            }
        });
    }

    public CompletableFuture<StationRecord> addAnchor(Player admin, String actionId) {
        Location location = admin.getLocation();
        World world = location.getWorld();
        StationRecord record = new StationRecord(UUID.randomUUID(), actionId, StationRecord.Kind.ANCHOR, world.getUID(),
                world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), null, null,
                admin.getUniqueId().toString(), System.currentTimeMillis());
        AuditEntry audit = new AuditEntry(admin.getUniqueId().toString(), admin.getName(), "station.anchor", actionId,
                "ANCHOR " + record.id() + " @ " + world.getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(),
                null);
        return database.async(() -> {
            store.insert(record, audit);
            records.add(record);
            return record;
        });
    }

    /** ลบ NPC ที่แอดมินมองอยู่ */
    public CompletableFuture<Boolean> removeNpc(Player admin, Entity entity) {
        Optional<UUID> stationId = stationIdOf(entity);
        if (stationId.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        entity.remove();
        return delete(admin, stationId.get(), "station.remove");
    }

    /** ลบ anchor ที่ใกล้ที่สุดในระยะ 3 บล็อก */
    public CompletableFuture<Boolean> removeNearestAnchor(Player admin) {
        Location location = admin.getLocation();
        StationRecord nearest = null;
        double best = 9.0;
        for (StationRecord record : records) {
            if (record.kind() != StationRecord.Kind.ANCHOR || !record.worldId().equals(location.getWorld().getUID())) {
                continue;
            }
            double dx = record.x() - location.getX();
            double dy = record.y() - location.getY();
            double dz = record.z() - location.getZ();
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= best) {
                best = d;
                nearest = record;
            }
        }
        if (nearest == null) {
            return CompletableFuture.completedFuture(false);
        }
        return delete(admin, nearest.id(), "station.unanchor");
    }

    private CompletableFuture<Boolean> delete(Player admin, UUID stationId, String action) {
        AuditEntry audit = new AuditEntry(admin.getUniqueId().toString(), admin.getName(), action, stationId.toString(), null, null);
        return database.async(() -> {
            boolean deleted = store.delete(stationId, audit, System.currentTimeMillis());
            records.removeIf(r -> r.id().equals(stationId));
            return deleted;
        });
    }

    private static Villager.Profession professionFor(String actionId) {
        if (actionId.startsWith("bank.")) {
            return Villager.Profession.LIBRARIAN;
        }
        if (actionId.startsWith("travel.") || actionId.startsWith("navigation.")) {
            return Villager.Profession.CARTOGRAPHER;
        }
        if (actionId.startsWith("home.") || actionId.startsWith("land.")) {
            return Villager.Profession.MASON;
        }
        if (actionId.startsWith("equipment.") || actionId.startsWith("repair.") || actionId.startsWith("craft.")) {
            return Villager.Profession.TOOLSMITH;
        }
        return Villager.Profession.CLERIC;
    }
}
