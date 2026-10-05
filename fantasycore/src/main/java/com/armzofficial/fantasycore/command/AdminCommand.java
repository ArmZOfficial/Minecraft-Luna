package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.hook.VaultHook;
import com.armzofficial.fantasycore.item.ItemTemplate;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import com.armzofficial.fantasycore.menu.BankHistory;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.station.StationRecord;
import com.armzofficial.fantasycore.storage.PlayerStore;
import com.armzofficial.fantasycore.util.Money;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * /fa — เครื่องมือแอดมินฉบับข้อความของ FantasyAdminPanel (GUI เต็มเป็น phase ถัดไป)
 * ทุกงานเขียนต้องมีเหตุผล, preview → /fa confirm &lt;รหัส&gt;, และบันทึก audit พร้อม operation ID
 */
public final class AdminCommand implements TabExecutor {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BankHistory.BANGKOK);

    private final Services services;
    private final PendingConfirmations confirmations = new PendingConfirmations();

    public AdminCommand(Services services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        Messages m = services.messages();
        if (!sender.hasPermission("fantasyadmin.view")) {
            m.send(sender, "common.no-permission");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "doctor" -> doctor(sender);
            case "bank" -> bankLook(sender, args);
            case "eco" -> eco(sender, args);
            case "confirm" -> confirm(sender, args);
            case "npc" -> npc(sender, args);
            case "item" -> item(sender, args);
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
        line(sender, Bukkit.getPluginManager().isPluginEnabled("Citizens") ? "ready" : "off",
                "Citizens", "ไม่บังคับใน v0.1 — ใช้ NPC ของ Core ได้");
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
        for (String key : services.messages().missingKeys()) {
            line(sender, "fix", "messages_th.yml", "ขาด key " + key);
        }
        line(sender, "ready", "จุดบริการ", services.stations().records().size() + " จุด (radius "
                + services.stations().radius() + ")");
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
        if (!sender.hasPermission("fantasyadmin.economy.adjust")) {
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
        if (reason.length() < 3) {
            m.send(sender, "admin.reason-required");
            return;
        }
        long delta = give ? amount.getAsLong() : -amount.getAsLong();
        resolve(sender, args[2], known -> services.tasks().then(services.economy().balances(known.uuid()), (balances, error) -> {
            if (error != null) {
                m.send(sender, "common.storage-error");
                return;
            }
            long before = balances.get(bucket.get());
            long after = before + delta;
            if (after < 0) {
                m.send(sender, "admin.eco.would-be-negative", Messages.p("before", Money.format(before)));
                return;
            }
            String opId = OpMeta.newOpId();
            UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
            AuditEntry audit = new AuditEntry(actor == null ? null : actor.toString(), sender.getName(), "economy.adjust",
                    known.uuid().toString(), bucket.get().key() + " " + (delta > 0 ? "+" : "") + delta + " (" + before + " → " + after + ")",
                    reason);
            String token = confirmations.create(actor, () -> applyAdjust(sender, known, bucket.get(), delta, before, opId, audit));
            m.send(sender, "admin.eco.preview", Messages.p("player", known.name()), Messages.p("bucket", bucket.get().key()),
                    Messages.p("delta", (delta > 0 ? "+" : "") + Money.format(delta)), Messages.p("before", Money.format(before)),
                    Messages.p("after", Money.format(after)), Messages.p("reason", reason), Messages.p("token", token));
        }));
    }

    private void applyAdjust(CommandSender sender, PlayerStore.Known known, Bucket bucket, long delta, long before,
                             String opId, AuditEntry audit) {
        Messages m = services.messages();
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
                            Messages.p("version", t.version()), Messages.p("material", t.material().name())));
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

    /** ออกไอเทม: ตรวจช่องว่าง → บันทึก serial → ใส่ inventory → อัปเดตสถานะ (ไม่โยนของลงพื้น) */
    private void give(CommandSender sender, Player target, ItemTemplate template) {
        Messages m = services.messages();
        if (target.getInventory().firstEmpty() == -1) {
            m.send(sender, "admin.item.inventory-full", Messages.p("player", target.getName()));
            return;
        }
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
            Player online = Bukkit.getPlayer(targetId);
            HashMap<Integer, ItemStack> leftover = online == null ? null
                    : online.getInventory().addItem(services.items().create(template, serial));
            boolean delivered = online != null && leftover.isEmpty();
            if (serial != null) {
                String state = delivered ? "DELIVERED" : "DELIVERY_FAILED";
                services.tasks().then(services.database().async(() -> {
                    services.itemInstances().setState(serial, state, System.currentTimeMillis());
                    return null;
                }), (x, e) -> {
                });
            }
            m.send(sender, delivered ? "admin.item.given" : "admin.item.delivery-failed",
                    Messages.p("player", target.getName()), Messages.p("id", template.id()),
                    Messages.p("serial", serial == null ? "-" : serial.toString().substring(0, 8)));
        });
    }

    // ------------------------------------------------------------ audit

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
    private void resolve(CommandSender sender, String name, java.util.function.Consumer<PlayerStore.Known> then) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            then.accept(new PlayerStore.Known(online.getUniqueId(), online.getName()));
            return;
        }
        services.tasks().then(services.database().async(() -> services.players().findByName(name)), (known, error) -> {
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
        if (args.length == 1) {
            options.addAll(List.of("help", "doctor", "bank", "eco", "confirm", "npc", "item", "audit"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "eco" -> options.addAll(List.of("give", "take"));
                case "npc" -> options.addAll(List.of("spawn", "anchor", "remove", "unanchor", "list"));
                case "item" -> options.addAll(List.of("list", "give", "inspect"));
                case "bank", "audit" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "eco", "item" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                case "npc" -> {
                    if (args[1].equalsIgnoreCase("spawn") || args[1].equalsIgnoreCase("anchor")) {
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
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
