package com.armzofficial.fantasycore.home;

import java.util.UUID;

public record HomeRecord(UUID owner, String key, String display, UUID worldId, String worldName,
                         double x, double y, double z, float yaw, float pitch, String regionId,
                         long createdAt, long updatedAt) {

    public String coords() {
        return (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z);
    }
}
