package com.armzofficial.fantasycore.home;

import com.armzofficial.fantasycore.claim.ClaimAdapter;
import com.armzofficial.fantasycore.claim.ClaimInfo;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.travel.LandingValidator;
import com.armzofficial.fantasycore.travel.TeleportService;
import com.armzofficial.fantasycore.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Home เป็นเจ้าของ /sethome /home (CASUAL-SURVIVAL §9):
 * ตั้งได้เฉพาะโลกที่อนุญาต + ในแปลงที่เป็นเจ้าของ/สมาชิก ตรวจสิทธิ์แปลงซ้ำทุกครั้งก่อนวาร์ป
 */
public final class HomeService {

    private final Settings settings;
    private final Messages messages;
    private final HomeStore store;
    private final Database database;
    private final ClaimAdapter claims;
    private final LandingValidator landing;
    private final TeleportService teleports;
    private final Tasks tasks;
    private final Map<UUID, List<String>> nameCache = new ConcurrentHashMap<>();

    public HomeService(Settings settings, Messages messages, HomeStore store, Database database, ClaimAdapter claims,
                       LandingValidator landing, TeleportService teleports, Tasks tasks) {
        this.settings = settings;
        this.messages = messages;
        this.store = store;
        this.database = database;
        this.claims = claims;
        this.landing = landing;
        this.teleports = teleports;
        this.tasks = tasks;
    }

