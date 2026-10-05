package com.armzofficial.fantasycore.claim;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * อ่าน region ของ WorldGuard — ครอบคลุม ProtectionStones ด้วย เพราะ PS สร้าง region ชื่อ "ps{x}x{y}y{z}z" ใน WorldGuard
 * โหลดคลาสนี้เฉพาะเมื่อมี WorldGuard แล้วเท่านั้น
 */
public final class WorldGuardClaimAdapter implements ClaimAdapter {

    private final List<String> playerPrefixes;

    public WorldGuardClaimAdapter(List<String> playerPrefixes) {
        this.playerPrefixes = playerPrefixes.stream().map(p -> p.toLowerCase(Locale.ROOT)).toList();
    }

    @Override
    public String name() {
        return "WorldGuard (prefix " + playerPrefixes + ")";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Optional<ClaimInfo> playerClaimAt(Location location, Player viewer) {
        RegionManager manager = manager(location.getWorld());
        if (manager == null) {
            return Optional.empty();
        }
        ApplicableRegionSet set = manager.getApplicableRegions(
                BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        ProtectedRegion best = null;
        for (ProtectedRegion region : set) {
            if (!isPlayerRegion(region.getId())) {
                continue;
            }
            if (best == null || region.getPriority() > best.getPriority()) {
                best = region;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        UUID id = viewer.getUniqueId();
        BlockVector3 min = best.getMinimumPoint();
        BlockVector3 max = best.getMaximumPoint();
        return Optional.of(new ClaimInfo(best.getId(), best.getOwners().contains(id), best.getMembers().contains(id),
                max.x() - min.x() + 1, max.z() - min.z() + 1, min.y(), max.y(), best.getPriority()));
    }

    @Override
    public boolean anyRegionIntersects(World world, int minX, int minZ, int maxX, int maxZ) {
        RegionManager manager = manager(world);
        if (manager == null) {
            return false;
        }
        ProtectedCuboidRegion probe = new ProtectedCuboidRegion("__fantasycore_probe", true,
                BlockVector3.at(minX, world.getMinHeight(), minZ),
                BlockVector3.at(maxX, world.getMaxHeight() - 1, maxZ));
        for (ProtectedRegion region : manager.getApplicableRegions(probe)) {
            if (!ProtectedRegion.GLOBAL_REGION.equalsIgnoreCase(region.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerRegion(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        for (String prefix : playerPrefixes) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static RegionManager manager(World world) {
        if (world == null) {
            return null;
        }
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }
}
