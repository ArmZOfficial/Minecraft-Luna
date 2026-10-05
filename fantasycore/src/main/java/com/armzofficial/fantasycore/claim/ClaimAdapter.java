package com.armzofficial.fantasycore.claim;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * มุมมองของ Core ต่อระบบที่ดิน — WorldGuard/ProtectionStones เป็นเจ้าของ region จริง
 * Core ใช้อ่านเพื่อตัดสินใจเรื่องบ้านและ RTP เท่านั้น (ยังไม่แก้ region ใน v0.1)
 */
public interface ClaimAdapter {

    /** ชื่อ provider สำหรับ /fa doctor */
    String name();

    /** false = ไม่มี provider → งานที่ต้องพึ่งที่ดินต้อง fail closed */
    boolean available();

    /** แปลงของผู้เล่น (region ตาม prefix ที่ตั้งไว้) ที่ตำแหน่งนี้ มุมมองของ viewer */
    Optional<ClaimInfo> playerClaimAt(Location location, Player viewer);

    /** มี region ใด ๆ (รวม reserve ของแอดมิน) ทับพื้นที่สี่เหลี่ยมนี้ตลอดความสูงโลกหรือไม่ */
    boolean anyRegionIntersects(World world, int minX, int minZ, int maxX, int maxZ);
}
