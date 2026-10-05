package com.armzofficial.fantasycore.combat;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Scaling เฉพาะ NATURAL vanilla; snapshot ติด entity, ไม่คูณซ้ำเมื่อเดินข้ามระดับ/restart */
public final class DepthMonsterService implements Listener {
    private static final Set<EntityType> TYPES = Set.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.ENDERMAN);
    private final Plugin plugin;
    private final Map<String, NamespacedKey> keys = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final List<String> problems = new ArrayList<>();
    private Set<String> worlds = Set.of();
    private DepthDifficulty difficulty;
    private boolean configuredEnabled;
    private boolean showHealth;
    private BukkitTask ticker;

    public DepthMonsterService(Plugin plugin) {
        this.plugin = plugin;
        for (String name : List.of("rules", "tier_name", "rank", "damage_scale", "damage_cap", "color", "original_health", "scaled_health")) {
            keys.put(name, new NamespacedKey(plugin, "depth_" + name));
        }
        try {
            var yaml = new YamlConfiguration();
            yaml.load(new File(plugin.getDataFolder(), "monsters.yml"));
            if (!yaml.isBoolean("enabled") || !yaml.isBoolean("target-health-bar") || !yaml.isList("worlds")
                    || yaml.getList("worlds").stream().anyMatch(world -> !(world instanceof String s) || s.isBlank())) {
                throw new IllegalArgumentException("enabled/target-health-bar/worlds มีชนิดข้อมูลผิด");
            }
            configuredEnabled = yaml.getBoolean("enabled");
            showHealth = yaml.getBoolean("target-health-bar");
            worlds = Set.copyOf(yaml.getStringList("worlds"));
            if (worlds.isEmpty()) { throw new IllegalArgumentException("worlds ต้องไม่ว่าง"); }
            difficulty = DepthDifficulty.load(yaml);
        } catch (Exception e) {
            problems.add("monsters.yml: " + e.getMessage());
        }
    }

    public boolean enabled() { return configuredEnabled && difficulty != null && problems.isEmpty(); }
    public List<String> problems() { return List.copyOf(problems); }
    public String status() { return enabled() ? difficulty.tiers().size() + " ชั้น · " + String.join(", ", worlds)
            + " · HP cap " + difficulty.healthCap() + " · เพิ่ม damage ถึง " + difficulty.rawDamageCap() : "ปิด scaling/health bar"; }

    public void start() {
        if (!enabled()) {
            // Restore เฉพาะ mob ที่ Core เคยปรับ; ไม่แก้ mob/attribute ของ provider อื่น
            for (var world : Bukkit.getWorlds()) { world.getEntitiesByClass(Monster.class).forEach(this::restore); }
        } else if (showHealth) {
            ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::updateBars, 10, 10);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (!enabled() || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
                || !(event.getEntity() instanceof Monster monster) || !TYPES.contains(monster.getType())
                || !worlds.contains(monster.getWorld().getName()) || monster.hasMetadata("NPC") || monster.customName() != null
                || !monster.getPersistentDataContainer().getKeys().isEmpty() || !monster.getScoreboardTags().isEmpty()) { return; }
        var health = monster.getAttribute(Attribute.MAX_HEALTH);
        // ข้าม health modifiers ของ vanilla leader/custom provider เพื่อไม่คำนวณสูตร attribute ซ้อน
        if (health == null || !health.getModifiers().isEmpty() || health.getBaseValue() <= 0 || health.getBaseValue() > difficulty.healthCap()) { return; }
        var tier = difficulty.at(monster.getLocation().getBlockY());
        double original = health.getBaseValue();
        double scaled = difficulty.health(original, tier);
        double ratio = monster.getHealth() / health.getValue();
        health.setBaseValue(scaled);
        monster.setHealth(Math.min(health.getValue(), Math.max(0, ratio * health.getValue())));
        var pdc = monster.getPersistentDataContainer();
        pdc.set(keys.get("rules"), PersistentDataType.INTEGER, 1);
        pdc.set(keys.get("tier_name"), PersistentDataType.STRING, tier.name());
        pdc.set(keys.get("rank"), PersistentDataType.INTEGER, difficulty.tiers().indexOf(tier) + 1);
        pdc.set(keys.get("damage_scale"), PersistentDataType.DOUBLE, tier.damageScale());
        pdc.set(keys.get("damage_cap"), PersistentDataType.DOUBLE, difficulty.rawDamageCap());
        pdc.set(keys.get("color"), PersistentDataType.STRING, tier.color());
        pdc.set(keys.get("original_health"), PersistentDataType.DOUBLE, original);
        pdc.set(keys.get("scaled_health"), PersistentDataType.DOUBLE, scaled);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE)) { return; }
        Monster monster = event.getDamager() instanceof Monster direct ? direct
                : event.getDamager() instanceof Projectile shot && shot.getShooter() instanceof Monster shooter ? shooter : null;
        if (monster == null || !managed(monster)) { return; }
        if (!enabled()) { restore(monster); return; }
        if (!worlds.contains(monster.getWorld().getName()) || monster.hasMetadata("NPC")) { return; }
        var pdc = monster.getPersistentDataContainer();
        Double scale = pdc.get(keys.get("damage_scale"), PersistentDataType.DOUBLE);
        Double cap = pdc.get(keys.get("damage_cap"), PersistentDataType.DOUBLE);
        if (scale == null || cap == null) { return; }
        try { event.setDamage(DepthDifficulty.damage(event.getDamage(), scale, cap)); }
        catch (IllegalArgumentException ignored) { /* marker เสียรูป: ไม่เพิ่มความเสียหาย */ }
    }

    @EventHandler public void chunk(ChunkLoadEvent event) {
        if (!enabled()) for (var entity : event.getChunk().getEntities()) {
            if (entity instanceof Monster monster) { restore(monster); }
        }
    }

    @EventHandler public void quit(PlayerQuitEvent event) { hide(event.getPlayer()); }

    private boolean tagged(Monster monster) {
        return Integer.valueOf(1).equals(monster.getPersistentDataContainer().get(keys.get("rules"), PersistentDataType.INTEGER));
    }

    private boolean managed(Monster monster) {
        if (!tagged(monster)) { return false; }
        if (monster.hasMetadata("NPC") || !monster.getScoreboardTags().isEmpty()
                || monster.getPersistentDataContainer().getKeys().stream().anyMatch(key -> !keys.containsValue(key))) {
            restore(monster);
            return false;
        }
        return true;
    }

    private void restore(Monster monster) {
        if (!tagged(monster)) { return; }
        var pdc = monster.getPersistentDataContainer();
        Double original = pdc.get(keys.get("original_health"), PersistentDataType.DOUBLE);
        Double scaled = pdc.get(keys.get("scaled_health"), PersistentDataType.DOUBLE);
        var health = monster.getAttribute(Attribute.MAX_HEALTH);
        if (!monster.isDead() && original != null && scaled != null && Double.isFinite(original) && original > 0 && original <= 1000
                && health != null && Math.abs(health.getBaseValue() - scaled) < 0.000001) {
            double ratio = monster.getHealth() / health.getValue();
            health.setBaseValue(original);
            monster.setHealth(Math.min(health.getValue(), Math.max(0, ratio * health.getValue())));
        }
        keys.values().forEach(pdc::remove);
    }

    private void updateBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!worlds.contains(player.getWorld().getName()) || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
                hide(player); continue;
            }
            var eye = player.getEyeLocation();
            var hit = player.getWorld().rayTrace(eye, eye.getDirection(), 8, FluidCollisionMode.NEVER, true, 0.15,
                    entity -> entity != player && entity instanceof LivingEntity living && !living.isDead());
            if (hit == null || !(hit.getHitEntity() instanceof Monster monster) || !managed(monster)) {
                hide(player); continue;
            }
            var max = monster.getAttribute(Attribute.MAX_HEALTH);
            if (max == null || max.getValue() <= 0 || !Double.isFinite(max.getValue())) { hide(player); continue; }
            var pdc = monster.getPersistentDataContainer();
            String name = pdc.get(keys.get("tier_name"), PersistentDataType.STRING);
            String color = pdc.get(keys.get("color"), PersistentDataType.STRING);
            Integer rank = pdc.get(keys.get("rank"), PersistentDataType.INTEGER);
            if (name == null || name.length() > 40 || rank == null || rank < 1 || rank > 8 || color == null) { hide(player); continue; }
            BossBar.Color tint;
            try { tint = BossBar.Color.valueOf(color); }
            catch (IllegalArgumentException e) { hide(player); continue; }
            Component title = Component.text("✦ " + name + " · ชั้น " + rank + " | ", NamedTextColor.GOLD)
                    .append(Component.translatable(monster.getType().translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(" · " + (int) Math.ceil(monster.getHealth()) + "/" + (int) Math.ceil(max.getValue()) + " HP", NamedTextColor.AQUA));
            float progress = (float) Math.clamp(monster.getHealth() / max.getValue(), 0, 1);
            BossBar bar = bars.get(player.getUniqueId());
            if (bar == null) {
                bar = BossBar.bossBar(title, progress, tint, BossBar.Overlay.PROGRESS);
                bars.put(player.getUniqueId(), bar);
                player.showBossBar(bar);
            } else { bar.name(title).progress(progress).color(tint); }
        }
    }

    private void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) { player.hideBossBar(bar); }
    }

    public void close() {
        if (ticker != null) { ticker.cancel(); }
        for (Player player : Bukkit.getOnlinePlayers()) { hide(player); }
        bars.clear();
    }
}
