package com.armzofficial.fantasycore.world;

import com.armzofficial.fantasycore.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * โหลด/สร้างโลกที่ config กำหนด (luma_housing, luma_resource) และตั้ง world border
 * โลกหลัก hub มาจาก level-name ใน server.properties ไม่สร้างที่นี่
 */
public final class WorldService {

    private final Settings settings;
    private final Logger log;
    private final List<String> problems = new ArrayList<>();

    public WorldService(Settings settings, Logger log) {
        this.settings = settings;
        this.log = log;
    }

    public void loadConfiguredWorlds() {
        for (Settings.WorldSpec spec : settings.worlds().values()) {
            World world = Bukkit.getWorld(spec.name());
            if (world == null) {
                if (!spec.create()) {
                    problems.add(spec.name() + ": ไม่ได้โหลดและตั้ง create: false");
                    log.warning("โลก " + spec.name() + " ไม่ได้โหลด (create: false)");
                    continue;
                }
                log.info("กำลังโหลด/สร้างโลก " + spec.name() + " (" + spec.environment() + ")");
                WorldCreator creator = new WorldCreator(spec.name()).environment(spec.environment()).type(WorldType.NORMAL);
                if (spec.seed() != null) {
                    creator.seed(spec.seed());
                }
                try {
                    world = creator.createWorld();
                } catch (RuntimeException e) {
                    problems.add(spec.name() + ": สร้างไม่สำเร็จ — " + e.getMessage());
                    log.severe("สร้างโลก " + spec.name() + " ไม่สำเร็จ: " + e);
                    continue;
                }
                if (world == null) {
                    problems.add(spec.name() + ": createWorld คืนค่า null");
                    continue;
                }
            }
            if (spec.borderRadius() > 0) {
                world.getWorldBorder().setCenter(spec.borderCenterX(), spec.borderCenterZ());
                world.getWorldBorder().setSize(spec.borderRadius() * 2.0);
            }
        }
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }
}
