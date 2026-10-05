package com.armzofficial.fantasycore.reward;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.mail.MailService;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.util.Money;
import com.armzofficial.fantasycore.util.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * รับของรายวัน (CASUAL-SURVIVAL §4): วันใหม่ 00:00 Asia/Bangkok, รอบสะสม 7 ครั้งไม่บังคับต่อเนื่อง,
 * รับได้วันละครั้ง ไม่ไล่รับย้อนหลังวันที่ไม่ได้เข้า
 */
public final class RewardService {

    public static final String PROGRAM = "daily";
    public static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    private final Messages messages;
    private final RewardStore store;
    private final MailService mail;
    private final Database database;
    private final Tasks tasks;
    private final boolean enabled;
    private final List<RewardStore.DayReward> cycle;
    private final List<String> problems = new ArrayList<>();
    private final Set<UUID> claiming = ConcurrentHashMap.newKeySet();

    public RewardService(ConfigurationSection config, Messages messages, RewardStore store, MailService mail,
                         Database database, Tasks tasks) {
        this.messages = messages;
        this.store = store;
        this.mail = mail;
        this.database = database;
        this.tasks = tasks;
        this.enabled = config != null && config.getBoolean("enabled", true);
        this.cycle = Collections.unmodifiableList(parse(config));
    }

    /** สร้างของรางวัลบน main thread ตอนเปิดปลั๊กอิน แล้ว serialize เก็บไว้ */
    private List<RewardStore.DayReward> parse(ConfigurationSection config) {
        List<RewardStore.DayReward> days = new ArrayList<>();
        if (config == null) {
            problems.add("ไม่มีหัวข้อ rewards.daily ใน config.yml");
            return days;
        }
        List<Map<?, ?>> entries = config.getMapList("days");
        if (entries.isEmpty() || entries.size() > 14) {
            problems.add("rewards.daily.days ต้องมี 1–14 วัน (ตอนนี้ " + entries.size() + ")");
            return days;
        }
        for (int i = 0; i < entries.size(); i++) {
            Map<?, ?> entry = entries.get(i);
            long gold = number(entry.get("gold"));
            long red = number(entry.get("red"));
            List<RewardStore.RewardItem> items = new ArrayList<>();
            Object rawItems = entry.get("items");
            if (rawItems instanceof List<?> list) {
                for (Object raw : list) {
                    if (!(raw instanceof Map<?, ?> itemMap)) {
                        continue;
                    }
                    Material material = Material.matchMaterial(String.valueOf(itemMap.get("material")));
                    int amount = (int) number(itemMap.get("amount"));
                    if (material == null || !material.isItem() || material.isAir()) {
                        problems.add("วันที่ " + (i + 1) + ": material '" + itemMap.get("material") + "' ไม่ถูกต้อง");
                        continue;
                    }
                    if (amount < 1 || amount > material.getMaxStackSize()) {
                        problems.add("วันที่ " + (i + 1) + ": amount ของ " + material + " ต้องอยู่ระหว่าง 1–" + material.getMaxStackSize());
                        continue;
                    }
                    Object label = itemMap.get("label");
                    String text = label == null ? MailService.describe(new ItemStack(material, amount)) : label + " ×" + amount;
                    ItemStack stack = new ItemStack(material, amount);
                    if (label != null) {
                        ItemMeta meta = stack.getItemMeta();
                        meta.displayName(Component.text(String.valueOf(label)).decoration(TextDecoration.ITALIC, false));
                        stack.setItemMeta(meta);
                    }
                    items.add(new RewardStore.RewardItem(text, stack.serializeAsBytes()));
                }
            }
            if (gold < 0 || red < 0) {
                problems.add("วันที่ " + (i + 1) + ": gold/red ติดลบไม่ได้");
                gold = Math.max(0, gold);
                red = Math.max(0, red);
            }
            days.add(new RewardStore.DayReward(i + 1, gold, red, List.copyOf(items)));
        }
        return days;
    }

    private static long number(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean enabled() {
        return enabled && !cycle.isEmpty();
    }

    public List<RewardStore.DayReward> cycle() {
        return cycle;
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }

    public static String period() {
        return LocalDate.now(BANGKOK).toString();
    }

    public static Duration untilReset() {
        ZonedDateTime now = ZonedDateTime.now(BANGKOK);
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(BANGKOK));
    }

    public static String formatDuration(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        return minutes / 60 + " ชม. " + minutes % 60 + " นาที";
    }

    public CompletableFuture<RewardStore.ProgramStatus> status(UUID player) {
        return database.async(() -> store.status(player, PROGRAM, period(), Math.max(1, cycle.size())));
    }

    /** ข้อความสรุปรางวัลของวันหนึ่ง เช่น "150 ทอง + ขนมปัง ×8" */
    public String describe(RewardStore.DayReward reward) {
        List<String> parts = new ArrayList<>();
        if (reward.gold() > 0) {
            parts.add(Money.format(reward.gold()) + " ทอง");
        }
        if (reward.red() > 0) {
            parts.add(Money.format(reward.red()) + " เงินแดง");
        }
        for (RewardStore.RewardItem item : reward.items()) {
            parts.add(item.label());
        }
        return parts.isEmpty() ? "-" : String.join(" + ", parts);
    }

    /** รับรางวัลวันนี้ แล้วส่งของเข้า inventory ทันทีถ้ามีที่ ไม่งั้นรอในกล่องจดหมาย */
    public void claim(Player player, Runnable afterwards) {
        if (!enabled()) {
            messages.send(player, "reward.disabled");
            afterwards.run();
            return;
        }
        UUID id = player.getUniqueId();
        if (!claiming.add(id)) {
            // กดซ้ำระหว่างรอ: ไม่ส่งคำขอใหม่ (ฐานข้อมูลกันรับซ้ำอีกชั้นอยู่แล้ว) แต่ปลดสถานะรอของเมนู
            afterwards.run();
            return;
        }
        String period = period();
        tasks.then(database.async(() -> store.claim(id, PROGRAM, period, cycle)), (result, error) -> {
            claiming.remove(id);
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            if (error != null) {
                messages.send(online, "common.storage-error");
                afterwards.run();
                return;
            }
            switch (result.status()) {
                case ALREADY_CLAIMED -> {
                    messages.send(online, "reward.already", Messages.p("time", formatDuration(untilReset())));
                    afterwards.run();
                }
                case EMPTY_PROGRAM -> {
                    messages.send(online, "reward.disabled");
                    afterwards.run();
                }
                case CLAIMED -> {
                    messages.send(online, "reward.claimed", Messages.p("index", result.cycleIndex()),
                            Messages.p("cycle", cycle.size()), Messages.p("reward", describe(result.reward())),
                            Messages.p("wallet", Money.format(result.after().gold())));
                    if (result.mailIds().isEmpty()) {
                        afterwards.run();
                        return;
                    }
                    mail.claimIds(online, result.mailIds(), summary -> {
                        if (summary.remaining() > 0) {
                            messages.send(online, "reward.items-in-mail", Messages.p("count", summary.remaining()));
                        }
                        afterwards.run();
                    });
                }
            }
        });
    }
}
