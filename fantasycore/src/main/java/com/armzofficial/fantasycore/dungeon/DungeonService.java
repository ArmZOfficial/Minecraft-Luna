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

/** ponytail: three reusable exclusive worlds; no world cloning/deletion during a run. */
public final class DungeonService implements Listener {
    private final Plugin plugin;
    private final Messages messages;
    private final Settings settings;
    private final Database database;
    private final DungeonGroupStore store;
    private final Tasks tasks;
    private final TeleportService teleports;
    private final LandingValidator landing;
    private final EnumMap<InstanceSlot,MoonfallWorld> maps=new EnumMap<>(InstanceSlot.class);
    private final EnumMap<InstanceSlot,Session> sessions=new EnumMap<>(InstanceSlot.class);
    private final PartyRoster parties=new PartyRoster(System::currentTimeMillis);
    private final NamespacedKey runKey;
    private final Set<UUID> transfers=new HashSet<>(), recovering=new HashSet<>();
    private final Set<InstanceSlot> visiting=new HashSet<>();
    private final Map<UUID,Long> loadTokens=new HashMap<>();
    private final List<String> problems=new ArrayList<>();
    private DungeonRules rules;
    private BukkitTask ticker;
    private long ticks,transferSequence;

    private static final class Session {
        final UUID leader;
        final PartyRoster.Snapshot party;
        final InstanceSlot slot;
        final MoonfallWorld map;
        final List<DungeonGroupStore.Member> members;
        final DungeonPresence presence;
        final Set<UUID> ids,mobs=new HashSet<>();
        final Set<Chunk> chunks=new HashSet<>();
        String run;
        boolean active,wave,busy,closing,stopQueued,terminal,paused;
        int stage;
        long started=System.currentTimeMillis(),nextSlam;
        Monster boss;
        BukkitTask slam;
        Location telegraph;
        BossBar bar;
        Session(UUID leader,PartyRoster.Snapshot party,InstanceSlot slot,MoonfallWorld map,List<DungeonGroupStore.Member> members) {
            this.leader=leader; this.party=party; this.slot=slot; this.map=map; this.members=List.copyOf(members);
            ids=Set.copyOf(members.stream().map(DungeonGroupStore.Member::player).toList()); presence=new DungeonPresence(ids);
        }
    }
    public DungeonService(Plugin plugin,Messages messages,Settings settings,Database database,DungeonGroupStore store,
                          Tasks tasks,TeleportService teleports,LandingValidator landing) {
        this.plugin=plugin; this.messages=messages; this.settings=settings; this.database=database; this.store=store;
        this.tasks=tasks; this.teleports=teleports; this.landing=landing; runKey=new NamespacedKey(plugin,"dungeon_run");
        for(var slot:InstanceSlot.values()) { maps.put(slot,new MoonfallWorld(plugin,slot)); }
        try { var yaml=new YamlConfiguration(); yaml.load(new File(plugin.getDataFolder(),"dungeons.yml")); rules=DungeonRules.load(yaml); }
        catch(Exception e) { problems.add("dungeons.yml: "+e.getMessage()); }
    }
    public PartyRoster parties() { return parties; }
    public boolean playing(UUID player) { return session(player)!=null; }
    public boolean owns(World world) { return maps.values().stream().anyMatch(map -> map.owns(world)); }
    public boolean enabled() { return rules!=null && rules.enabled() && problems.isEmpty(); }
    public boolean ready() { return enabled() && maps.get(InstanceSlot.TRAINING).ready(); }
    public List<String> problems() { return List.copyOf(problems); }
    public int durationMinutes() { return rules==null?18:(int)Math.ceil(rules.timeoutSeconds()/60.0); }
    public String status() {
        return (enabled()?"เปิดฝึก":"ปิดรับผู้เล่น")+" · training: "+maps.get(InstanceSlot.TRAINING).status()
                +" · party: "+(enabled() && rules.partyEnabled()?"เปิด":"ปิด")+" · active "+sessions.size()+"/3";
    }
    public List<String> instanceStatus() { return maps.entrySet().stream().map(e -> e.getKey().key()+": "+e.getValue().status()+" · "+(sessions.containsKey(e.getKey())?"มีรอบ":"ว่าง")).toList(); }
    public void start() {
        maps.values().stream().map(MoonfallWorld::world).filter(Objects::nonNull).forEach(w -> cleanStale(w.getEntities()));
        ticker=Bukkit.getScheduler().runTaskTimer(plugin,() -> tick(),5,5); Bukkit.getOnlinePlayers().forEach(this::recover);
    }
    private Session session(UUID player) { return sessions.values().stream().filter(s -> s.ids.contains(player)).findFirst().orElse(null); }
    private Session worldSession(World world) { return sessions.values().stream().filter(s -> s.map.owns(world)).findFirst().orElse(null); }
    private boolean current(Session s) { return sessions.get(s.slot)==s; }
    public void build(CommandSender sender) { build(sender,InstanceSlot.TRAINING); }
    public void build(CommandSender sender,InstanceSlot slot) {
        if(sessions.containsKey(slot) || visiting.contains(slot) || maps.values().stream().anyMatch(MoonfallWorld::building)) { messages.send(sender,"dungeon.occupied"); return; }
        maps.get(slot).build(sender,text -> messages.send(sender,"dungeon.admin-report",Messages.p("detail",text)));
    }
    public void visit(Player player) { visit(player,InstanceSlot.TRAINING); }
    public void visit(Player player,InstanceSlot slot) {
        MoonfallWorld map=maps.get(slot);
        if(!map.ready() || sessions.containsKey(slot) || visiting.contains(slot) || !map.world().getPlayers().isEmpty() || playing(player.getUniqueId())) { messages.send(player,"dungeon.unavailable"); return; }
        visiting.add(slot);
        transfer(player,new Location(map.world(),22.5,88,18.5),() -> !sessions.containsKey(slot),ok -> {
            visiting.remove(slot); if(ok) { messages.send(player,"dungeon.visiting"); }
        });
    }
    public void join(Player leader) {
        if(!leader.hasPermission("fantasy.dungeon")) { messages.send(leader,"common.no-permission"); return; }
        if(!enabled()) { messages.send(leader,"dungeon.unavailable"); return; }
        var party=parties.party(leader.getUniqueId()).orElse(null);
        if(party!=null && !party.leader().equals(leader.getUniqueId())) { messages.send(leader,"party.not-leader"); return; }
        List<UUID> ids=party==null?List.of(leader.getUniqueId()):party.members();
        boolean group=ids.size()>1;
        if(group && !rules.partyEnabled()) { messages.send(leader,"dungeon.party-disabled"); return; }
        InstanceSlot slot=Arrays.stream(InstanceSlot.values()).filter(s -> s.party()==group && maps.get(s).ready() && !sessions.containsKey(s)
                && !visiting.contains(s) && maps.get(s).world().getPlayers().isEmpty()).findFirst().orElse(null);
        if(slot==null) { messages.send(leader,"dungeon.occupied"); return; }
        List<DungeonGroupStore.Member> members=new ArrayList<>();
        for(UUID id:ids) {
            Player player=Bukkit.getPlayer(id);
            if(player==null || !eligible(player) || playing(id) || recovering.contains(id) || loadTokens.containsKey(id)
                    || landing.checkStanding(player.getLocation()).isPresent()) { messages.send(leader,"dungeon.member-not-ready",Messages.p("player",name(id))); return; }
            Location at=player.getLocation();
            var exit=new DungeonStore.ReturnPoint(at.getWorld().getUID(),at.getX(),at.getY(),at.getZ(),at.getYaw(),at.getPitch());
            members.add(new DungeonGroupStore.Member(id,exit,"เศษจันทรา ×2",reward()));
        }
        if(party!=null && !parties.lock(party)) { messages.send(leader,"party.locked"); return; }
        Session s=new Session(leader.getUniqueId(),party,slot,maps.get(slot),members); sessions.put(slot,s);
        livePlayers(s).forEach(Player::closeInventory);
        tasks.then(database.async(() -> store.begin(slot,s.leader,party==null?null:party.id(),members,MoonfallMap.fingerprint())),(result,error) -> {
            if(error!=null || result.isEmpty()) {
                broadcast(s,error!=null?"dungeon.storage-error":"dungeon.occupied"); release(s); livePlayers(s).forEach(this::recover); return;
            }
            s.run=result.get();
            if(s.closing || !current(s) || !allAtOrigin(s)) { stop(s,"entry cancelled"); return; }
            try { s.map.gate(0,true); s.map.gate(1,true); }
            catch(RuntimeException e) { plugin.getLogger().warning("Moonfall entry gate: "+e); stop(s,"entry gate failed"); return; }
            int[] left={s.members.size()};
            for(int i=0;i<s.members.size();i++) {
                var member=s.members.get(i); Player player=Bukkit.getPlayer(member.player());
                transfer(player,entry(s,i),() -> current(s) && !s.closing && atOrigin(member),ok -> {
                    if(!ok || s.closing || !current(s)) { stop(s,"entry teleport failed"); return; }
                    if(--left[0]==0) {
                        tasks.then(database.async(() -> store.activate(s.run,s.leader)),(active,failure) -> {
                            if(failure!=null || !Boolean.TRUE.equals(active) || s.closing || !current(s) || livePlayers(s).size()!=s.members.size()
                                    || livePlayers(s).stream().anyMatch(p -> !s.map.owns(p.getWorld()) || p.isDead() || p.getGameMode()!=GameMode.SURVIVAL)) { stop(s,"activation failed"); return; }
                            s.active=true; s.started=System.currentTimeMillis();
                            s.bar=BossBar.bossBar(messages.plain("dungeon.walk-hall"),1,BossBar.Color.BLUE,BossBar.Overlay.PROGRESS);
                            livePlayers(s).forEach(p -> p.showBossBar(s.bar)); broadcast(s,"dungeon.started");
                        });
                    }
                });
            }
        });
    }
    private String name(UUID id) { Player p=Bukkit.getPlayer(id); return p==null?id.toString().substring(0,8):p.getName(); }
    private byte[] reward() {
        var item=Icons.of(Material.PRISMARINE_SHARD,messages.plain("dungeon.reward-name"),messages.lines("dungeon.reward-lore")); item.setAmount(2);
        var meta=item.getItemMeta(); meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"dungeon_reward"),PersistentDataType.INTEGER,1); item.setItemMeta(meta); return item.serializeAsBytes();
    }
    private boolean eligible(Player p) { return p.isOnline() && p.hasPermission("fantasy.dungeon") && p.getGameMode()==GameMode.SURVIVAL && !p.isDead()
            && !p.isFlying() && !p.isInsideVehicle() && !owns(p.getWorld()) && teleports.canStart(p); }
    private boolean atOrigin(DungeonGroupStore.Member member) {
        Player p=Bukkit.getPlayer(member.player()); var exit=member.exit();
        return p!=null && eligible(p) && p.getWorld().getUID().equals(exit.world())
                && p.getLocation().distanceSquared(new Location(p.getWorld(),exit.x(),exit.y(),exit.z()))<=1;
    }
    private boolean allAtOrigin(Session s) { return enabled() && s.map.ready() && s.members.stream().allMatch(this::atOrigin); }
    private List<Player> livePlayers(Session s) { return s.members.stream().map(m -> Bukkit.getPlayer(m.player())).filter(Objects::nonNull).filter(Player::isOnline).toList(); }
    private double offset(Session s,int index) { return (index-(s.members.size()-1)/2.0)*2; }
    private Location entry(Session s,int index) { return new Location(s.map.world(),22.5+offset(s,index),88,18.5,0,0); }
    private Location checkpoint(Session s,int index) {
        double offset=offset(s,index);
        return switch(s.stage) {
            case 0 -> s.wave?new Location(s.map.world(),22.5+offset,78,46.5):entry(s,index);
            case 1 -> new Location(s.map.world(),106.5,38,61.5+offset,-90,0);
            default -> new Location(s.map.world(),116.5+offset,20,100.5,0,0);
        };
    }
    public void leave(Player p) {
        Session s=session(p.getUniqueId());
        if(s!=null && !s.closing) { broadcast(s,"dungeon.left"); stop(s,"member left"); }
        else if(s!=null || owns(p.getWorld())) { recover(p); } else { messages.send(p,"dungeon.not-playing"); }
    }
    public void abort(CommandSender sender) {
        if(sessions.isEmpty()) { messages.send(sender,"dungeon.not-playing"); return; }
        List.copyOf(sessions.values()).forEach(s -> stop(s,"admin abort: "+sender.getName()));
        messages.send(sender,"dungeon.admin-report",Messages.p("detail","ยกเลิกทุกรอบและเริ่มนำสมาชิกกลับ"));
    }
    private void tick() { ticks+=5; for(Session s:List.copyOf(sessions.values())) { tick(s); } }
    private void tick(Session s) {
        if(s.closing) { tryRelease(s); return; }
        long now=System.currentTimeMillis();
        if(!s.active) { if(now-s.started>15000) { stop(s,"entry timeout"); } return; }
        if(!enabled() || !s.map.ready() || (s.slot.party() && !rules.partyEnabled())) { stop(s,"disabled"); return; }
        if(now-s.started>rules.timeoutSeconds()*1000L) { broadcast(s,"dungeon.timeout"); stop(s,"timeout"); return; }
        for(var member:s.members) {
            Player p=Bukkit.getPlayer(member.player());
            if(p==null || !p.isOnline()) { s.presence.disconnected(member.player(),now); }
            else if(p.isDead() || !s.map.owns(p.getWorld()) || p.getGameMode()!=GameMode.SURVIVAL || p.isFlying() || p.isInsideVehicle()) {
                if(!s.presence.canReconnect(member.player(),now)) { stop(s,"member unavailable"); return; }
            }
        }
        if(s.presence.expired(now)) { broadcast(s,"dungeon.reconnect-expired"); stop(s,"reconnect grace expired"); return; }
        if(s.presence.waiting()) {
            pause(s); s.bar.name(messages.plain("dungeon.paused",Messages.p("seconds",s.presence.secondsLeft(now)))).color(BossBar.Color.YELLOW); return;
        }
        if(s.paused) { resume(s); }
        var visitors=s.map.world().getPlayers().stream().filter(p -> !s.ids.contains(p.getUniqueId()) && !recovering.contains(p.getUniqueId())).toList();
        if(!visitors.isEmpty()) { visitors.forEach(this::recover); stop(s,"unexpected visitor"); return; }
        if(s.busy || s.ids.stream().anyMatch(loadTokens::containsKey)) { return; }
        var room=MoonfallMap.ENCOUNTERS.get(s.stage); List<Player> players=livePlayers(s);
        if(!s.wave) {
            boolean together=players.size()==s.members.size() && players.stream().allMatch(p -> inside(room,p.getLocation()));
            if(together) { spawnWave(s); }
            else {
                for(int i=0;i<s.members.size();i++) {
                    Player p=Bukkit.getPlayer(s.members.get(i).player());
                    if(p!=null && s.stage<2 && MoonfallMap.ENCOUNTERS.subList(s.stage+1,3).stream().anyMatch(r -> inside(r,p.getLocation()))) { tether(s,p,i); }
                }
                s.bar.name(messages.plain("dungeon.gather",Messages.p("room",roomName(s.stage)))).progress(1);
            }
            return;
        }
        for(int i=0;i<s.members.size();i++) { Player p=Bukkit.getPlayer(s.members.get(i).player()); if(p!=null && !inside(room,p.getLocation())) { tether(s,p,i); } }
        double hp=0,max=0;
        for(UUID id:s.mobs) {
            Entity entity=Bukkit.getEntity(id);
            if(!(entity instanceof Monster mob) || !mob.isValid() || mob.isDead() || !encounter(mob)) { stop(s,"encounter missing"); return; }
            if(!inside(room,mob.getLocation())) { mob.teleport(checkpoint(s,0)); }
            Player nearest=players.stream().min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(mob.getLocation()))).orElse(null);
            mob.setTarget(nearest); hp+=mob.getHealth(); max+=Objects.requireNonNull(mob.getAttribute(Attribute.MAX_HEALTH)).getValue();
        }
        s.bar.name(messages.plain("dungeon.encounter",Messages.p("room",roomName(s.stage)),Messages.p("hp",(int)Math.ceil(hp)),Messages.p("max",(int)Math.ceil(max))))
                .progress((float)Math.clamp(hp/Math.max(1,max),0,1)).color(s.stage==2?BossBar.Color.PURPLE:BossBar.Color.BLUE);
        if(s.stage==2 && s.boss!=null && ticks>=s.nextSlam && s.slam==null) { slam(s); }
        if(s.telegraph!=null) { for(int i=0;i<24;i++) { double angle=i*Math.PI*2/24;
            s.map.world().spawnParticle(Particle.DUST,s.telegraph.clone().add(Math.cos(angle)*4,0.15,Math.sin(angle)*4),1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(170,100,255),1.3f));
        } }
    }
    private static boolean inside(MoonfallMap.Room room,Location at) { return room.inside(at.getX(),at.getY(),at.getZ()); }
    private void tether(Session s,Player p,int index) { transfer(p,checkpoint(s,index),() -> current(s) && !s.closing,ok -> { if(!ok) { stop(s,"checkpoint teleport failed"); } }); }
    private void pause(Session s) {
        if(s.paused) { return; } s.paused=true; cancelSlam(s);
        for(UUID id:s.mobs) { Entity e=Bukkit.getEntity(id); if(e instanceof Monster mob && mob.isValid()) { mob.setAI(false); mob.setVelocity(new org.bukkit.util.Vector()); } }
        broadcast(s,"dungeon.party-waiting");
    }
    private void resume(Session s) {
        s.paused=false; s.nextSlam=ticks+80;
        for(UUID id:s.mobs) { Entity e=Bukkit.getEntity(id); if(e instanceof Monster mob && mob.isValid()) { mob.setAI(true); } }
        broadcast(s,"dungeon.party-resumed");
    }
    private String roomName(int stage) { return List.of("ห้องอักษรรูน","ห้องเก็บศิลาจันทร์","ผู้พิทักษ์จันทร์แตก").get(stage); }
    private void spawnWave(Session s) {
        s.wave=true;
        try {
            if(s.stage==0) {
                spawn(s,EntityType.ZOMBIE,18.5,78,59.5,rules.hallHealth(),4); spawn(s,EntityType.ZOMBIE,26.5,78,59.5,rules.hallHealth(),4); spawn(s,EntityType.ZOMBIE,22.5,78,63.5,rules.hallHealth(),4);
            } else if(s.stage==1) {
                spawn(s,EntityType.HUSK,117.5,38,56.5,rules.reliquaryHealth(),5); spawn(s,EntityType.HUSK,121.5,38,66.5,rules.reliquaryHealth(),5);
            } else { s.boss=spawn(s,EntityType.HUSK,116.5,20,118.5,rules.bossHealth(),6); s.nextSlam=ticks+160; }
            livePlayers(s).forEach(p -> messages.send(p,"dungeon.room-start",Messages.p("room",roomName(s.stage))));
        } catch(RuntimeException e) { plugin.getLogger().warning("Moonfall spawn: "+e); stop(s,"spawn failed"); }
    }
    private Monster spawn(Session s,EntityType type,double x,double y,double z,double baseHealth,double baseAttack) {
        Location at=new Location(s.map.world(),x,y,z,s.stage==1?90:180,0);
        if(landing.checkStanding(at).isPresent()) { throw new IllegalStateException("unsafe spawn"); }
        Chunk chunk=at.getChunk(); if(s.chunks.add(chunk)) { chunk.addPluginChunkTicket(plugin); }
        double health=DungeonRules.scaledHealth(baseHealth,s.members.size()),attack=DungeonRules.scaledAttack(baseAttack,s.members.size());
        Entity entity=s.map.world().spawn(at,Objects.requireNonNull(type.getEntityClass()),CreatureSpawnEvent.SpawnReason.CUSTOM,mob -> {
            mob.getPersistentDataContainer().set(runKey,PersistentDataType.STRING,s.run); mob.setPersistent(false);
            if(mob instanceof Zombie zombie) { zombie.setAdult(); zombie.setCanBreakDoors(false); zombie.setShouldBurnInDay(false); }
            if(mob instanceof Monster monster) {
                Objects.requireNonNull(monster.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health); monster.setHealth(health);
                Objects.requireNonNull(monster.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(attack);
                monster.setRemoveWhenFarAway(false); monster.setCanPickupItems(false);
                var equipment=monster.getEquipment(); if(equipment!=null) { equipment.clear(); equipment.setHelmet(new org.bukkit.inventory.ItemStack(Material.CHAINMAIL_HELMET)); equipment.setHelmetDropChance(0); }
            }
        });
        if(!(entity instanceof Monster mob) || !entity.isValid()) { entity.remove(); throw new IllegalStateException("spawn cancelled/provider conflict"); }
        s.mobs.add(mob.getUniqueId()); mob.setTarget(livePlayers(s).getFirst()); return mob;
    }
    private void slam(Session s) {
        s.telegraph=s.boss.getLocation().clone(); s.boss.setAI(false); s.boss.setVelocity(new org.bukkit.util.Vector()); broadcast(s,"dungeon.slam-warning");
        s.slam=Bukkit.getScheduler().runTaskLater(plugin,() -> {
            s.slam=null;
            if(!current(s) || s.closing || s.paused || !s.active || s.boss==null || !s.boss.isValid() || s.boss.isDead()) { s.telegraph=null; return; }
            Location center=s.telegraph; s.telegraph=null; s.boss.setAI(true);
            for(Player p:livePlayers(s)) { if(s.closing) { break; } if(!p.isDead() && s.map.owns(p.getWorld()) && center!=null && center.distanceSquared(p.getLocation())<=16 && s.boss.hasLineOfSight(p)) {
                p.damage(DungeonRules.scaledAttack(8,s.members.size()),s.boss);
            } }
            if(s.closing) { return; }
            s.map.world().playSound(s.boss.getLocation(),Sound.ENTITY_IRON_GOLEM_ATTACK,0.7f,0.7f);
            s.nextSlam=ticks+(s.boss.getHealth()<=DungeonRules.scaledHealth(rules.bossHealth(),s.members.size())/2?160:200);
        },25);
    }
    public boolean encounter(Entity entity) {
        Session s=worldSession(entity.getWorld());
        return s!=null && s.run!=null && !s.closing && s.run.equals(entity.getPersistentDataContainer().get(runKey,PersistentDataType.STRING));
    }
    private boolean tagged(Entity entity) { return entity.getPersistentDataContainer().has(runKey,PersistentDataType.STRING); }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(EntityDeathEvent event) {
        if(!owns(event.getEntity().getWorld()) || !tagged(event.getEntity())) { return; }
        event.getDrops().clear(); event.setDroppedExp(0); Session s=worldSession(event.getEntity().getWorld());
        if(s==null || !s.active || s.closing || !encounter(event.getEntity())) { return; }
        if(s.paused || s.presence.waiting()) { stop(s,"encounter died while paused"); return; }
        if(!s.mobs.remove(event.getEntity().getUniqueId()) || !s.mobs.isEmpty()) { return; }
        s.busy=true; cancelSlam(s); releaseChunks(s); int cleared=s.stage;
        tasks.then(database.async(() -> {
            if(!store.advance(s.run,s.leader,cleared)) { throw new java.sql.SQLException("stage changed"); }
            return cleared==2?store.complete(s.run,s.leader,RewardService.period()):null;
        }),(completed,error) -> {
            if(!current(s) || s.closing) { return; }
            if(error!=null || (cleared==2 && !completed.valid())) { broadcast(s,"dungeon.storage-error"); stop(s,"progress storage failed"); return; }
            if(cleared==2) {
                completed.members().forEach((id,result) -> { Player p=Bukkit.getPlayer(id); if(p!=null) { messages.send(p,result==DungeonStore.Completion.REWARDED?"dungeon.rewarded":"dungeon.completed-daily"); } });
                stop(s,"completed");
            } else {
                s.map.gate(cleared,false); s.stage++; s.wave=false; s.busy=false;
                s.bar.name(messages.plain("dungeon.walk-next",Messages.p("room",roomName(s.stage)))).progress(1); broadcast(s,"dungeon.room-cleared");
            }
        });
    }
    private void stop(Session s,String reason) {
        if(s.stopQueued) { return; }
        if(!s.closing && !reason.equals("completed") && !reason.equals("member left")) { broadcast(s,"dungeon.stopped"); }
        s.closing=true; cleanup(s); if(s.run==null) { return; } s.stopQueued=true;
        tasks.then(database.async(() -> { store.abort(s.run,s.leader,reason); return null; }),(ignored,error) -> {
            s.terminal=error==null;
            if(error!=null) { broadcast(s,"dungeon.storage-error"); }
            for(var member:s.members) { Player p=Bukkit.getPlayer(member.player()); if(p!=null && p.isOnline() && !p.isDead()) {
                returnPlayer(p,obligation(s,member),ok -> tryRelease(s));
            } }
            tryRelease(s);
        });
    }
    private DungeonStore.Run obligation(Session s,DungeonGroupStore.Member member) { return new DungeonStore.Run(s.run,member.player(),"ABORTED",s.stage,true,member.exit()); }
    private void tryRelease(Session s) { if(current(s) && s.closing && s.terminal && s.map.world().getPlayers().isEmpty()) { release(s); } }
    private void release(Session s) { if(current(s)) { sessions.remove(s.slot); } if(s.party!=null) { parties.unlock(s.party.id()); } }
    private void cleanup(Session s) {
        cancelSlam(s); if(s.bar!=null) { livePlayers(s).forEach(p -> p.hideBossBar(s.bar)); }
        for(UUID id:s.mobs) { Entity e=Bukkit.getEntity(id); if(e!=null) { e.remove(); } } s.mobs.clear(); releaseChunks(s);
        if(s.map.world()!=null) { s.map.world().getEntitiesByClass(Projectile.class).forEach(Entity::remove); }
    }
    private void cancelSlam(Session s) { if(s.slam!=null) { s.slam.cancel(); s.slam=null; } s.telegraph=null; }
    private void releaseChunks(Session s) { s.chunks.forEach(c -> c.removePluginChunkTicket(plugin)); s.chunks.clear(); }
    private void broadcast(Session s,String key) { livePlayers(s).forEach(p -> messages.send(p,key)); }

    public void recover(Player p) {
        Session s=session(p.getUniqueId());
        if(s!=null && s.active && !s.closing) { reconnect(s,p); return; }
        if(s!=null && !s.closing) { return; }
        if(!recovering.add(p.getUniqueId())) { return; }
        tasks.then(database.async(() -> store.pendingReturn(p.getUniqueId())),(receipt,error) -> {
            if(error!=null || !p.isOnline() || p.isDead()) { recovering.remove(p.getUniqueId()); if(error!=null) { messages.send(p,"dungeon.storage-error"); } return; }
            if(receipt.isPresent()) {
                var run=receipt.get();
                tasks.then(database.async(() -> { store.abort(run.id(),run.player(),"return recovery"); return null; }),(ignored,failure) -> {
                    if(s!=null && failure==null) { s.terminal=true; }
                    returnPlayer(p,run,ok -> { recovering.remove(p.getUniqueId()); if(s!=null) { tryRelease(s); } });
                });
            } else if(owns(p.getWorld())) { hub(p,ok -> recovering.remove(p.getUniqueId())); }
            else { recovering.remove(p.getUniqueId()); }
        });
    }
    private void reconnect(Session s,Player p) {
        long now=System.currentTimeMillis();
        if(!s.presence.canReconnect(p.getUniqueId(),now)) { if(s.presence.expired(now)) { stop(s,"late reconnect"); } return; }
        if(recovering.contains(p.getUniqueId())) { return; }
        if(p.isDead() || p.getGameMode()!=GameMode.SURVIVAL || p.isFlying() || p.isInsideVehicle() || !p.hasPermission("fantasy.dungeon")) { stop(s,"reconnect unavailable"); return; }
        recovering.add(p.getUniqueId()); int index=s.members.stream().map(DungeonGroupStore.Member::player).toList().indexOf(p.getUniqueId());
        transfer(p,checkpoint(s,index),() -> current(s) && !s.closing && s.presence.canReconnect(p.getUniqueId(),System.currentTimeMillis()),ok -> {
            recovering.remove(p.getUniqueId());
            if(!ok || !s.presence.arrived(p.getUniqueId(),System.currentTimeMillis())) { stop(s,"reconnect failed"); return; }
            if(s.bar!=null) { p.showBossBar(s.bar); } messages.send(p,"dungeon.reconnected");
        });
    }
    private void returnPlayer(Player p,DungeonStore.Run run,Consumer<Boolean> done) {
        if(!owns(p.getWorld())) { markReturned(run,done); return; }
        World world=Bukkit.getWorld(run.exit().world());
        if(world==null || owns(world)) { hub(p,ok -> { if(ok) { markReturned(run,done); } else { done.accept(false); } }); return; }
        Location exit=new Location(world,run.exit().x(),run.exit().y(),run.exit().z(),run.exit().yaw(),run.exit().pitch());
        transfer(p,exit,ok -> { if(ok) { markReturned(run,done); } else { hub(p,fallback -> { if(fallback) { markReturned(run,done); } else { done.accept(false); } }); } });
    }
    private void markReturned(DungeonStore.Run run,Consumer<Boolean> done) { tasks.then(database.async(() -> store.returned(run.id(),run.player())),(ok,error) -> done.accept(error==null && Boolean.TRUE.equals(ok))); }
    private void hub(Player p,Consumer<Boolean> done) {
        World world=Bukkit.getWorld(settings.spawnWorld()); if(world==null || owns(world)) { world=Bukkit.getWorlds().stream().filter(w -> !owns(w)).findFirst().orElse(null); }
        if(world==null) { messages.send(p,"dungeon.exit-failed"); done.accept(false); return; }
        transfer(p,world.getSpawnLocation().clone().add(0.5,0,0.5),ok -> { if(!ok) { messages.send(p,"dungeon.exit-failed"); } done.accept(ok); });
    }
    private void transfer(Player p,Location target,Consumer<Boolean> done) { transfer(p,target,() -> true,done); }
    private void transfer(Player p,Location target,java.util.function.BooleanSupplier eligible,Consumer<Boolean> done) {
        if(p==null || !p.isOnline() || p.isDead() || target.getWorld()==null) { done.accept(false); return; }
        UUID id=p.getUniqueId(); if(loadTokens.containsKey(id)) { done.accept(false); return; }
        long token=++transferSequence; loadTokens.put(id,token);
        Consumer<Boolean> finish=ok -> { if(loadTokens.remove(id,token)) { done.accept(ok); } }; tasks.later(300,() -> finish.accept(false));
        target.getWorld().getChunkAtAsync(target.getBlockX()>>4,target.getBlockZ()>>4).whenComplete((chunk,error) -> tasks.sync(() -> {
            if(!Objects.equals(loadTokens.get(id),token)) { return; }
            if(error!=null || !eligible.getAsBoolean() || !p.isOnline() || p.isDead() || landing.checkStanding(target).isPresent()) { finish.accept(false); return; }
            transfers.add(id); boolean ok;
            try { ok=p.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN); }
            catch(RuntimeException e) { plugin.getLogger().warning("Moonfall transfer: "+e); ok=false; }
            finally { transfers.remove(id); } finish.accept(ok);
        }));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent e) {
        Session s=session(e.getPlayer().getUniqueId());
        if((s!=null && !s.closing || owns(e.getFrom().getWorld()) || owns(e.getTo().getWorld())) && !transfers.contains(e.getPlayer().getUniqueId())) { e.setCancelled(true); messages.send(e.getPlayer(),"dungeon.use-leave"); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent e) {
        if(!owns(e.getEntity().getWorld())) { return; }
        Session s=worldSession(e.getEntity().getWorld());
        if(e.getEntity() instanceof Player p) { if(s==null || !s.ids.contains(p.getUniqueId()) || !s.active || s.closing || s.paused || s.presence.waiting()) { e.setCancelled(true); } }
        else if(tagged(e.getEntity()) && !(e instanceof EntityDamageByEntityEvent)) { e.setCancelled(true); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hit(EntityDamageByEntityEvent e) {
        if(!owns(e.getEntity().getWorld())) { return; } Session s=worldSession(e.getEntity().getWorld()); Entity source=e.getDamager();
        if(source instanceof Projectile shot && shot.getShooter() instanceof Entity shooter) { source=shooter; }
        boolean allowed=s!=null && s.map.owns(source.getWorld()) && s.active && !s.closing && !s.paused && !s.presence.waiting()
                && ((source instanceof Player attacker && s.ids.contains(attacker.getUniqueId()) && encounter(e.getEntity()))
                || (encounter(source) && e.getEntity() instanceof Player victim && s.ids.contains(victim.getUniqueId())));
        if(!allowed) { e.setCancelled(true); }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void target(EntityTargetLivingEntityEvent e) {
        if(!owns(e.getEntity().getWorld())) { return; } Session s=worldSession(e.getEntity().getWorld());
        if(s==null || !s.active || s.paused || s.presence.waiting() || s.closing || !(e.getTarget() instanceof Player p) || !s.ids.contains(p.getUniqueId())) { e.setCancelled(true); }
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        Session s=session(e.getPlayer().getUniqueId()); if(s==null || s.closing) { return; }
        if(!s.active) { stop(s,"disconnect during entry"); } else { s.presence.disconnected(e.getPlayer().getUniqueId(),System.currentTimeMillis()); pause(s); }
    }
    @EventHandler public void join(PlayerJoinEvent e) { recovering.add(e.getPlayer().getUniqueId()); tasks.later(2,() -> { recovering.remove(e.getPlayer().getUniqueId()); recover(e.getPlayer()); }); }
    @EventHandler public void playerDeath(PlayerDeathEvent e) { Session s=session(e.getEntity().getUniqueId()); if(s!=null) { stop(s,"member died"); } }
    @EventHandler public void respawn(PlayerRespawnEvent e) { tasks.later(2,() -> recover(e.getPlayer())); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void gameMode(PlayerGameModeChangeEvent e) { Session s=session(e.getPlayer().getUniqueId()); if(s!=null && e.getNewGameMode()!=GameMode.SURVIVAL) { stop(s,"gamemode changed"); } }
    @EventHandler public void load(ChunkLoadEvent e) { if(owns(e.getWorld())) { cleanStale(Arrays.asList(e.getChunk().getEntities())); } }
    private void cleanStale(Collection<? extends Entity> entities) { entities.stream().filter(e -> tagged(e) && !encounter(e)).forEach(Entity::remove); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void unload(WorldUnloadEvent e) { if(owns(e.getWorld())) { e.setCancelled(true); } }
    public void close() {
        if(ticker!=null) { ticker.cancel(); } maps.values().forEach(MoonfallWorld::close);
        for(Session s:List.copyOf(sessions.values())) { s.closing=true; cleanup(s); if(s.party!=null) { parties.unlock(s.party.id()); } }
        try { store.recoverInterrupted(); } catch(java.sql.SQLException e) { plugin.getLogger().severe("Dungeon shutdown journal: "+e); }
        for(Player p:Bukkit.getOnlinePlayers()) { if(owns(p.getWorld()) && !p.isDead()) {
            World hub=Bukkit.getWorlds().stream().filter(w -> !owns(w)).findFirst().orElse(null);
            if(hub!=null && hub.isChunkLoaded(hub.getSpawnLocation().getBlockX()>>4,hub.getSpawnLocation().getBlockZ()>>4) && landing.checkStanding(hub.getSpawnLocation()).isEmpty()) {
                transfers.add(p.getUniqueId()); try { p.teleport(hub.getSpawnLocation(),PlayerTeleportEvent.TeleportCause.PLUGIN); } finally { transfers.remove(p.getUniqueId()); }
            }
        } }
    }
}
