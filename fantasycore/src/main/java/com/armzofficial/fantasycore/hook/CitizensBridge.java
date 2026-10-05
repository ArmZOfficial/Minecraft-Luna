package com.armzofficial.fantasycore.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * เชื่อม Citizens ผ่าน reflection — FantasyCore จึง build/รันได้โดยไม่ต้องมี Citizens
 * ใช้เฉพาะ API สาธารณะที่ตรวจกับ CitizensAPI (2026-10):
 * CitizensAPI.getNPCRegistry(), NPCRegistry.getNPC(Entity), NPC.getUniqueId()/getId()/getName()
 */
public final class CitizensBridge {

    /** NPC ของ Citizens: uuid คงที่ข้ามการ respawn (ต่างจาก entity UUID) */
    public record NpcRef(UUID uuid, int id, String name) {
    }

    private final Method getRegistry;
    private final Method getNpc;
    private final Method getUniqueId;
    private final Method getId;
    private final Method getName;

    private CitizensBridge(Method getRegistry, Method getNpc, Method getUniqueId, Method getId, Method getName) {
        this.getRegistry = getRegistry;
        this.getNpc = getNpc;
        this.getUniqueId = getUniqueId;
        this.getId = getId;
        this.getName = getName;
    }

    /** คืน empty ถ้าไม่มี Citizens หรือ API ไม่ตรงรุ่นที่รองรับ */
    public static Optional<CitizensBridge> detect(Logger log) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return Optional.empty();
        }
        try {
            ClassLoader loader = Bukkit.getPluginManager().getPlugin("Citizens").getClass().getClassLoader();
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI", true, loader);
            Class<?> registry = Class.forName("net.citizensnpcs.api.npc.NPCRegistry", true, loader);
            Class<?> npc = Class.forName("net.citizensnpcs.api.npc.NPC", true, loader);
            return Optional.of(new CitizensBridge(
                    api.getMethod("getNPCRegistry"),
                    registry.getMethod("getNPC", Entity.class),
                    npc.getMethod("getUniqueId"),
                    npc.getMethod("getId"),
                    npc.getMethod("getName")));
        } catch (ReflectiveOperationException | LinkageError e) {
            log.warning("พบ Citizens แต่ API ไม่ตรงรุ่นที่รองรับ — ปิดการผูก NPC ของ Citizens: " + e);
            return Optional.empty();
        }
    }

    /** entity นี้เป็น NPC ของ Citizens หรือไม่ (คืน empty ถ้าไม่ใช่) */
    public Optional<NpcRef> npcOf(Entity entity) {
        // Citizens ใส่ metadata "NPC" ให้ entity ของตัวเอง — เช็กก่อนเพื่อไม่เรียก reflection กับทุก entity
        if (entity == null || !entity.hasMetadata("NPC")) {
            return Optional.empty();
        }
        try {
            Object registry = getRegistry.invoke(null);
            Object npc = registry == null ? null : getNpc.invoke(registry, entity);
            if (npc == null) {
                return Optional.empty();
            }
            return Optional.of(new NpcRef((UUID) getUniqueId.invoke(npc), (Integer) getId.invoke(npc),
                    String.valueOf(getName.invoke(npc))));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
