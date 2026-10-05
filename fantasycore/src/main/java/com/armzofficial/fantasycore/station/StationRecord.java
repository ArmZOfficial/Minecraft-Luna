package com.armzofficial.fantasycore.station;

import java.util.UUID;

/**
 * จุดบริการ: NPC ของ Core (kind=NPC) หรือ anchor สำหรับ NPC ของ Citizens (kind=ANCHOR)
 */
public record StationRecord(UUID id, String actionId, Kind kind, UUID worldId, String worldName,
                            double x, double y, double z, float yaw, UUID entityId, String label,
                            String createdBy, long createdAt) {

    public enum Kind {
        NPC,
        ANCHOR
    }
}
