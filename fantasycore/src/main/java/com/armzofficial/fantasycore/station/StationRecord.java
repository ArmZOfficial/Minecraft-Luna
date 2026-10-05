package com.armzofficial.fantasycore.station;

import java.util.UUID;

/**
 * จุดบริการ: NPC ของ Core (kind=NPC), NPC ของ Citizens ที่ผูกตรง (kind=CITIZENS, entityId = UUID ของ NPC ใน Citizens)
 * หรือ anchor พิกัด (kind=ANCHOR) สำหรับทางเข้าแบบคำสั่ง
 */
public record StationRecord(UUID id, String actionId, Kind kind, UUID worldId, String worldName,
                            double x, double y, double z, float yaw, UUID entityId, String label,
                            String createdBy, long createdAt) {

    public enum Kind {
        NPC,
        CITIZENS,
        ANCHOR
    }
}