    public int limit(Player player) {
        int limit = settings.homeDefaultLimit();
        for (Map.Entry<String, Integer> entry : settings.homeLimitPermissions().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                limit = Math.max(limit, entry.getValue());
            }
        }
        return Math.min(limit, settings.homeMaxLimit());
    }

    public CompletableFuture<List<HomeRecord>> list(UUID player) {
        return database.async(() -> {
            List<HomeRecord> homes = store.list(player);
            nameCache.put(player, homes.stream().map(HomeRecord::display).toList());
            return homes;
        });
    }

    /** ชื่อบ้านจากการโหลดครั้งล่าสุด — ใช้กับ tab-complete โดยไม่ query บน main thread */
    public List<String> cachedNames(UUID player) {
        return nameCache.getOrDefault(player, List.of());
    }

    public void forget(UUID player) {
        nameCache.remove(player);
    }

    private void refreshNames(UUID player) {
        list(player);
    }

    // ------------------------------------------------------------ set

    public void setHome(Player player, String rawName, boolean confirm) {
        Optional<HomeNames.Name> parsed = HomeNames.parse(rawName == null ? HomeNames.DEFAULT : rawName);
        if (parsed.isEmpty()) {
            messages.send(player, "home.invalid-name", Messages.p("max", HomeNames.MAX_LENGTH));
            return;
        }
        HomeNames.Name name = parsed.get();
        Location location = player.getLocation();
        Optional<String> placeProblem = checkPlace(player, location);
        if (placeProblem.isPresent()) {
            messages.send(player, placeProblem.get(), Messages.p("worlds", String.join(", ", settings.homeWorlds())));
            return;
        }
        if (landing.checkStanding(location).isPresent()) {
            messages.send(player, "home.unsafe-here");
            return;
        }
        String regionId = claims.playerClaimAt(location, player).map(ClaimInfo::regionId).orElse(null);
        long now = System.currentTimeMillis();
        World world = location.getWorld();
        HomeRecord record = new HomeRecord(player.getUniqueId(), name.key(), name.display(), world.getUID(), world.getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), regionId, now, now);
        int limit = limit(player);
        UUID id = player.getUniqueId();
        tasks.then(database.async(() -> {
            Optional<HomeRecord> previous = store.get(id, name.key());
            HomeStore.SaveResult result = store.save(record, limit, confirm);
            int count = store.count(id);
            return new SetOutcome(result, previous.orElse(null), count);
        }), (outcome, error) -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            if (error != null) {
                messages.send(online, "common.storage-error");
                return;
            }
            refreshNames(id);
            switch (outcome.result()) {
                case CREATED -> messages.send(online, "home.created", Messages.p("name", name.display()),
                        Messages.p("coords", record.coords()), Messages.p("count", outcome.count()), Messages.p("limit", limit));
                case UPDATED -> messages.send(online, "home.moved", Messages.p("name", name.display()),
                        Messages.p("old", outcome.previous().coords()), Messages.p("coords", record.coords()));
                case NEEDS_CONFIRM -> messages.send(online, "home.confirm-move", Messages.p("name", name.display()),
                        Messages.p("old", outcome.previous().coords()), Messages.p("coords", record.coords()));
                case LIMIT_REACHED -> messages.send(online, "home.limit", Messages.p("limit", limit));
            }
        });
    }

    private record SetOutcome(HomeStore.SaveResult result, HomeRecord previous, int count) {
    }

    /** คืน key ข้อความเมื่อพื้นที่นี้ตั้งบ้านไม่ได้ */
    private Optional<String> checkPlace(Player player, Location location) {
        if (!settings.homeWorlds().contains(location.getWorld().getName())) {
            return Optional.of("home.world-not-allowed");
        }
        if (!settings.homeRequiresClaim()) {
            return Optional.empty();
        }
        if (!claims.available()) {
            return Optional.of("home.claims-unavailable");
        }
        Optional<ClaimInfo> claim = claims.playerClaimAt(location, player);
        if (claim.isEmpty()) {
            return Optional.of("home.not-in-claim");
        }
        if (!claim.get().trusted()) {
            return Optional.of("home.not-member");
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------ teleport

    public void teleport(Player player, HomeRecord home) {
        teleports.begin(player, settings.warmupSeconds(),
                messages.plain("home.label", Messages.p("name", home.display())),
                p -> resolve(p, home),
                arrived -> messages.send(player, "home.arrived", Messages.p("name", home.display())));
    }

    private TeleportService.Resolution resolve(Player player, HomeRecord home) {
        World world = Bukkit.getWorld(home.worldId());
        if (world == null) {
            return TeleportService.Resolution.fail(messages.chat("home.world-missing", Messages.p("name", home.display())));
        }
        Location location = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        Optional<String> placeProblem = checkPlace(player, location);
        if (placeProblem.isPresent()) {
            // สิทธิ์แปลงหาย/ถูกถอน/โลกไม่อนุญาตแล้ว — ไม่วาร์ปเข้าแปลงคนอื่นด้วยข้อมูลเก่า
            return TeleportService.Resolution.fail(messages.chat("home.no-longer-valid", Messages.p("name", home.display())));
        }
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            world.getChunkAt(location); // บ้านของผู้เล่นคนเดียว โหลด chunk เดียวได้
        }
        if (landing.checkStanding(location).isPresent()) {
            return TeleportService.Resolution.fail(messages.chat("home.unsafe-target", Messages.p("name", home.display())));
        }
        return TeleportService.Resolution.ok(location);
    }

    /** /home [ชื่อ] — ไม่มีชื่อและมีบ้านเดียวจะไปบ้านนั้น */
    public void teleportByName(Player player, String rawName, Runnable openMenuIfMany) {
        UUID id = player.getUniqueId();
        tasks.then(database.async(() -> store.list(id)), (homes, error) -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            if (error != null) {
                messages.send(online, "common.storage-error");
                return;
            }
            if (homes.isEmpty()) {
                messages.send(online, "home.none");
                return;
            }
            if (rawName == null) {
                if (homes.size() == 1) {
                    teleport(online, homes.getFirst());
                } else {
                    openMenuIfMany.run();
                }
                return;
            }
            Optional<HomeNames.Name> name = HomeNames.parse(rawName);
            Optional<HomeRecord> match = name.flatMap(n -> homes.stream().filter(h -> h.key().equals(n.key())).findFirst());
            if (match.isEmpty()) {
                messages.send(online, "home.not-found", Messages.p("name", rawName),
                        Messages.p("names", String.join(", ", homes.stream().map(HomeRecord::display).toList())));
                return;
            }
            teleport(online, match.get());
        });
    }

    public void delete(Player player, String rawName) {
        Optional<HomeNames.Name> name = HomeNames.parse(rawName);
        if (name.isEmpty()) {
            messages.send(player, "home.invalid-name", Messages.p("max", HomeNames.MAX_LENGTH));
            return;
        }
        UUID id = player.getUniqueId();
        tasks.then(database.async(() -> store.delete(id, name.get().key())), (deleted, error) -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            if (error != null) {
                messages.send(online, "common.storage-error");
            } else if (Boolean.TRUE.equals(deleted)) {
                refreshNames(id);
                messages.send(online, "home.deleted", Messages.p("name", name.get().display()));
            } else {
                messages.send(online, "home.not-found", Messages.p("name", rawName), Messages.p("names", "-"));
            }
        });
    }
}
