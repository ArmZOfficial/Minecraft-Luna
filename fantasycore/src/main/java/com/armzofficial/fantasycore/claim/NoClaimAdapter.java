package com.armzofficial.fantasycore.claim;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class NoClaimAdapter implements ClaimAdapter {

    private final String reason;

    public NoClaimAdapter(String reason) {
        this.reason = reason;
    }

    @Override
    public String name() {
        return "none (" + reason + ")";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<ClaimInfo> playerClaimAt(Location location, Player viewer) {
        return Optional.empty();
    }

    @Override
    public boolean anyRegionIntersects(World world, int minX, int minZ, int maxX, int maxZ) {
        return false;
    }
}
