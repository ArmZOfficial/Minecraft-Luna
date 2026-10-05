package com.armzofficial.fantasycore.travel;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * ตัวตรวจจุดลงจอดเดียวกันสำหรับ RTP, home และ spawn (CASUAL-SURVIVAL §3, §9)
 * เรียกบน main thread เท่านั้น และ chunk ต้องโหลดแล้ว
 */
public final class LandingValidator {

    private static final Set<Material> DANGEROUS = EnumSet.of(
            Material.LAVA, Material.WATER, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE, Material.POWDER_SNOW, Material.POINTED_DRIPSTONE, Material.COBWEB,
            Material.BUBBLE_COLUMN, Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY,
            Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.TNT);

    /** ช่องว่างเหนือพื้น: เท้า + หัว + เผื่อ 1 = 3 บล็อก */
    private static final int HEADROOM = 3;

    private final int safeMinY;
    private final int safeMaxY;

    public LandingValidator(int safeMinY, int safeMaxY) {
        this.safeMinY = safeMinY;
        this.safeMaxY = safeMaxY;
    }

    public enum Problem {
        WORLD_MISSING, OUTSIDE_BORDER, Y_OUT_OF_RANGE, NO_FLOOR, DANGEROUS_FLOOR, BLOCKED, DANGEROUS_SPACE
    }

    /**
     * ตรวจว่ายืนที่จุดนี้ได้ (ใช้กับ home/spawn) — รองรับพื้นบล็อกไม่เต็ม เช่น พรม ขั้นบันได แผ่นหิน:
     * ช่องเท้า/หัวต้องไม่ใช่บล็อกทึบเต็มก้อน ไม่มีของเหลว/ของอันตราย และต้องมีพื้นรองรับไม่ตกลงไป
     */
    public Optional<Problem> checkStanding(Location feet) {
        World world = feet.getWorld();
        if (world == null) {
            return Optional.of(Problem.WORLD_MISSING);
        }
        int x = feet.getBlockX();
        int y = feet.getBlockY();
        int z = feet.getBlockZ();
        if (y - 1 < world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return Optional.of(Problem.Y_OUT_OF_RANGE);
        }
        if (!world.getWorldBorder().isInside(feet)) {
            return Optional.of(Problem.OUTSIDE_BORDER);
        }
        Block feetBlock = world.getBlockAt(x, y, z);
        Block headBlock = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);
        for (Block block : new Block[]{feetBlock, headBlock}) {
            if (DANGEROUS.contains(block.getType()) || block.isLiquid()) {
                return Optional.of(Problem.DANGEROUS_SPACE);
            }
            if (block.getType().isOccluding()) {
                return Optional.of(Problem.BLOCKED);
            }
        }
        if (DANGEROUS.contains(below.getType())) {
            return Optional.of(Problem.DANGEROUS_FLOOR);
        }
        // พื้นรองรับ: บล็อกใต้เท้าแข็ง หรือช่องเท้าเองเป็นบล็อกครึ่ง/พรมที่ยืนบนได้
        boolean supported = below.getType().isSolid() || !feetBlock.isPassable();
        return supported ? Optional.empty() : Optional.of(Problem.NO_FLOOR);
    }

    /**
     * หาจุดลงจอด RTP ที่ footprint 2×2 เริ่มที่ (x, z) — คืน Location กลาง footprint
     */
    public Optional<Location> findRtpLanding(World world, int x, int z) {
        int floorY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int feetY = floorY + 1;
        if (feetY < Math.max(safeMinY, world.getMinHeight() + 1) || feetY > Math.min(safeMaxY, world.getMaxHeight() - HEADROOM)) {
            return Optional.empty();
        }
        WorldBorder border = world.getWorldBorder();
        if (!border.isInside(new Location(world, x, feetY, z)) || !border.isInside(new Location(world, x + 1.999, feetY, z + 1.999))) {
            return Optional.empty();
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (checkColumn(world, x + dx, floorY, z + dz, HEADROOM).isPresent()) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new Location(world, x + 1.0, feetY, z + 1.0));
    }

    /** ตรวจว่าพิกัดลงจอด RTP ยังปลอดภัยก่อนย้ายจริง (หลังอุ่นเครื่อง) */
    public boolean stillSafeForRtp(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return false;
        }
        int x = center.getBlockX() - 1;
        int z = center.getBlockZ() - 1;
        int floorY = center.getBlockY() - 1;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (checkColumn(world, x + dx, floorY, z + dz, HEADROOM).isPresent()) {
                    return false;
                }
            }
        }
        return world.getWorldBorder().isInside(center);
    }

    private Optional<Problem> checkColumn(World world, int x, int floorY, int z, int space) {
        Block floor = world.getBlockAt(x, floorY, z);
        Material floorType = floor.getType();
        if (DANGEROUS.contains(floorType)) {
            return Optional.of(Problem.DANGEROUS_FLOOR);
        }
        if (!floorType.isSolid() || Tag.LEAVES.isTagged(floorType)) {
            return Optional.of(Problem.NO_FLOOR);
        }
        for (int i = 1; i <= space; i++) {
            if (floorY + i >= world.getMaxHeight()) {
                break;
            }
            Block block = world.getBlockAt(x, floorY + i, z);
            Material type = block.getType();
            if (DANGEROUS.contains(type) || block.isLiquid()) {
                return Optional.of(Problem.DANGEROUS_SPACE);
            }
            if (!block.isPassable()) {
                return Optional.of(Problem.BLOCKED);
            }
        }
        return Optional.empty();
    }
}
