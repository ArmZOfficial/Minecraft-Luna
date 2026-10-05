package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.dungeon.InstanceSlot;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.exchange.ExchangeService;
import com.armzofficial.fantasycore.hook.VaultHook;
import com.armzofficial.fantasycore.item.ItemTemplate;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import com.armzofficial.fantasycore.menu.BankHistory;
import com.armzofficial.fantasycore.menu.AdminPanelMenu;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.station.StationRecord;
import com.armzofficial.fantasycore.storage.PlayerStore;
import com.armzofficial.fantasycore.util.Money;
import com.armzofficial.fantasycore.util.PlayerDataSaving;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * /fa — panel and text commands share the same permission/preview/ledger path.
 * ทุกงานเขียนต้องมีเหตุผล, preview → /fa confirm &lt;รหัส&gt;, และบันทึก audit พร้อม operation ID
 */
public final class AdminCommand implements TabExecutor {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BankHistory.BANGKOK);

    private final Services services;
    private final PendingConfirmations confirmations = new PendingConfirmations();
    private final AdminChatInput input;

    public AdminCommand(Services services) {
        this.services = services;
        this.input = new AdminChatInput(services);
    }

    public AdminChatInput input() { return input; }
    public void close() { input.close(); confirmations.clear(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        return execute(sender, args);
    }

    /** Panel routes typed subcommands here; it has no console dispatch or arbitrary command field. */
    public boolean execute(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (!sender.hasPermission("fantasyadmin.view")) {
            m.send(sender, "common.no-permission");
            return true;
        }
        String sub = args.length == 0 && sender instanceof Player ? "panel" : args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "panel", "player" -> {
                if (sender instanceof Player player) {
                    input.cancel(player);
                    var menu = new AdminPanelMenu(player.getUniqueId(), services, this);
                    if (sub.equals("player")) { menu.showPlayers(player, args.length > 1 ? args[1] : null); }
                    else { menu.open(player); }
                } else { m.send(sender, "admin.help"); }
            }
            case "doctor" -> doctor(sender);
            case "bank" -> bankLook(sender, args);
            case "eco" -> eco(sender, args);
            case "confirm" -> confirm(sender, args);
            case "npc" -> npc(sender, args);
            case "item" -> item(sender, args);
            case "mail" -> mail(sender, args);
            case "exchange", "craft" -> exchange(sender, args);
            case "repair" -> repair(sender, args);
            case "dungeon" -> dungeon(sender, args);
            case "audit" -> audit(sender, args);
            default -> m.send(sender, "admin.help");
        }
        return true;
    }

    // ------------------------------------------------------------ doctor

    private void doctor(CommandSender sender) {
        Messages m = services.messages();
        m.send(sender, "admin.doctor.header", Messages.p("version", services.plugin().getPluginMeta().getVersion()));
        long started = System.nanoTime();
        services.tasks().then(services.database().async(() -> services.database().read(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT 1")) {
                return rs.next();
            }
        })), (ok, error) -> {
            long ms = (System.nanoTime() - started) / 1_000_000;
            line(sender, error == null && Boolean.TRUE.equals(ok) ? "ready" : "broken", "ฐานข้อมูล SQLite",
                    error == null ? "ตอบใน " + ms + " ms" : String.valueOf(error.getMessage()));
            doctorRest(sender);
        });
    }

    private void doctorRest(CommandSender sender) {
        var itemsCore = Bukkit.getPluginManager().getPlugin("ItemsCore");
        line(sender, itemsCore == null ? "missing" : itemsCore.isEnabled() ? "fix" : "off", "ItemsCore (ทดลองเสริม)",
                itemsCore == null ? "ยังไม่มี JAR — ชุด import และแผนทดสอบอยู่ใน server/content/itemscore"
                        : "v" + itemsCore.getPluginMeta().getVersion() + " · ยังไม่เปิด provider ของ craft/repair; ตรวจคู่มือ ITEMSCORE-INTEGRATION");
        line(sender, services.craft().enabled() ? "ready" : "off", "คราฟต์อุปกรณ์รูน",
                services.craft().recipes().size() + " สูตร · crafting.yml · /craft");
        services.craft().problems().forEach(p -> line(sender, "fix", "crafting.yml", p));
        line(sender, services.alchemy().enabled() ? "ready" : "off", "ร้านยาไลรา",
                services.alchemy().recipes().size() + " สูตร · alchemy.yml · /alchemy · journalร่วม /fa craft review");
        services.alchemy().problems().forEach(p -> line(sender, "fix", "alchemy.yml", p));
        services.tasks().then(services.database().async(() -> services.craft().store().countReview()), (count, error) -> {
            if (error != null) { line(sender, "broken", "คราฟต์ค้างตรวจ", error.getMessage()); }
            else { line(sender, count == 0 ? "ready" : "fix", "คราฟต์ค้างตรวจ", count + " รายการ · /fa craft review"); }
        });
        boolean vaultPlugin = Bukkit.getPluginManager().isPluginEnabled("Vault")
                || Bukkit.getPluginManager().isPluginEnabled("VaultUnlocked");
        if (!vaultPlugin || !services.plugin().isVaultHooked()) {
            line(sender, "missing", "Vault economy bridge", "ไม่พบ Vault/VaultUnlocked — ปลั๊กอินอื่นจ่ายเงินผ่าน Vault ไม่ได้");
        } else if (VaultHook.isOurs()) {
            line(sender, "ready", "Vault economy bridge", "provider = FantasyCore (gold.wallet)");
        } else {
            line(sender, "fix", "Vault economy bridge", "provider ที่ใช้อยู่คือ " + VaultHook.activeProviderName()
                    + " — ปิด economy ของปลั๊กอินอื่น (ห้ามมี wallet สองที่)");
        }
        line(sender, services.claims().available() ? "ready" : "missing", "ระบบที่ดิน", services.claims().name());
        line(sender, Bukkit.getPluginManager().isPluginEnabled("ProtectionStones") ? "ready" : "missing",
                "ProtectionStones", Bukkit.getPluginManager().isPluginEnabled("ProtectionStones") ? "เปิดอยู่" : "ไม่ได้ติดตั้ง/โหลดไม่สำเร็จ");
        line(sender, Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "ready" : "missing",
                "PlaceholderAPI", "%fantasycore_gold% / _bank / _red");
        line(sender, Bukkit.getPluginManager().isPluginEnabled("ViaVersion") ? "ready" : "missing",
                "ViaVersion", Bukkit.getPluginManager().isPluginEnabled("ViaBackwards") ? "+ ViaBackwards" : "ไม่มี ViaBackwards (client เก่าเข้าไม่ได้)");
        if (services.citizens().isPresent()) {
            line(sender, "ready", "Citizens", "ผูก NPC กับบริการได้ด้วย /fa npc bind <action>");
        } else if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            line(sender, "fix", "Citizens", "เปิดอยู่แต่ API ไม่ตรงรุ่นที่รองรับ — ดู log ตอนเปิดเซิร์ฟ");
        } else {
            line(sender, "off", "Citizens", "ไม่บังคับ — ใช้ NPC ของ Core ได้");
        }
        line(sender, services.rewards().enabled() && services.rewards().problems().isEmpty() ? "ready" : "fix",
                "รางวัลรายวัน", services.rewards().cycle().size() + " ครั้ง/รอบ · วันนี้ " + com.armzofficial.fantasycore.reward.RewardService.period()
                        + " (เวลาไทย)");
        for (String problem : services.rewards().problems()) {
            line(sender, "fix", "rewards.daily", problem);
        }
        line(sender, services.exchange().enabled() ? "ready" : "off", "เควสแลกของ",
                services.exchange().recipes().size() + " สูตร — /exchange");
        boolean saving = PlayerDataSaving.enabled();
        line(sender, saving ? "ready" : "broken", "Player data saving",
                saving ? "เปิดการบันทึก — ต้องเฝ้า disk/log ด้วย" : "players.disable-saving = true หรือ API อ่านไม่ได้; แลกของและรับ mail ไม่ได้");
        for (String problem : services.exchange().problems()) {
            line(sender, "fix", "exchanges.yml", problem);
        }
        line(sender, services.repair().enabled() ? "ready" : "off", "ซ่อมอุปกรณ์", "vanilla + Core serial — /repair ที่สถานี repair.main");
        for (String problem : services.repair().problems()) {
            line(sender, "fix", "repair.yml", problem);
        }
        services.tasks().then(services.database().async(() -> services.repair().store().countReview()), (count, error) -> {
            line(sender, error != null ? "broken" : count == 0 ? "ready" : "fix", "Repair journal",
                    error != null ? "อ่านรายการค้างไม่ได้" : count + " รายการรอตรวจ — /fa repair review");
        });
        services.tasks().then(services.database().async(() -> services.exchange().store().countReview()), (count, error) -> {
            if (error != null) {
                line(sender, "broken", "Exchange journal", "อ่านรายการค้างไม่ได้");
            } else {
                line(sender, count == 0 ? "ready" : "fix", "Exchange journal", count + " รายการรอตรวจ — /fa exchange review");
            }
        });
        for (var spec : services.settings().worlds().values()) {
            World world = Bukkit.getWorld(spec.name());
            line(sender, world != null ? "ready" : "broken", "โลก " + spec.name(), world == null ? "ไม่ได้โหลด"
                    : "border " + Money.format((long) world.getWorldBorder().getSize()) + " บล็อก");
        }
        for (String problem : services.worlds().problems()) {
            line(sender, "fix", "World", problem);
        }
        for (String problem : services.items().problems()) {
            line(sender, "fix", "items.yml", problem);
        }
        line(sender, services.monsters().enabled() ? "ready" : "fix", "Depth monsters", services.monsters().status());
        line(sender, services.dungeon().ready() ? "ready" : "off", "Moonfall solo + party", services.dungeon().status());
        services.dungeon().instanceStatus().forEach(detail -> services.messages().send(sender,"dungeon.admin-report",Messages.p("detail",detail)));
        services.dungeon().problems().forEach(problem -> line(sender,"fix","dungeons.yml",problem));
        services.monsters().problems().forEach(problem -> line(sender, "fix", "monsters.yml", problem));
        for (String key : services.messages().missingKeys()) {
            line(sender, "fix", "messages_th.yml", "ขาด key " + key);
        }
        line(sender, "ready", "จุดบริการ", services.stations().records().size() + " จุด (radius "
                + services.stations().radius() + ")");
        services.tasks().then(services.database().async(() -> services.mail().store().countReview()), (count, error) -> {
            if (error == null) {
                line(sender, count == 0 ? "ready" : "fix", "กล่องจดหมาย",
                        count == 0 ? "ไม่มีรายการค้างตรวจ" : count + " รายการรอทีมงานตัดสิน — /fa mail review");
            }
        });
    }

    private void dungeon(CommandSender sender,String[] args) {
        Messages m=services.messages();
        String sub=args.length>=2?args[1].toLowerCase(Locale.ROOT):"status";
        if(sub.equals("status")) {
            m.send(sender,"dungeon.admin-report",Messages.p("detail",services.dungeon().status()));
            services.dungeon().instanceStatus().forEach(detail -> m.send(sender,"dungeon.admin-report",Messages.p("detail",detail))); return;
        }
        if(!sender.hasPermission("fantasyadmin.dungeon.manage")) { m.send(sender,"common.no-permission"); return; }
        if(sub.equals("visit") && sender instanceof Player player) {
            var slot=InstanceSlot.parse(args.length>=3?args[2]:"training");
            if(slot.isEmpty()) { m.send(sender,"dungeon.admin-usage"); return; }
            services.dungeon().visit(player,slot.get()); return;
        }
        boolean build=sub.equals("build") || sub.equals("buildparty");
        int reasonStart=sub.equals("buildparty")?3:2;
        if((build || sub.equals("abort")) && args.length>reasonStart) {
            InstanceSlot slot=InstanceSlot.TRAINING;
            if(sub.equals("buildparty")) {
                if(!List.of("1","2").contains(args[2])) { m.send(sender,"dungeon.admin-usage"); return; }
                slot=args[2].equals("1")?InstanceSlot.PARTY1:InstanceSlot.PARTY2;
            }
            final InstanceSlot selected=slot;
            String reason=String.join(" ",Arrays.copyOfRange(args,reasonStart,args.length)).trim();
            if(reason.length()<3 || reason.length()>200) { m.send(sender,"admin.reason-required"); return; }
            UUID actor=sender instanceof Player p?p.getUniqueId():null;
            String target=build?selected.key():"ALL ACTIVE INSTANCES";
            String token=confirmations.create(actor,() -> {
                if(!sender.hasPermission("fantasyadmin.dungeon.manage")) { m.send(sender,"common.no-permission"); return; }
                AuditEntry entry=new AuditEntry(actor==null?null:actor.toString(),sender.getName(),"dungeon.admin."+sub,target,services.dungeon().status(),reason);
                services.tasks().then(services.database().async(() -> { services.audit().record(entry,OpMeta.newOpId()); return null; }),(ignored,error) -> {
                    if(error!=null) { m.send(sender,"dungeon.storage-error"); return; }
                    if(build) { services.dungeon().build(sender,selected); } else { services.dungeon().abort(sender); }
                });
            });
            m.send(sender,"dungeon.admin-preview",Messages.p("action",sub),Messages.p("target",target),Messages.p("reason",reason),Messages.p("token",token)); return;
        }
        m.send(sender,"dungeon.admin-usage");
    }

    private void line(CommandSender sender, String status, String name, String detail) {
        Messages m = services.messages();
        sender.sendMessage(m.plain("admin.doctor.line", Messages.c("status", m.plain("admin.status." + status)),
                Messages.p("name", name), Messages.p("detail", detail == null ? "" : detail)));
    }

    // ------------------------------------------------------------ economy

    private void bankLook(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (args.length < 2) {
            m.send(sender, "admin.usage.bank");
            return;
        }
        resolve(sender, args[1], known -> services.tasks().then(services.economy().balances(known.uuid()), (balances, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
                return;
            }
            m.send(sender, "admin.bank.summary", Messages.p("player", known.name()), Messages.p("uuid", known.uuid()),
                    Messages.p("wallet", Money.format(balances.gold())), Messages.p("bank", Money.format(balances.bank())),
                    Messages.p("redcoin", Money.format(balances.red())));
            BankHistory.show(services, sender, known.uuid(), known.name());
        }));
    }

    private void eco(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (!canAdjust(sender)) {
            m.send(sender, "common.no-permission");
            return;
        }
        if (args.length < 6 || !(args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take"))) {
            m.send(sender, "admin.usage.eco");
            return;
        }
        boolean give = args[1].equalsIgnoreCase("give");
        Optional<Bucket> bucket = Bucket.parse(args[3]);
        OptionalLong amount = Money.parsePositive(args[4]);
        String reason = String.join(" ", Arrays.copyOfRange(args, 5, args.length)).trim();
        if (bucket.isEmpty() || amount.isEmpty()) {
            m.send(sender, "admin.usage.eco");
            return;
        }
        if (reason.length() < 3 || reason.length() > 200 || reason.codePoints().anyMatch(Character::isISOControl)) {
            m.send(sender, "admin.reason-required");
            return;
        }
        long delta = give ? amount.getAsLong() : -amount.getAsLong();
        resolve(sender, args[2], known -> previewAdjust(sender, known, bucket.get(), delta, reason, null));
    }

    public record EconomyPreview(PlayerStore.Known target, Bucket bucket, long delta, long before, long after,
                                 String reason, String token) { }

    static boolean canAdjust(CommandSender sender) {
        return sender.hasPermission("fantasyadmin.view") && sender.hasPermission("fantasyadmin.economy.adjust");
    }

    public void previewEconomy(Player actor, PlayerStore.Known target, Bucket bucket, long delta, String reason,
                               Consumer<EconomyPreview> ready) {
        previewAdjust(actor, target, bucket, delta, reason, ready);
    }

    private void previewAdjust(CommandSender sender, PlayerStore.Known known, Bucket bucket, long delta, String reason,
                               Consumer<EconomyPreview> ready) {
        Messages m = services.messages();
        if (!canAdjust(sender)) { m.send(sender, "common.no-permission"); failPreview(ready); return; }
        if (delta == 0 || delta == Long.MIN_VALUE || Math.abs(delta) > services.settings().maxTransaction()) {
            m.send(sender, "admin.eco.limit"); failPreview(ready); return;
        }
        if (reason == null || reason.length() < 3 || reason.length() > 200 || reason.codePoints().anyMatch(Character::isISOControl)) {
            m.send(sender, "admin.reason-required"); failPreview(ready); return;
        }
        services.tasks().then(services.economy().balances(known.uuid()), (balances, error) -> {
            if (!canAdjust(sender)) { m.send(sender, "common.no-permission"); failPreview(ready); return; }
            if (error != null) {
                m.send(sender, "common.storage-error");
                failPreview(ready);
                return;
            }
            long before = balances.get(bucket);
            long after;
            try { after = Math.addExact(before, delta); }
            catch (ArithmeticException e) { m.send(sender, "admin.eco.limit"); failPreview(ready); return; }
            if (after < 0) {
                m.send(sender, "admin.eco.would-be-negative", Messages.p("before", Money.format(before)));
                failPreview(ready);
                return;
            }
            String opId = OpMeta.newOpId();
            UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
            AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(), "economy.adjust",
                    known.uuid().toString(), bucket.key() + " " + (delta > 0 ? "+" : "") + delta + " (" + before + " → " + after + ")",
                    reason);
            String token = confirmations.create(actor, () -> canAdjust(sender), () -> applyAdjust(sender, known, bucket, delta, before, opId, audit));
            if (ready != null) { ready.accept(new EconomyPreview(known, bucket, delta, before, after, reason, token)); return; }
            m.send(sender, "admin.eco.preview", Messages.p("player", known.name()), Messages.p("bucket", bucket.key()),
                    Messages.p("delta", (delta > 0 ? "+" : "") + Money.format(delta)), Messages.p("before", Money.format(before)),
                    Messages.p("after", Money.format(after)), Messages.p("reason", reason), Messages.p("token", token));
        });
    }

    public void cancelPreview(Player actor, String token) { confirmations.cancel(token, actor.getUniqueId()); }
    private static void failPreview(Consumer<EconomyPreview> ready) { if (ready != null) { ready.accept(null); } }

    private void applyAdjust(CommandSender sender, PlayerStore.Known known, Bucket bucket, long delta, long before,
                             String opId, AuditEntry audit) {
        Messages m = services.messages();
        if (!canAdjust(sender)) { m.send(sender, "common.no-permission"); return; }
        services.tasks().then(services.economy().adminAdjust(known.uuid(), bucket, delta, before, opId, audit), (result, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
                return;
            }
            switch (result.status()) {
                case OK -> m.send(sender, "admin.eco.applied", Messages.p("player", known.name()),
                        Messages.p("bucket", bucket.key()), Messages.p("after", Money.format(result.after().get(bucket))),
                        Messages.p("op", opId));
                case BALANCE_CHANGED -> m.send(sender, "admin.eco.changed");
                case DUPLICATE -> m.send(sender, "admin.eco.duplicate", Messages.p("op", opId));
                default -> m.send(sender, "admin.eco.failed", Messages.p("status", result.status().name()));
            }
            Player target = Bukkit.getPlayer(known.uuid());
            if (target != null && result.status() == TxResult.Status.OK) {
                m.send(target, "economy.adjusted-by-staff", Messages.p("bucket", bucket.key()),
                        Messages.p("delta", (delta > 0 ? "+" : "") + Money.format(delta)));
            }
        });
    }

    private void confirm(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (args.length < 2) {
            m.send(sender, "admin.usage.confirm");
            return;
        }
        UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
        switch (confirmations.confirm(args[1], actor)) {
            case APPLIED -> m.send(sender, "admin.confirm.applying");
            case EXPIRED -> m.send(sender, "admin.confirm.expired");
            case WRONG_ACTOR -> m.send(sender, "admin.confirm.wrong-actor");
            case UNKNOWN -> m.send(sender, "admin.confirm.unknown");
            case DENIED -> m.send(sender, "common.no-permission");
        }
    }

    // ------------------------------------------------------------ stations

    private void npc(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (!sender.hasPermission("fantasyadmin.npc.edit")) {
            m.send(sender, "common.no-permission");
            return;
        }
        String sub = args.length < 2 ? "list" : args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            List<StationRecord> records = services.stations().records();
            m.send(sender, "admin.npc.list-header", Messages.p("count", records.size()));
            for (StationRecord r : records) {
                sender.sendMessage(m.plain("admin.npc.list-line", Messages.p("kind", r.kind().name()),
                        Messages.p("action", r.actionId()), Messages.p("world", r.worldName()),
                        Messages.p("coords", (int) r.x() + ", " + (int) r.y() + ", " + (int) r.z()),
                        Messages.p("id", r.id().toString().substring(0, 8))));
            }
            return;
        }
        if (!(sender instanceof Player player)) {
            m.send(sender, "common.players-only");
            return;
        }
        switch (sub) {
            case "spawn", "anchor" -> {
                if (args.length < 3) {
                    m.send(sender, "admin.usage.npc");
                    return;
                }
                String action = args[2].toLowerCase(Locale.ROOT);
                if (!ActionRegistry.ID_FORMAT.matcher(action).matches() || !services.actions().isKnown(action)) {
                    m.send(sender, "admin.npc.unknown-action", Messages.p("action", action),
                            Messages.p("known", String.join(", ", services.actions().knownIds())));
                    return;
                }
                CompletableFuture<StationRecord> future;
                if (sub.equals("spawn")) {
                    String labelText = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : action;
                    Component label = MiniMessage.miniMessage().deserialize(labelText);
                    future = services.stations().spawnNpc(player, action, label, labelText);
                } else {
                    future = services.stations().addAnchor(player, action);
                }
                services.tasks().then(future, (record, error) -> {
                    if (error instanceof IllegalStateException blocked) {
                        m.send(sender, "admin.npc.spawn-blocked", Messages.p("detail", blocked.getMessage()));
                        return;
                    }
                    if (error != null) {
                        m.send(sender, "common.storage-error");
                        return;
                    }
                    m.send(sender, sub.equals("spawn") ? "admin.npc.spawned" : "admin.npc.anchored",
                            Messages.p("action", action), Messages.p("id", record.id().toString().substring(0, 8)),
                            Messages.p("connected", services.actions().isImplemented(action) ? "พร้อมใช้" : "ยังไม่เชื่อมระบบ"));
                });
            }
            case "remove" -> {
                Entity target = player.getTargetEntity(6);
                if (target == null || services.stations().stationIdOf(target).isEmpty()) {
                    m.send(sender, "admin.npc.no-target");
                    return;
                }
                services.tasks().then(services.stations().removeNpc(player, target), (ok, error) ->
                        m.send(sender, error == null && Boolean.TRUE.equals(ok) ? "admin.npc.removed" : "common.storage-error"));
            }
            case "bind" -> {
                if (args.length < 3) {
                    m.send(sender, "admin.usage.npc");
                    return;
                }
                String action = args[2].toLowerCase(Locale.ROOT);
                if (!ActionRegistry.ID_FORMAT.matcher(action).matches() || !services.actions().isKnown(action)) {
                    m.send(sender, "admin.npc.unknown-action", Messages.p("action", action),
                            Messages.p("known", String.join(", ", services.actions().knownIds())));
                    return;
                }
                if (services.citizens().isEmpty()) {
                    m.send(sender, "admin.npc.no-citizens");
                    return;
                }
                Entity target = player.getTargetEntity(6);
                var npc = services.citizens().get().npcOf(target);
                if (npc.isEmpty()) {
                    m.send(sender, "admin.npc.not-citizens");
                    return;
                }
                services.tasks().then(services.stations().bindCitizens(player, target, npc.get().uuid(), npc.get().name(), action),
                        (record, error) -> {
                            if (error != null) {
                                m.send(sender, "common.storage-error");
                                return;
                            }
                            m.send(sender, "admin.npc.bound", Messages.p("npc", npc.get().name()),
                                    Messages.p("npcid", npc.get().id()), Messages.p("action", action),
                                    Messages.p("connected", services.actions().isImplemented(action) ? "พร้อมใช้" : "ยังไม่เชื่อมระบบ"));
                        });
            }
            case "unbind" -> {
                if (services.citizens().isEmpty()) {
                    m.send(sender, "admin.npc.no-citizens");
                    return;
                }
                var npc = services.citizens().get().npcOf(player.getTargetEntity(6));
                if (npc.isEmpty()) {
                    m.send(sender, "admin.npc.not-citizens");
                    return;
                }
                services.tasks().then(services.stations().unbindCitizens(player, npc.get().uuid()), (ok, error) ->
                        m.send(sender, error != null ? "common.storage-error"
                                : Boolean.TRUE.equals(ok) ? "admin.npc.unbound" : "admin.npc.not-bound",
                                Messages.p("npc", npc.get().name())));
            }
            case "unanchor" -> services.tasks().then(services.stations().removeNearestAnchor(player), (ok, error) ->
                    m.send(sender, error != null ? "common.storage-error"
                            : Boolean.TRUE.equals(ok) ? "admin.npc.unanchored" : "admin.npc.no-anchor"));
            default -> m.send(sender, "admin.usage.npc");
        }
    }

    // ------------------------------------------------------------ items

    private void item(CommandSender sender, String[] args) {
        Messages m = services.messages();
        ItemTemplateService items = services.items();
        String sub = args.length < 2 ? "list" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                m.send(sender, "admin.item.list-header", Messages.p("count", items.templates().size()));
                for (ItemTemplate t : items.templates().values()) {
                    sender.sendMessage(m.plain("admin.item.list-line", Messages.p("id", t.id()),
                            Messages.p("version", t.version()), Messages.p("material", t.material().name()),
                            Messages.p("enchants", t.describeEnchantments())));
                }
            }
            case "inspect" -> {
                if (!(sender instanceof Player player)) {
                    m.send(sender, "common.players-only");
                    return;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                Optional<ItemTemplateService.Identity> identity = items.identify(held);
                if (identity.isEmpty()) {
                    m.send(sender, "admin.item.not-core");
                    return;
                }
                ItemTemplateService.Identity id = identity.get();
                if (id.serial() == null) {
                    m.send(sender, "admin.item.identity", Messages.p("template", id.templateId()),
                            Messages.p("version", id.version()), Messages.p("serial", "-"), Messages.p("state", "ไม่มี serial"));
                    return;
                }
                services.tasks().then(services.database().async(() -> services.itemInstances().find(id.serial())),
                        (instance, error) -> m.send(sender, "admin.item.identity", Messages.p("template", id.templateId()),
                                Messages.p("version", id.version()), Messages.p("serial", id.serial()),
                                Messages.p("state", error != null ? "อ่านไม่ได้" : instance.map(i -> i.state() + " โดย " + i.issuedBy())
                                        .orElse("ไม่พบในทะเบียน — ต้องตรวจสอบ"))));
            }
            case "give" -> {
                if (!sender.hasPermission("fantasyadmin.content.edit")) {
                    m.send(sender, "common.no-permission");
                    return;
                }
                if (args.length < 4) {
                    m.send(sender, "admin.usage.item");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                Optional<ItemTemplate> template = items.template(args[3]);
                if (target == null) {
                    m.send(sender, "admin.player-offline", Messages.p("player", args[2]));
                    return;
                }
                if (template.isEmpty()) {
                    m.send(sender, "admin.item.unknown", Messages.p("id", args[3]));
                    return;
                }
                give(sender, target, template.get());
            }
            default -> m.send(sender, "admin.usage.item");
        }
    }

    /** ออกไอเทม: บันทึก serial → ใส่ inventory หรือกล่องจดหมายถ้าเต็ม → อัปเดตสถานะ (ไม่โยนของลงพื้น) */
    private void give(CommandSender sender, Player target, ItemTemplate template) {
        Messages m = services.messages();
        UUID serial = template.serialized() ? UUID.randomUUID() : null;
        String opId = OpMeta.newOpId();
        UUID targetId = target.getUniqueId();
        UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
        AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(), "item.give",
                targetId.toString(), template.id() + " v" + template.version() + (serial == null ? "" : " #" + serial), null);
        long now = System.currentTimeMillis();
        CompletableFuture<Void> issue = serial == null
                ? services.database().async(() -> {
                    services.audit().record(audit, opId);
                    return null;
                })
                : services.database().async(() -> {
                    services.itemInstances().issue(serial, template.id(), template.version(), targetId, opId,
                            sender.getName(), audit, now);
                    return null;
                });
        services.tasks().then(issue, (ignored, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
                return;
            }
            ItemStack stack = services.items().create(template, serial);
            Player online = Bukkit.getPlayer(targetId);
            java.util.function.Consumer<Integer> finish = mailed -> {
                String state = mailed == null || mailed < 0 ? "DELIVERY_FAILED" : mailed > 0 ? "MAILED" : "DELIVERED";
                if (serial != null) {
                    services.tasks().then(services.database().async(() -> {
                        services.itemInstances().setState(serial, state, System.currentTimeMillis());
                        return null;
                    }), (x, e) -> {
                    });
                }
                String key = switch (state) {
                    case "DELIVERED" -> "admin.item.given";
                    case "MAILED" -> "admin.item.mailed";
                    default -> "admin.item.delivery-failed";
                };
                m.send(sender, key, Messages.p("player", target.getName()), Messages.p("id", template.id()),
                        Messages.p("serial", serial == null ? "-" : serial.toString().substring(0, 8)));
            };
            if (online == null) {
                services.mail().mailToPlayer(targetId, List.of(stack), "admin.item", opId, null, null, finish);
            } else {
                services.mail().deliverOrMail(online, List.of(stack), "admin.item", opId, null, null, finish);
            }
        });
    }

    // ------------------------------------------------------------ mail

    private void mail(CommandSender sender, String[] args) {
        Messages m = services.messages();
        String sub = args.length < 2 ? "review" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "review" -> services.tasks().then(services.database().async(() -> services.mail().store().review(20)),
                    (rows, error) -> {
                        if (error != null) {
                            m.send(sender, "common.storage-error");
                            return;
                        }
                        m.send(sender, "admin.mail.review-header", Messages.p("count", rows.size()));
                        for (var row : rows) {
                            sender.sendMessage(m.plain("admin.mail.review-line", Messages.p("id", row.id()),
                                    Messages.p("player", row.player()), Messages.p("item", row.label()),
                                    Messages.p("source", row.source()),
                                    Messages.p("time", TIME.format(Instant.ofEpochMilli(row.createdAt())))));
                        }
                    });
            case "release", "void" -> {
                if (!sender.hasPermission("fantasyadmin.economy.adjust")) {
                    m.send(sender, "common.no-permission");
                    return;
                }
                if (args.length < 4) {
                    m.send(sender, "admin.usage.mail");
                    return;
                }
                long id;
                try {
                    id = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    m.send(sender, "admin.usage.mail");
                    return;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
                if (reason.length() < 3) {
                    m.send(sender, "admin.reason-required");
                    return;
                }
                boolean release = sub.equals("release");
                UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
                AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(),
                        release ? "mail.release" : "mail.void", "mail#" + id, null, reason);
                services.tasks().then(services.database().async(() -> services.mail().store().resolveReview(id, release, audit)),
                        (ok, error) -> m.send(sender, error != null ? "common.storage-error"
                                : Boolean.TRUE.equals(ok) ? (release ? "admin.mail.released" : "admin.mail.voided")
                                : "admin.mail.not-review", Messages.p("id", id)));
            }
            case "give" -> {
                if (!sender.hasPermission("fantasyadmin.content.edit")) {
                    m.send(sender, "common.no-permission");
                    return;
                }
                if (!(sender instanceof Player player)) {
                    m.send(sender, "common.players-only");
                    return;
                }
                if (args.length < 4) {
                    m.send(sender, "admin.usage.mail");
                    return;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.isEmpty()) {
                    m.send(sender, "admin.mail.empty-hand");
                    return;
                }
                if (services.items().identify(held).map(i -> i.serial() != null).orElse(false)) {
                    // ของที่มี serial ต้องออกผ่าน /fa item give เพื่อไม่ให้ serial ซ้ำ
                    m.send(sender, "admin.mail.serialized");
                    return;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
                if (reason.length() < 3) {
                    m.send(sender, "admin.reason-required");
                    return;
                }
                ItemStack copy = held.clone();
                resolve(sender, args[2], known -> {
                    String opId = OpMeta.newOpId();
                    AuditEntry audit = new AuditEntry(player.getUniqueId().toString(), player.getName(), "mail.give",
                            known.uuid().toString(), com.armzofficial.fantasycore.mail.MailService.describe(copy), reason);
                    services.mail().mailToPlayer(known.uuid(), List.of(copy), "admin.mail", opId, null, audit, count ->
                            m.send(sender, count != null && count > 0 ? "admin.mail.sent" : "common.storage-error",
                                    Messages.p("player", known.name())));
                });
            }
            default -> m.send(sender, "admin.usage.mail");
        }
    }

    // ------------------------------------------------------------ exchange recovery

    private void exchange(CommandSender sender, String[] args) {
        Messages m = services.messages();
        String group = args[0].equalsIgnoreCase("craft") ? "craft" : "exchange";
        String prefix = "admin." + group + ".";
        String permission = "fantasyadmin." + group + ".resolve";
        ExchangeService exchange = group.equals("craft") ? services.craft() : services.exchange();
        String sub = args.length < 2 ? "review" : args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("review")) {
            services.tasks().then(services.database().async(() -> exchange.store().review(20)), (rows, error) -> {
                if (error != null) {
                    m.send(sender, "common.storage-error");
                    return;
                }
                m.send(sender, prefix + "header", Messages.p("count", rows.size()));
                for (var row : rows) {
                    m.send(sender, prefix + "line", Messages.p("op", row.opId()), Messages.p("player", row.player()), Messages.p("price", row.goldCost()),
                            Messages.p("outputs", row.outputs()),
                            Messages.p("recipe", row.recipe()), Messages.p("version", row.version()), Messages.p("batch", row.batch()),
                            Messages.p("inputs", row.inputs()), Messages.p("period", row.period()));
                }
            });
            return;
        }
        if (!sub.equals("complete") && !sub.equals("cancel")) {
            m.send(sender, "admin.usage." + group);
            return;
        }
        if (!sender.hasPermission(permission)) {
            m.send(sender, "common.no-permission");
            return;
        }
        if (args.length < 4) {
            m.send(sender, "admin.usage." + group);
            return;
        }
        String opId = args[2];
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
        if (reason.length() < 3) {
            m.send(sender, "admin.reason-required");
            return;
        }
        boolean complete = sub.equals("complete");
        UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
        services.tasks().then(services.database().async(() -> exchange.store().findReview(opId)), (row, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
            } else if (row.isEmpty()) {
                m.send(sender, prefix + "not-review", Messages.p("op", opId));
            } else {
                var review = row.get();
                AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(),
                        (group.equals("craft") ? "craft." : "quest.exchange.") + (complete ? "resolve_complete" : "resolve_cancel"), review.player().toString(),
                        review.recipe() + " v" + review.version() + " ×" + review.batch() + " · " + review.inputs() + " · gold " + review.goldCost(), reason);
                String token = confirmations.create(actor, () -> {
                    if (!sender.hasPermission(permission)) {
                        m.send(sender, "common.no-permission");
                        return;
                    }
                    services.tasks().then(services.database().async(() -> exchange.store().resolveReview(opId, complete, audit)),
                            (result, resolveError) -> {
                                services.economy().load(review.player());
                                m.send(sender, resolveError != null ? "common.storage-error"
                                    : prefix + (result.changed() ? "resolved" : "not-review"), Messages.p("op", opId));
                            });
                });
                m.send(sender, prefix + "line", Messages.p("op", opId), Messages.p("player", review.player()), Messages.p("price", review.goldCost()),
                        Messages.p("outputs", review.outputs()),
                        Messages.p("recipe", review.recipe()), Messages.p("version", review.version()), Messages.p("batch", review.batch()),
                        Messages.p("inputs", review.inputs()), Messages.p("period", review.period()));
                m.send(sender, prefix + "preview", Messages.p("op", opId), Messages.p("reason", reason), Messages.p("token", token),
                        Messages.c("effect", m.plain(prefix + (complete ? "complete-effect" : "cancel-effect"))));
            }
        });
    }

    // ------------------------------------------------------------ audit

    private void repair(CommandSender sender, String[] args) {
        Messages m = services.messages();
        String sub = args.length < 2 ? "review" : args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("review")) {
            services.tasks().then(services.database().async(() -> services.repair().store().review(20)), (rows, error) -> {
                if (error != null) { m.send(sender, "common.storage-error"); return; }
                m.send(sender, "admin.repair.header", Messages.p("count", rows.size()));
                for (var row : rows) { repairLine(sender, row); }
            });
            return;
        }
        if (!sub.equals("complete") && !sub.equals("cancel")) { m.send(sender, "admin.usage.repair"); return; }
        if (!sender.hasPermission("fantasyadmin.repair.resolve")) { m.send(sender, "common.no-permission"); return; }
        if (args.length < 4) { m.send(sender, "admin.usage.repair"); return; }
        String opId = args[2];
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
        if (reason.length() < 3) { m.send(sender, "admin.reason-required"); return; }
        boolean complete = sub.equals("complete");
        UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
        services.tasks().then(services.database().async(() -> services.repair().store().findReview(opId)), (row, error) -> {
            if (error != null) { m.send(sender, "common.storage-error"); }
            else if (row.isEmpty()) { m.send(sender, "admin.repair.not-review", Messages.p("op", opId)); }
            else {
                var review = row.get();
                AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(),
                        complete ? "item.repair.resolve_complete" : "item.repair.resolve_cancel", review.player().toString(),
                        review.material() + " damage " + review.damage() + "/" + review.maxDamage() + " cost " + review.price(), reason);
                String token = confirmations.create(actor, () -> {
                    if (!sender.hasPermission("fantasyadmin.repair.resolve")) { m.send(sender, "common.no-permission"); return; }
                    services.tasks().then(services.database().async(() -> services.repair().store().resolveReview(opId, complete, audit)),
                            (resolved, resolveError) -> {
                                m.send(sender, resolveError != null ? "common.storage-error" : Boolean.TRUE.equals(resolved)
                                        ? "admin.repair.resolved" : "admin.repair.not-review", Messages.p("op", opId));
                                if (resolveError == null && Boolean.TRUE.equals(resolved) && Bukkit.getPlayer(review.player()) != null) {
                                    services.economy().load(review.player());
                                }
                            });
                });
                repairLine(sender, review);
                m.send(sender, "admin.repair.preview", Messages.p("reason", reason), Messages.p("token", token),
                        Messages.c("effect", m.plain(complete ? "admin.repair.complete-effect" : "admin.repair.cancel-effect")));
            }
        });
    }

    private void repairLine(CommandSender sender, com.armzofficial.fantasycore.repair.RepairStore.Review row) {
        services.messages().send(sender, "admin.repair.line", Messages.p("op", row.opId()), Messages.p("player", row.player()),
                Messages.p("material", row.material()), Messages.p("damage", row.damage()), Messages.p("max", row.maxDamage()),
                Messages.p("price", Money.format(row.price())), Messages.p("serial", row.serial() == null ? "vanilla" : row.serial()));
    }

    private void audit(CommandSender sender, String[] args) {
        Messages m = services.messages();
        if (!sender.hasPermission("fantasyadmin.audit")) {
            m.send(sender, "common.no-permission");
            return;
        }
        if (args.length >= 2) {
            resolve(sender, args[1], known -> showAudit(sender, known.uuid().toString(), known.name()));
        } else {
            showAudit(sender, null, "ทั้งหมด");
        }
    }

    private void showAudit(CommandSender sender, String target, String label) {
        Messages m = services.messages();
        services.tasks().then(services.database().async(() -> services.audit().recent(target, 10)), (rows, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
                return;
            }
            m.send(sender, "admin.audit.header", Messages.p("target", label), Messages.p("count", rows.size()));
            for (AuditStore.Row row : rows) {
                sender.sendMessage(m.plain("admin.audit.line", Messages.p("time", TIME.format(Instant.ofEpochMilli(row.createdAt()))),
                        Messages.p("actor", row.actorName()), Messages.p("action", row.action()),
                        Messages.p("detail", row.detail() == null ? "" : row.detail()),
                        Messages.p("reason", row.reason() == null ? "-" : row.reason())));
            }
        });
    }

    // ------------------------------------------------------------ helpers

    /** หา UUID จากชื่อ: ออนไลน์ก่อน แล้วจึงทะเบียนผู้เล่นของ Core (ไม่สร้างบัญชีจากชื่อที่พิมพ์เฉย ๆ) */
    public void resolve(CommandSender sender, String name, Consumer<PlayerStore.Known> then) {
        if (!sender.hasPermission("fantasyadmin.view")) { services.messages().send(sender, "common.no-permission"); return; }
        UUID uuid;
        try { uuid = UUID.fromString(name); } catch (IllegalArgumentException e) { uuid = null; }
        final UUID wanted = uuid;
        if (wanted != null) {
            Player byId = Bukkit.getPlayer(wanted);
            if (byId != null) { then.accept(new PlayerStore.Known(byId.getUniqueId(), byId.getName())); return; }
            services.tasks().then(services.database().async(() -> services.players().findByUuid(wanted)), (known, error) -> {
                if (!sender.hasPermission("fantasyadmin.view")) { services.messages().send(sender, "common.no-permission"); return; }
                if (error != null) { services.messages().send(sender, "common.storage-error"); }
                else if (known.isEmpty()) { services.messages().send(sender, "admin.player-unknown", Messages.p("player", name)); }
                else { then.accept(known.get()); }
            });
            return;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            then.accept(new PlayerStore.Known(online.getUniqueId(), online.getName()));
            return;
        }
        services.tasks().then(services.database().async(() -> services.players().findByName(name)), (known, error) -> {
            if (!sender.hasPermission("fantasyadmin.view")) { services.messages().send(sender, "common.no-permission"); return; }
            if (error != null) {
                services.messages().send(sender, "common.storage-error");
            } else if (known.isEmpty()) {
                services.messages().send(sender, "admin.player-unknown", Messages.p("player", name));
            } else {
                then.accept(known.get());
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission("fantasyadmin.view")) { return List.of(); }
        if (args.length > 1 && !suggestible(sender, args[0], args.length > 2 ? args[1] : null)) { return List.of(); }
        if (args.length == 1) {
            options.addAll(List.of("panel", "player", "help", "doctor", "bank", "eco", "confirm", "npc", "item", "mail", "exchange", "repair", "craft", "audit", "dungeon"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "eco" -> options.addAll(List.of("give", "take"));
                case "npc" -> options.addAll(List.of("spawn", "bind", "unbind", "anchor", "remove", "unanchor", "list"));
                case "mail" -> options.addAll(List.of("review", "release", "void", "give"));
                case "exchange", "repair", "craft" -> options.addAll(List.of("review", "complete", "cancel"));
                case "item" -> options.addAll(List.of("list", "give", "inspect"));
                case "dungeon" -> options.addAll(List.of("status", "build", "buildparty", "visit", "abort"));
                case "bank", "audit", "player" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "eco", "item" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                case "mail" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    }
                }
                case "npc" -> {
                    if (args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("anchor")
                            || args[1].equalsIgnoreCase("bind")) {
                        options.addAll(services.actions().knownIds());
                    }
                }
                default -> {
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("eco")) {
                options.addAll(List.of("gold", "bank", "red"));
            } else if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
                options.addAll(services.items().templates().keySet());
            }
        }
        if(args.length==3 && args[0].equalsIgnoreCase("dungeon")) {
            if(args[1].equalsIgnoreCase("visit")) { options.addAll(List.of("training","party1","party2")); }
            if(args[1].equalsIgnoreCase("buildparty")) { options.addAll(List.of("1","2")); }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix))
                .filter(o -> args.length == 1 ? suggestible(sender, o, null) : args.length != 2 || suggestible(sender, args[0], o)).toList();
    }

    static boolean suggestible(CommandSender sender, String root, String sub) {
        if (!sender.hasPermission("fantasyadmin.view")) { return false; }
        root = root.toLowerCase(Locale.ROOT);
        String permission = switch (root) {
            case "eco" -> "fantasyadmin.economy.adjust";
            case "npc" -> "fantasyadmin.npc.edit";
            case "audit" -> "fantasyadmin.audit";
            default -> null;
        };
        if (permission != null && !sender.hasPermission(permission)) { return false; }
        if (sub == null) { return true; }
        sub = sub.toLowerCase(Locale.ROOT);
        permission = switch (root) {
            case "item" -> sub.equals("give") ? "fantasyadmin.content.edit" : null;
            case "mail" -> sub.equals("give") ? "fantasyadmin.content.edit" : List.of("release", "void").contains(sub) ? "fantasyadmin.economy.adjust" : null;
            case "exchange", "craft", "repair" -> List.of("complete", "cancel").contains(sub) ? "fantasyadmin."+root+".resolve" : null;
            case "dungeon" -> sub.equals("status") ? null : "fantasyadmin.dungeon.manage";
            default -> null;
        };
        return permission == null || sender.hasPermission(permission);
    }
}
