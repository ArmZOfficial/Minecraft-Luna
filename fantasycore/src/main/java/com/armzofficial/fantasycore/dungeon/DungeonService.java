package com.armzofficial.fantasycore.dungeon;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.menu.Icons;
import com.armzofficial.fantasycore.reward.RewardService;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.travel.LandingValidator;
import com.armzofficial.fantasycore.travel.TeleportService;
import com.armzofficial.fantasycore.util.Tasks;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/** ponytail: one exclusive solo lane; party/parallel runs need separate instances, not a shared mob pool. */
public final class DungeonService implements Listener {
    private final Plugin plugin;
    private final Messages messages;
    private final Settings settings;
    private final Database database;
    private final DungeonStore store;
    private final Tasks tasks;
    private final TeleportService teleports;
    private final LandingValidator landing;
    private final MoonfallWorld map;
    private final NamespacedKey runKey;
    private final Set<UUID> transfers=new HashSet<>();
    private final Set<UUID> recovering=new HashSet<>();
    private final Map<UUID,Long> loadTokens=new HashMap<>();
    private final List<String> problems=new ArrayList<>();
    private DungeonRules rules;
    private Session current;
    private BukkitTask ticker;
    private long ticks;
    private boolean accessPending;
    private long transferSequence;

    private static final class Session {
        final UUID player;
        final Set<UUID> mobs=new HashSet<>();
        final Set<Chunk> chunks=new HashSet<>();
        DungeonStore.Run receipt;
        boolean active, wave, busy, closing, stopQueued;
        int stage;
        long started, nextSlam;
        Monster boss;
        BukkitTask slam;
        Location telegraph;
        BossBar bar;
        Session(UUID player) { this.player=player; this.started=System.currentTimeMillis(); }
    }

