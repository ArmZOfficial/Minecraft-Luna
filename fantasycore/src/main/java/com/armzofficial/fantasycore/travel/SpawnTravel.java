package com.armzofficial.fantasycore.travel;

import com.armzofficial.fantasycore.Services;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** /spawn — จุดกลับ hub ที่ต้องมีจากทุกโลกปลายทาง (INTERIOR-AND-MAP-PLAN: "จุดกลับ hub ต้องมีในทุกโลกปลายทาง") */
public final class SpawnTravel {

    private SpawnTravel() {
    }

    public static World spawnWorld(Services services) {
        String name = services.settings().spawnWorld();
        World world = name == null || name.isBlank() ? null : Bukkit.getWorld(name);
        return world != null ? world : Bukkit.getWorlds().getFirst();
    }

    public static void teleport(Services services, Player player) {
        services.teleports().begin(player, services.settings().warmupSeconds(), services.messages().plain("spawn.label"),
                p -> {
                    World world = spawnWorld(services);
                    Location spawn = world.getSpawnLocation().clone().add(0.5, 0, 0.5);
                    spawn.setYaw(world.getSpawnLocation().getYaw());
                    return TeleportService.Resolution.ok(spawn);
                },
                arrived -> services.messages().send(player, "spawn.arrived"));
    }
}
