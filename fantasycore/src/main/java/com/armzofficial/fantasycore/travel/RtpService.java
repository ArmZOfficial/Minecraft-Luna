package com.armzofficial.fantasycore.travel;

import com.armzofficial.fantasycore.claim.ClaimAdapter;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * สุ่มวาร์ปตาม CASUAL-SURVIVAL §3:
 * allowlist โลก → cooldown/combat → สุ่มวงแหวนแบบกระจายตามพื้นที่ → โหลด chunk แบบ async (จำกัดงานต่อโลก)
 * → ตรวจ footprint 2×2 + ช่องหัว 3 + border + ไม่ทับ region → อุ่นเครื่อง → ตรวจซ้ำ → teleport → บันทึก receipt/cooldown
 */
public final class RtpService {

    public static final String RECEIPT_KIND = "rtp";

    private final Plugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final LandingValidator landing;
    private final ClaimAdapter claims;
    private final TeleportService teleports;
    private final TravelStore store;
    private final com.armzofficial.fantasycore.storage.Database database;
    private final Tasks tasks;

    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastSuccess = new ConcurrentHashMap<>();
    private final Map<UUID, Long> retryAfter = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> chunkJobs = new ConcurrentHashMap<>();

    public RtpService(Plugin plugin, Settings settings, Messages messages, LandingValidator landing, ClaimAdapter claims,
                      TeleportService teleports, TravelStore store,
                      com.armzofficial.fantasycore.storage.Database database, Tasks tasks) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.landing = landing;
        this.claims = claims;
        this.teleports = teleports;
        this.store = store;
        this.database = database;
        this.tasks = tasks;
    }

    public Map<String, Settings.RtpProfile> profiles() {
        return settings.rtpProfiles();
    }

    /** เวลาที่ต้องรอ (วินาที) จากข้อมูลในหน่วยความจำ — ใช้แสดงในเมนู */
    public long cooldownLeftCached(Player player) {
        Long last = lastSuccess.get(player.getUniqueId());
        if (last == null) {
            return 0;
        }
        long left = last + settings.rtpCooldownSeconds() * 1000L - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    public void start(Player player, String profileId) {
        Settings.RtpProfile profile = profileId == null ? null : settings.rtpProfiles().get(profileId.toLowerCase());
        if (profile == null) {
            messages.send(player, "rtp.unknown-profile", Messages.p("profiles", String.join(", ", settings.rtpProfiles().keySet())));
            return;
        }
        if (!player.hasPermission("fantasy.rtp.use")) {
            messages.send(player, "common.no-permission");
            return;
        }
        World world = Bukkit.getWorld(profile.world());
        if (world == null) {
            messages.send(player, "rtp.world-not-loaded", Messages.p("world", profile.display()));
            return;
        }
        if (player.isDead() || !teleports.canStart(player)) {
            return;
        }
        UUID id = player.getUniqueId();
        Long retry = retryAfter.get(id);
        if (retry != null && retry > System.currentTimeMillis()) {
            messages.send(player, "rtp.retry-wait", Messages.p("seconds", (retry - System.currentTimeMillis() + 999) / 1000));
            return;
        }
        if (!searching.add(id)) {
            messages.send(player, "rtp.already-searching");
            return;
        }
        boolean bypass = player.hasPermission("fantasy.rtp.bypass-cooldown");
        // cooldown อ่านจาก receipt ในฐานข้อมูล จึงไม่ reset เมื่อรีสตาร์ต
        tasks.then(database.async(() -> store.lastTime(id, RECEIPT_KIND)), (last, error) -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                searching.remove(id);
                return;
            }
            if (error != null) {
                searching.remove(id);
                messages.send(online, "common.storage-error");
                return;
            }
            OptionalLong lastTime = last == null ? OptionalLong.empty() : last;
            lastTime.ifPresent(time -> lastSuccess.put(id, time));
            long left = lastTime.isPresent()
                    ? lastTime.getAsLong() + settings.rtpCooldownSeconds() * 1000L - System.currentTimeMillis() : 0;
            if (left > 0 && !bypass) {
                searching.remove(id);
                messages.send(online, "rtp.cooldown", Messages.p("seconds", (left + 999) / 1000));
                return;
            }
            messages.send(online, "rtp.searching", Messages.p("world", profile.display()));
            long deadline = System.currentTimeMillis() + settings.rtpTimeoutSeconds() * 1000L;
            AtomicBoolean finished = new AtomicBoolean();
            Consumer<Optional<Location>> once = found -> {
                if (finished.compareAndSet(false, true)) {
                    onSearchFinished(id, world, profile, found);
                }
            };
            search(world, profile, new int[]{0}, deadline, once);
            // กันกรณี callback โหลด chunk ไม่กลับมา: ปิดการค้นหาหลัง timeout + 2 วินาที
            tasks.later((settings.rtpTimeoutSeconds() + 2) * 20L, () -> once.accept(Optional.empty()));
        });
    }

    private void onSearchFinished(UUID id, World world, Settings.RtpProfile profile, Optional<Location> found) {
        searching.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return;
        }
        if (found.isEmpty()) {
            retryAfter.put(id, System.currentTimeMillis() + settings.rtpFailureRetrySeconds() * 1000L);
            messages.send(player, "rtp.not-found", Messages.p("world", profile.display()));
            return;
        }
        Location target = found.get();
        target.setYaw(player.getLocation().getYaw());
        teleports.begin(player, settings.warmupSeconds(), messages.plain("rtp.label", Messages.p("world", profile.display())),
                p -> revalidate(p, world, profile, target),
                arrived -> {
                    long now = System.currentTimeMillis();
                    lastSuccess.put(id, now);
                    messages.send(player, "rtp.arrived", Messages.p("world", profile.display()),
                            Messages.p("x", arrived.getBlockX()), Messages.p("y", arrived.getBlockY()),
                            Messages.p("z", arrived.getBlockZ()));
                    tasks.then(database.async(() -> {
                        store.record(id, RECEIPT_KIND, world.getName(), arrived.getBlockX(), arrived.getBlockY(),
                                arrived.getBlockZ(), now);
                        return null;
                    }), (ignored, error) -> {
                    });
                });
    }

    private TeleportService.Resolution revalidate(Player player, World world, Settings.RtpProfile profile, Location target) {
        if (Bukkit.getWorld(world.getUID()) == null) {
            return TeleportService.Resolution.fail(messages.chat("rtp.world-not-loaded", Messages.p("world", profile.display())));
        }
        if (!landing.stillSafeForRtp(target) || claimed(world, target.getBlockX() - 1, target.getBlockZ() - 1)) {
            return TeleportService.Resolution.fail(messages.chat("rtp.target-changed"));
        }
        return TeleportService.Resolution.ok(target);
    }

    private boolean claimed(World world, int x, int z) {
        int buffer = settings.rtpClaimBuffer();
        return claims.anyRegionIntersects(world, x - buffer, z - buffer, x + 1 + buffer, z + 1 + buffer);
    }

    /** วนหา candidate ทีละจุด ไม่ generate chunk เกินโควตาต่อโลก ไม่บล็อก main thread */
    private void search(World world, Settings.RtpProfile profile, int[] attempts, long deadline,
                        Consumer<Optional<Location>> done) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (attempts[0] >= settings.rtpMaxCandidates() || System.currentTimeMillis() > deadline) {
            done.accept(Optional.empty());
            return;
        }
        int[] point = RtpMath.sample(ThreadLocalRandom.current(), profile.centerX(), profile.centerZ(),
                profile.minRadius(), profile.maxRadius());
        int x = point[0];
        int z = point[1];
        attempts[0]++;
        // ตรวจที่ถูกก่อนโหลด chunk: border และ region (index ของ WorldGuard อยู่ในหน่วยความจำ)
        if (!world.getWorldBorder().isInside(new Location(world, x, world.getMinHeight(), z))
                || !world.getWorldBorder().isInside(new Location(world, x + 1.999, world.getMinHeight(), z + 1.999))
                || claimed(world, x, z)) {
            tasks.sync(() -> search(world, profile, attempts, deadline, done));
            return;
        }
        AtomicInteger jobs = chunkJobs.computeIfAbsent(world.getName(), k -> new AtomicInteger());
        if (jobs.incrementAndGet() > settings.rtpMaxChunkJobsPerWorld()) {
            jobs.decrementAndGet();
            attempts[0]--; // ยังไม่ได้ใช้ candidate นี้ รอคิวแล้วสุ่มใหม่
            tasks.later(5, () -> search(world, profile, attempts, deadline, done));
            return;
        }
        world.getChunkAtAsync(x >> 4, z >> 4, true, chunk -> {
            jobs.decrementAndGet();
            if (chunk == null) {
                search(world, profile, attempts, deadline, done);
                return;
            }
            Optional<Location> landingSpot = landing.findRtpLanding(world, x, z);
            if (landingSpot.isPresent()) {
                done.accept(landingSpot);
            } else {
                search(world, profile, attempts, deadline, done);
            }
        });
    }

    public void forget(UUID player) {
        searching.remove(player);
        retryAfter.remove(player);
        lastSuccess.remove(player);
    }
}
