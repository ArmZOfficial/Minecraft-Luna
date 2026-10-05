package com.armzofficial.fantasycore.dungeon;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/** Only a fixed, explicitly owned new world is writable. Never rebuild a completed map. */
public final class MoonfallWorld {
    private static final String MARKER = "FantasyCore Moonfall training v1\n";
    private final Plugin plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey readyKey;
    private final NamespacedKey hashKey;
    private final InstanceSlot slot;
    private World world;
    private BukkitTask building;
    private String problem;

    public MoonfallWorld(Plugin plugin) {
        this(plugin,InstanceSlot.TRAINING);
    }
    public MoonfallWorld(Plugin plugin,InstanceSlot slot) {
        this.plugin=plugin;
        this.slot=Objects.requireNonNull(slot);
        ownerKey=new NamespacedKey(plugin,"moonfall_owner"); readyKey=new NamespacedKey(plugin,"moonfall_map_version");
        hashKey=new NamespacedKey(plugin,"moonfall_template_hash");
        try {
            Path folder=folder();
            if (Files.exists(folder) && markerValid(folder)) { load(); }
        } catch (Exception e) { problem=e.getMessage(); plugin.getLogger().warning("Moonfall world: "+problem); }
    }
    public World world() { return world; }
    public boolean owns(World candidate) { return world!=null && candidate!=null && world.getUID().equals(candidate.getUID()); }
    public boolean building() { return building!=null; }
    public boolean ready() {
        if(problem!=null || world==null || building!=null || world.getPersistentDataContainer().getOrDefault(readyKey,PersistentDataType.INTEGER,0)!=MoonfallMap.VERSION) { return false; }
        String hash=world.getPersistentDataContainer().getOrDefault(hashKey,PersistentDataType.STRING,"");
        return hash.equals(MoonfallMap.fingerprint()) || (!slot.party() && hash.isEmpty());
    }
    public String status() { return problem!=null?problem:building!=null?"กำลังสร้างแมพ":ready()?"แมพฝึกพร้อม":world==null?"ยังไม่ได้สร้างโลกฝึก":"แมพสร้างไม่ครบ — /fa dungeon build"; }

    public void build(CommandSender sender, Consumer<String> report) {
        if (building!=null) { report.accept("กำลังสร้างอยู่แล้ว"); return; }
        if (world!=null && world.getPersistentDataContainer().getOrDefault(readyKey,PersistentDataType.INTEGER,0)>0) {
            report.accept("แมพเคยสร้างเสร็จแล้ว ไม่เขียนทับงานตกแต่งเดิม (ตรวจ version/hash หากยังไม่ ready)"); return;
        }
        try {
            Path folder=folder();
            World loaded=Bukkit.getWorld(slot.world());
            if (loaded!=null && !owns(loaded)) { throw new IllegalStateException("ชื่อโลกถูกใช้อยู่ ไม่แก้ไขโลกที่ไม่มี ownership"); }
            if (Files.exists(folder)) {
                if (!markerValid(folder)) { throw new IllegalStateException("พบโฟลเดอร์โลกเดิมที่ไม่มี ownership — หยุดโดยไม่เขียนทับ"); }
            } else {
                Files.createDirectory(folder);
                Files.writeString(folder.resolve(".fantasycore-moonfall-owner"),marker(),StandardCharsets.UTF_8);
            }
            if (world==null) { load(); }
            if (!world.getPlayers().isEmpty()) { throw new IllegalStateException("ต้องให้ผู้เล่นออกจากโลกก่อนสร้าง"); }
            List<Map.Entry<MoonfallMap.Pos,String>> plan=new ArrayList<>(MoonfallMap.plan().entrySet());
            Map<String,BlockData> data=new HashMap<>();
            // Validate every palette entry before placing anything.
            plan.forEach(entry -> data.computeIfAbsent(entry.getValue(),Bukkit::createBlockData));
            int[] index={0};
            report.accept("เริ่มสร้าง Moonfall "+plan.size()+" บล็อกในโลกใหม่ "+slot.world());
            building=Bukkit.getScheduler().runTaskTimer(plugin,() -> {
                try {
                    long deadline=System.nanoTime()+3_000_000;
                    for(int count=0;index[0]<plan.size() && count<400 && System.nanoTime()<deadline;count++,index[0]++) {
                        var entry=plan.get(index[0]); var p=entry.getKey();
                        world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(data.get(entry.getValue()),false);
                    }
                    if (index[0]==plan.size()) {
                        building.cancel(); building=null;
                        world.setSpawnLocation(22,88,18,0);
                        // Save blocks before advertising readiness; interruption stays safely incomplete.
                        world.save(); world.getPersistentDataContainer().set(hashKey,PersistentDataType.STRING,MoonfallMap.fingerprint());
                        world.getPersistentDataContainer().set(readyKey,PersistentDataType.INTEGER,MoonfallMap.VERSION); world.save();
                        problem=null; report.accept("สร้างแมพฝึกเสร็จ — /fa dungeon visit เพื่อตรวจทางเดินก่อนเปิด enabled");
                    }
                } catch (RuntimeException e) {
                    if(building!=null) { building.cancel(); } building=null; problem=e.getMessage();
                    plugin.getLogger().warning("Moonfall build stopped: "+e); report.accept("สร้างไม่สำเร็จ: "+problem);
                }
            },1,1);
        } catch (Exception e) { problem=e.getMessage(); report.accept("สร้างไม่ได้: "+problem); }
    }