    public DungeonService(Plugin plugin,Messages messages,Settings settings,Database database,DungeonStore store,
                          Tasks tasks,TeleportService teleports,LandingValidator landing) {
        this.plugin=plugin; this.messages=messages; this.settings=settings; this.database=database; this.store=store;
        this.tasks=tasks; this.teleports=teleports; this.landing=landing;
        runKey=new NamespacedKey(plugin,"dungeon_run"); map=new MoonfallWorld(plugin);
        try {
            var yaml=new YamlConfiguration(); yaml.load(new File(plugin.getDataFolder(),"dungeons.yml")); rules=DungeonRules.load(yaml);
        } catch (Exception e) { problems.add("dungeons.yml: "+e.getMessage()); }
    }
    public boolean owns(World world) { return map.owns(world); }
    public boolean enabled() { return rules!=null && rules.enabled() && problems.isEmpty(); }
    public boolean ready() { return enabled() && map.ready(); }
    public String status() { return (enabled()?"เปิดฝึกเดี่ยว":"ปิดรับผู้เล่น")+" · "+map.status()+" · "+(current==null?"ว่าง":"มีรอบที่กำลังดำเนินการ"); }
    public List<String> problems() { return List.copyOf(problems); }
    public int durationMinutes() { return rules==null?18:(int)Math.ceil(rules.timeoutSeconds()/60.0); }
    public void start() {
        if (map.world()!=null) { cleanStale(map.world().getEntities()); }
        ticker=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,5,5);
        Bukkit.getOnlinePlayers().forEach(this::recover);
    }
    public void build(CommandSender sender) {
        if (current!=null) { messages.send(sender,"dungeon.occupied"); return; }
        map.build(sender,text -> messages.send(sender,"dungeon.admin-report",Messages.p("detail",text)));
    }
    public void visit(Player player) {
        if (!map.ready() || current!=null || accessPending || !map.world().getPlayers().isEmpty()) { messages.send(player,"dungeon.unavailable"); return; }
        accessPending=true;
        transfer(player,entry(),() -> current==null,ok -> { accessPending=false; if(ok) { messages.send(player,"dungeon.visiting"); } });
    }
    public void join(Player player) {
        if (!player.hasPermission("fantasy.dungeon")) { messages.send(player,"common.no-permission"); return; }
        if (!ready()) { messages.send(player,"dungeon.unavailable"); return; }
        if (current!=null || accessPending || !map.world().getPlayers().isEmpty() || recovering.contains(player.getUniqueId())) { messages.send(player,"dungeon.occupied"); return; }
        if (player.getGameMode()!=GameMode.SURVIVAL || player.isDead() || player.isInsideVehicle() || player.isFlying()
                || owns(player.getWorld()) || !teleports.canStart(player)) { messages.send(player,"dungeon.join-denied"); return; }
        if (landing.checkStanding(player.getLocation()).isPresent()) { messages.send(player,"dungeon.unsafe-exit"); return; }
        var at=player.getLocation(); var exit=new DungeonStore.ReturnPoint(at.getWorld().getUID(),at.getX(),at.getY(),at.getZ(),at.getYaw(),at.getPitch());
        var stack=Icons.of(Material.PRISMARINE_SHARD,messages.plain("dungeon.reward-name"),messages.lines("dungeon.reward-lore"));
        stack.setAmount(2);
        var meta=stack.getItemMeta(); meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"dungeon_reward"),PersistentDataType.INTEGER,1); stack.setItemMeta(meta);
        byte[] reward=stack.serializeAsBytes();
        Session session=new Session(player.getUniqueId()); current=session; player.closeInventory();
        byte[] frozen=reward;
        tasks.then(database.async(() -> store.begin(session.player,exit,"เศษจันทรา ×2",frozen)),(result,error) -> {
            if(error!=null || result.isEmpty()) {
                if(current==session) { current=null; }
                messages.send(player,error!=null?"dungeon.storage-error":"dungeon.occupied"); recover(player); return;
            }
            session.receipt=result.get();
            if(session.closing || !player.isOnline() || player.isDead() || current!=session || player.getGameMode()!=GameMode.SURVIVAL
                    || !ready() || !teleports.canStart(player) || !player.getWorld().equals(at.getWorld()) || player.getLocation().distanceSquared(at)>1) { stop(session,"entry cancelled"); return; }
            map.gate(0,true); map.gate(1,true);
            transfer(player,entry(),() -> current==session && !session.closing && ready() && player.getGameMode()==GameMode.SURVIVAL
                    && player.getWorld().equals(at.getWorld()) && player.getLocation().distanceSquared(at)<=1 && teleports.canStart(player),ok -> {
                if(!ok || current!=session || session.closing || !player.isOnline()) { stop(session,"entry teleport failed"); return; }
                tasks.then(database.async(() -> store.activate(session.receipt.id(),session.player)),(active,failure) -> {
                    if(failure!=null || !Boolean.TRUE.equals(active) || session.closing || current!=session || !player.isOnline() || !owns(player.getWorld())) {
                        stop(session,"activation failed"); return;
                    }
                    session.active=true; session.started=System.currentTimeMillis();
                    session.bar=BossBar.bossBar(messages.plain("dungeon.walk-hall"),1,BossBar.Color.BLUE,BossBar.Overlay.PROGRESS);
                    player.showBossBar(session.bar); messages.send(player,"dungeon.started");
                });
            });
        });
    }

    public void leave(Player player) {
        if(current!=null && current.player.equals(player.getUniqueId())) {
            if(current.closing) { recover(player); } else { messages.send(player,"dungeon.left"); stop(current,"player left"); }
        }
        else if(owns(player.getWorld())) { recover(player); }
        else { messages.send(player,"dungeon.not-playing"); }
    }
    public void abort(CommandSender sender) {
        if(current==null) { messages.send(sender,"dungeon.not-playing"); return; }
        stop(current,"admin abort: "+sender.getName()); messages.send(sender,"dungeon.admin-report",Messages.p("detail","ยกเลิกรอบและเริ่มนำผู้เล่นกลับ"));
    }
    private Location entry() { return new Location(map.world(),22.5,88,18.5,0,0); }
    private Location checkpoint(int stage) {
        return switch(stage) {
            case 0 -> new Location(map.world(),22.5,78,46.5,0,0);
            case 1 -> new Location(map.world(),106.5,38,61.5,-90,0);
            default -> new Location(map.world(),116.5,20,100.5,0,0);
        };
    }

    private void tick() {
        ticks+=5;
        if(current==null) { return; }
        Session s=current; Player player=Bukkit.getPlayer(s.player);
        if(s.closing) { return; }
        if(!s.active) { if(System.currentTimeMillis()-s.started>15000) { stop(s,"entry timeout"); } return; }
        if(player==null || player.isDead() || !owns(player.getWorld()) || !ready() || player.getGameMode()!=GameMode.SURVIVAL
                || player.isFlying() || player.isInsideVehicle()) { stop(s,"player unavailable"); return; }
        if(System.currentTimeMillis()-s.started>rules.timeoutSeconds()*1000L) { messages.send(player,"dungeon.timeout"); stop(s,"timeout"); return; }
        if(map.world().getPlayers().stream().anyMatch(p -> !p.getUniqueId().equals(s.player))) {
            map.world().getPlayers().stream().filter(p -> !p.getUniqueId().equals(s.player)).forEach(this::recover); stop(s,"unexpected visitor"); return;
        }
        if(s.busy || loadTokens.containsKey(s.player)) { return; }
        var room=MoonfallMap.ENCOUNTERS.get(s.stage); var at=player.getLocation();
        if(!s.wave) {
            if(room.inside(at.getX(),at.getY(),at.getZ())) { spawnWave(s,player); }
            else if(s.stage<2 && MoonfallMap.ENCOUNTERS.subList(s.stage+1,3).stream().anyMatch(r -> r.inside(at.getX(),at.getY(),at.getZ()))) {
                transfer(player,checkpoint(s.stage),() -> current==s && !s.closing,ok -> { if(!ok) { stop(s,"checkpoint teleport failed"); } });
            }
            return;
        }
        if(!room.inside(at.getX(),at.getY(),at.getZ())) {
            transfer(player,checkpoint(s.stage),() -> current==s && !s.closing,ok -> { if(!ok) { stop(s,"room boundary teleport failed"); } }); return;
        }
        double hp=0,max=0;
        for(UUID id : s.mobs) {
            Entity entity=Bukkit.getEntity(id);
            if(!(entity instanceof Monster mob) || !mob.isValid() || mob.isDead() || !encounter(mob)) { stop(s,"encounter entity missing"); return; }
            var pos=mob.getLocation();
            if(!room.inside(pos.getX(),pos.getY(),pos.getZ())) { mob.teleport(checkpoint(s.stage)); }
            mob.setTarget(player); hp+=mob.getHealth(); max+=Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).getValue();
        }
        s.bar.name(messages.plain("dungeon.encounter",Messages.p("room",roomName(s.stage)),Messages.p("hp",(int)Math.ceil(hp)),Messages.p("max",(int)Math.ceil(max))))
                .progress((float)Math.clamp(hp/Math.max(1,max),0,1)).color(s.stage==2?BossBar.Color.PURPLE:BossBar.Color.BLUE);
        if(s.stage==2 && s.boss!=null && ticks>=s.nextSlam && s.slam==null) { slam(s,player); }
        if(s.telegraph!=null) {
            for(int i=0;i<24;i++) {
                double angle=i*Math.PI*2/24;
                map.world().spawnParticle(Particle.DUST,s.telegraph.clone().add(Math.cos(angle)*4,0.15,Math.sin(angle)*4),1,0,0,0,0,
                        new Particle.DustOptions(Color.fromRGB(170,100,255),1.3f));
            }
        }
    }
    private String roomName(int stage) { return List.of("ห้องอักษรรูน","ห้องเก็บศิลาจันทร์","ผู้พิทักษ์จันทร์แตก").get(stage); }
    private void spawnWave(Session s,Player player) {
        s.wave=true;
        try {
            if(s.stage==0) {
                spawn(s,player,EntityType.ZOMBIE,18.5,78,59.5,rules.hallHealth(),4);
                spawn(s,player,EntityType.ZOMBIE,26.5,78,59.5,rules.hallHealth(),4);
                spawn(s,player,EntityType.ZOMBIE,22.5,78,63.5,rules.hallHealth(),4);
            } else if(s.stage==1) {
                spawn(s,player,EntityType.HUSK,117.5,38,56.5,rules.reliquaryHealth(),5);
                spawn(s,player,EntityType.HUSK,121.5,38,66.5,rules.reliquaryHealth(),5);
            } else {
                s.boss=spawn(s,player,EntityType.HUSK,116.5,20,118.5,rules.bossHealth(),6); s.nextSlam=ticks+160;
            }
            messages.send(player,"dungeon.room-start",Messages.p("room",roomName(s.stage)));
        } catch(RuntimeException e) { plugin.getLogger().warning("Moonfall spawn failed: "+e); stop(s,"spawn failed"); }
    }
    private Monster spawn(Session s,Player player,EntityType type,double x,double y,double z,double health,double attack) {
        Location at=new Location(map.world(),x,y,z,s.stage==1?90:180,0);
        if(landing.checkStanding(at).isPresent()) { throw new IllegalStateException("unsafe encounter spawn"); }
        Chunk chunk=at.getChunk(); if(s.chunks.add(chunk)) { chunk.addPluginChunkTicket(plugin); }
        Class<? extends Entity> entityClass=Objects.requireNonNull(type.getEntityClass());
        Entity entity=map.world().spawn(at,entityClass,CreatureSpawnEvent.SpawnReason.CUSTOM,mob -> {
            mob.getPersistentDataContainer().set(runKey,PersistentDataType.STRING,s.receipt.id());
            mob.setPersistent(false);
            if(mob instanceof Zombie zombie) { zombie.setAdult(); zombie.setCanBreakDoors(false); zombie.setShouldBurnInDay(false); }
            if(mob instanceof Monster monster) {
                Objects.requireNonNull(monster.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health); monster.setHealth(health);
                Objects.requireNonNull(monster.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(attack);
                monster.setRemoveWhenFarAway(false); monster.setCanPickupItems(false);
                var equipment=monster.getEquipment();
                if(equipment!=null) { equipment.clear(); equipment.setHelmet(new org.bukkit.inventory.ItemStack(Material.CHAINMAIL_HELMET)); equipment.setHelmetDropChance(0); }
            }
        });
        if(!(entity instanceof Monster mob) || !entity.isValid()) { entity.remove(); throw new IllegalStateException("spawn cancelled/provider conflict"); }
        s.mobs.add(mob.getUniqueId()); mob.setTarget(player); return mob;
    }
    private void slam(Session s,Player player) {
        s.telegraph=s.boss.getLocation().clone(); s.boss.setAI(false); s.boss.setVelocity(new org.bukkit.util.Vector());
        messages.send(player,"dungeon.slam-warning");
        s.slam=Bukkit.getScheduler().runTaskLater(plugin,() -> {
            s.slam=null;
            if(current!=s || s.closing || !s.active || s.boss==null || !s.boss.isValid() || s.boss.isDead()) { s.telegraph=null; return; }
            Location center=s.telegraph; s.telegraph=null; s.boss.setAI(true);
            if(player.isOnline() && !player.isDead() && owns(player.getWorld()) && center!=null
                    && center.distanceSquared(player.getLocation())<=16 && s.boss.hasLineOfSight(player)) { player.damage(8,s.boss); }
            map.world().playSound(s.boss.getLocation(),Sound.ENTITY_IRON_GOLEM_ATTACK,0.7f,0.7f);
            s.nextSlam=ticks+(s.boss.getHealth()<=rules.bossHealth()/2?160:200);
        },25);
    }
    public boolean encounter(Entity entity) {
        return current!=null && current.receipt!=null && !current.closing && owns(entity.getWorld())
                && current.receipt.id().equals(entity.getPersistentDataContainer().get(runKey,PersistentDataType.STRING));
    }
    private boolean tagged(Entity entity) { return entity.getPersistentDataContainer().has(runKey,PersistentDataType.STRING); }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(EntityDeathEvent event) {
        if(!owns(event.getEntity().getWorld()) || !tagged(event.getEntity())) { return; }
        event.getDrops().clear(); event.setDroppedExp(0);
        Session s=current;
        if(s==null || !s.active || s.closing || !encounter(event.getEntity()) || !s.mobs.remove(event.getEntity().getUniqueId())) { return; }
        if(!s.mobs.isEmpty()) { return; }
        s.busy=true; cancelSlam(s); releaseChunks(s);
        int cleared=s.stage;
        tasks.then(database.async(() -> {
            if(!store.advance(s.receipt.id(),s.player,cleared)) { throw new java.sql.SQLException("stage changed"); }
            return cleared==2?store.complete(s.receipt.id(),s.player,RewardService.period()):null;
        }),(completed,error) -> {
            if(current!=s || s.closing) { return; }
            if(error!=null) { messagesTo(s,"dungeon.storage-error"); stop(s,"progress storage failed"); return; }
            if(cleared==2 && completed==DungeonStore.Completion.INVALID) { messagesTo(s,"dungeon.storage-error"); stop(s,"invalid completion"); }
            else if(cleared==2) {
                messagesTo(s,completed==DungeonStore.Completion.REWARDED?"dungeon.rewarded":"dungeon.completed-daily"); stop(s,"completed");
            } else {
                map.gate(cleared,false); s.stage++; s.wave=false; s.busy=false;
                s.bar.name(messages.plain("dungeon.walk-next",Messages.p("room",roomName(s.stage)))).progress(1);
                messagesTo(s,"dungeon.room-cleared");
            }
        });
    }

    private void stop(Session s,String reason) {
        if(s.stopQueued) { return; }
        if(!s.closing && !reason.equals("completed") && !reason.equals("player left") && !reason.equals("disconnect")) { messagesTo(s,"dungeon.stopped"); }
        s.closing=true; cleanup(s);
        if(s.receipt==null) { return; } // begin callback retains the lane until it can abort the persisted row
        s.stopQueued=true;
        tasks.then(database.async(() -> { store.abort(s.receipt.id(),s.player,reason); return null; }),(ignored,error) -> {
            if(error!=null) { messagesTo(s,"dungeon.storage-error"); plugin.getLogger().warning("Dungeon return held until restart recovery: "+s.receipt.id()); }
            Player player=Bukkit.getPlayer(s.player);
            if(player!=null && !player.isDead()) { returnPlayer(player,s.receipt,ok -> { if(ok && current==s) { current=null; } }); }
            else if(current==s) { current=null; }
        });
    }
    private void cleanup(Session s) {
        cancelSlam(s);
        Player player=Bukkit.getPlayer(s.player); if(player!=null && s.bar!=null) { player.hideBossBar(s.bar); }
        for(UUID id : s.mobs) { Entity entity=Bukkit.getEntity(id); if(entity!=null) { entity.remove(); } }
        s.mobs.clear(); releaseChunks(s);
        if(map.world()!=null) { map.world().getEntitiesByClass(Projectile.class).forEach(Entity::remove); }
    }
    private void cancelSlam(Session s) { if(s.slam!=null) { s.slam.cancel(); s.slam=null; } s.telegraph=null; }
    private void releaseChunks(Session s) { s.chunks.forEach(c -> c.removePluginChunkTicket(plugin)); s.chunks.clear(); }
    private void messagesTo(Session s,String key) { Player p=Bukkit.getPlayer(s.player); if(p!=null) { messages.send(p,key); } }

    public void recover(Player player) {
        if(current!=null && current.player.equals(player.getUniqueId()) && !current.closing) { return; }
        if(!recovering.add(player.getUniqueId())) { return; }
        tasks.then(database.async(() -> store.pendingReturn(player.getUniqueId())),(receipt,error) -> {
            if(error!=null) { recovering.remove(player.getUniqueId()); messages.send(player,"dungeon.storage-error"); return; }
            if(!player.isOnline() || player.isDead()) { recovering.remove(player.getUniqueId()); return; }
            if(receipt.isPresent()) {
                var run=receipt.get();
                tasks.then(database.async(() -> { store.abort(run.id(),run.player(),"return recovery"); return null; }),(ignored,failure) ->
                        returnPlayer(player,run,ok -> { recovering.remove(player.getUniqueId()); if(ok && current!=null && current.player.equals(player.getUniqueId()) && current.closing) { current=null; } }));
            } else if(owns(player.getWorld())) {
                hub(player,ok -> recovering.remove(player.getUniqueId()));
            } else { recovering.remove(player.getUniqueId()); }
        });
    }
    private void returnPlayer(Player player,DungeonStore.Run run,Consumer<Boolean> done) {
        if(!owns(player.getWorld())) { markReturned(run,done); return; }
        World exitWorld=Bukkit.getWorld(run.exit().world());
        if(exitWorld==null || owns(exitWorld)) { hub(player,ok -> { if(ok) { markReturned(run,done); } else { done.accept(false); } }); return; }
        Location exit=new Location(exitWorld,run.exit().x(),run.exit().y(),run.exit().z(),run.exit().yaw(),run.exit().pitch());
        transfer(player,exit,ok -> {
            if(ok) { markReturned(run,done); }
            else { hub(player,fallback -> { if(fallback) { markReturned(run,done); } else { done.accept(false); } }); }
        });
    }
    private void markReturned(DungeonStore.Run run,Consumer<Boolean> done) {
        tasks.then(database.async(() -> store.returned(run.id(),run.player())),(ok,error) -> done.accept(error==null && Boolean.TRUE.equals(ok)));
    }
    private void hub(Player player,Consumer<Boolean> done) {
        World hub=Bukkit.getWorld(settings.spawnWorld());
        if(hub==null || owns(hub)) { hub=Bukkit.getWorlds().stream().filter(w -> !owns(w)).findFirst().orElse(null); }
        if(hub==null) { messages.send(player,"dungeon.exit-failed"); done.accept(false); return; }
        transfer(player,hub.getSpawnLocation().clone().add(0.5,0,0.5),ok -> { if(!ok) { messages.send(player,"dungeon.exit-failed"); } done.accept(ok); });
    }
    private void transfer(Player player,Location target,Consumer<Boolean> done) {
        transfer(player,target,() -> true,done);
    }
    private void transfer(Player player,Location target,java.util.function.BooleanSupplier eligible,Consumer<Boolean> done) {
        if(!player.isOnline() || player.isDead() || target.getWorld()==null) { done.accept(false); return; }
        UUID id=player.getUniqueId();
        if(loadTokens.containsKey(id)) { done.accept(false); return; }
        long token=++transferSequence; loadTokens.put(id,token);
        Consumer<Boolean> finish=ok -> { if(loadTokens.remove(id,token)) { done.accept(ok); } };
        tasks.later(300,() -> finish.accept(false));
        target.getWorld().getChunkAtAsync(target.getBlockX()>>4,target.getBlockZ()>>4).whenComplete((chunk,error) -> tasks.sync(() -> {
            if(!Objects.equals(loadTokens.get(id),token)) { return; }
            if(error!=null || !eligible.getAsBoolean() || !player.isOnline() || player.isDead() || landing.checkStanding(target).isPresent()) { finish.accept(false); return; }
            transfers.add(player.getUniqueId());
            boolean ok;
            try { ok=player.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN); }
            finally { transfers.remove(player.getUniqueId()); }
            finish.accept(ok);
        }));
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent event) {
        boolean participant=current!=null && !current.closing && current.player.equals(event.getPlayer().getUniqueId());
        if((participant || owns(event.getFrom().getWorld()) || owns(event.getTo().getWorld())) && !transfers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); messages.send(event.getPlayer(),"dungeon.use-leave");
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent event) {
        if(!owns(event.getEntity().getWorld())) { return; }
        if(event.getEntity() instanceof Player player) {
            if(current==null || !current.player.equals(player.getUniqueId()) || !current.active || current.closing) { event.setCancelled(true); }
        } else if(tagged(event.getEntity()) && !(event instanceof EntityDamageByEntityEvent)) { event.setCancelled(true); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent event) {
        if(!owns(event.getEntity().getWorld())) { return; }
        Entity source=event.getDamager();
        if(source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) { source=shooter; }
        boolean allowed=current!=null && current.active && !current.closing
                && ((source instanceof Player p && current.player.equals(p.getUniqueId()) && encounter(event.getEntity()))
                || (encounter(source) && event.getEntity() instanceof Player victim && current.player.equals(victim.getUniqueId())));
        if(!allowed) { event.setCancelled(true); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void target(EntityTargetLivingEntityEvent event) {
        if(owns(event.getEntity().getWorld()) && (current==null || !(event.getTarget() instanceof Player player) || !current.player.equals(player.getUniqueId()))) { event.setCancelled(true); }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { if(current!=null && current.player.equals(event.getPlayer().getUniqueId())) { stop(current,"disconnect"); } }
    @EventHandler public void join(PlayerJoinEvent event) { tasks.later(2,() -> recover(event.getPlayer())); }
    @EventHandler public void playerDeath(PlayerDeathEvent event) { if(current!=null && current.player.equals(event.getEntity().getUniqueId())) { stop(current,"player died"); } }
    @EventHandler public void respawn(PlayerRespawnEvent event) { tasks.later(2,() -> recover(event.getPlayer())); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void gameMode(PlayerGameModeChangeEvent event) {
        if(current!=null && current.player.equals(event.getPlayer().getUniqueId()) && event.getNewGameMode()!=GameMode.SURVIVAL) { stop(current,"gamemode changed"); }
    }
    @EventHandler public void load(ChunkLoadEvent event) { if(owns(event.getWorld())) { cleanStale(Arrays.asList(event.getChunk().getEntities())); } }
    private void cleanStale(Collection<? extends Entity> entities) { entities.stream().filter(e -> tagged(e) && !encounter(e)).forEach(Entity::remove); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void unload(WorldUnloadEvent event) { if(owns(event.getWorld())) { event.setCancelled(true); } }

    public void close() {
        if(ticker!=null) { ticker.cancel(); } map.close();
        if(current!=null) { cleanup(current); current.closing=true; }
        // No async callbacks can be relied on during disable; leave durable return flags for next enable/join.
        try { store.recoverInterrupted(); } catch(java.sql.SQLException e) { plugin.getLogger().severe("Dungeon shutdown journal: "+e); }
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(owns(player.getWorld()) && !player.isDead()) {
                World hub=Bukkit.getWorlds().stream().filter(w -> !owns(w)).findFirst().orElse(null);
                if(hub!=null && hub.isChunkLoaded(hub.getSpawnLocation().getBlockX()>>4,hub.getSpawnLocation().getBlockZ()>>4)
                        && landing.checkStanding(hub.getSpawnLocation()).isEmpty()) {
                    transfers.add(player.getUniqueId());
                    try { player.teleport(hub.getSpawnLocation(),PlayerTeleportEvent.TeleportCause.PLUGIN); }
                    finally { transfers.remove(player.getUniqueId()); }
                }
            }
        }
    }
}