    private Path folder() {
        Path root=Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path folder=root.resolve(slot.world()).normalize();
        if (!folder.getParent().equals(root) || Files.isSymbolicLink(folder)) { throw new IllegalStateException("unsafe world path"); }
        return folder;
    }
    private String marker() { return slot.party()?"FantasyCore Moonfall instance v1:"+slot.key()+"\n":MARKER; }
    private boolean markerValid(Path folder) throws Exception {
        Path marker=folder.resolve(".fantasycore-moonfall-owner");
        return !Files.isSymbolicLink(marker) && Files.isRegularFile(marker) && Files.readString(marker,StandardCharsets.UTF_8).equals(marker());
    }
    private void load() {
        world=new WorldCreator(slot.world()).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (world==null) { throw new IllegalStateException("createWorld returned null"); }
        world.getPersistentDataContainer().set(ownerKey,PersistentDataType.INTEGER,1);
        world.setGameRule(GameRules.PVP,false); world.setDifficulty(Difficulty.NORMAL); world.setTime(18000); world.setStorm(false);
        world.setGameRule(GameRules.SPAWN_MOBS,false); world.setGameRule(GameRules.MOB_GRIEFING,false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,0); world.setGameRule(GameRules.ADVANCE_TIME,false);
        world.setGameRule(GameRules.ADVANCE_WEATHER,false); world.setGameRule(GameRules.KEEP_INVENTORY,true);
        world.setGameRule(GameRules.SPAWN_PHANTOMS,false); world.setGameRule(GameRules.SPAWN_PATROLS,false);
        world.setGameRule(GameRules.SPAWN_WANDERING_TRADERS,false); world.setGameRule(GameRules.MOB_DROPS,false);
        world.setGameRule(GameRules.BLOCK_DROPS,false); world.setGameRule(GameRules.PROJECTILES_CAN_BREAK_BLOCKS,false);
        world.getWorldBorder().setCenter(72,72); world.getWorldBorder().setSize(144);
    }
    public void gate(int stage,boolean closed) {
        if (world==null) { return; }
        MoonfallMap.gates(stage,closed).forEach((p,data) -> world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(Bukkit.createBlockData(data),false));
    }
    public void close() { if(building!=null) { building.cancel(); building=null; } }

    private static final class VoidGenerator extends ChunkGenerator {
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
        @Override public Location getFixedSpawnLocation(World world, Random random) { return new Location(world,22.5,88,18.5); }
    }
}
