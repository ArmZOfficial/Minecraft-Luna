package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.command.AdminCommand;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.storage.PlayerStore;
import com.armzofficial.fantasycore.util.Money;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Dashboard/UUID player profiles/financial forms. Writes reuse AdminCommand's existing nonce and ledger. */
public final class AdminPanelMenu extends Menu {
    private enum Page { HOME, PLAYERS, PROFILE, MONEY, CONFIRM, ITEMS, STATIONS }
    private static final int[] CONTENT = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final Services services;
    private final AdminCommand admin;
    private Page page = Page.HOME;
    private int index;
    private PlayerStore.Known target;
    private Balances balances;
    private Bucket bucket = Bucket.GOLD_WALLET;
    private boolean give = true;
    private Long amount;
    private String reason;
    private long[] reviews;
    private AdminCommand.EconomyPreview preview;

    public AdminPanelMenu(UUID viewer, Services services, AdminCommand admin) {
        super(viewer, 6, services.messages().plain("admin.panel.title"));
        this.services = services; this.admin = admin;
    }

    private AdminPanelMenu copy(Page next) {
        var result = new AdminPanelMenu(viewer, services, admin);
        result.page = next; result.target = target; result.bucket = bucket;
        result.give = give; result.amount = amount; result.reason = reason;
        return result;
    }

    public void showPlayers(Player player, String query) {
        page = Page.PLAYERS;
        open(player);
        if (query != null) { search(player, query); }
    }

    private void search(Player actor, String query) {
        admin.resolve(actor, query, known -> {
            if (!isOpenFor(actor) || !actor.hasPermission("fantasyadmin.view")) { return; }
            var next = copy(Page.PROFILE); next.target = known; next.open(actor);
        });
    }

    @Override public void open(Player player) {
        super.open(player);
        if (!player.hasPermission("fantasyadmin.view")) { return; }
        if (page == Page.HOME) {
            services.tasks().then(services.database().async(() -> new long[]{services.mail().store().countReview(),
                    services.exchange().store().countReview(), services.craft().store().countReview(), services.repair().store().countReview()}), (counts, error) -> {
                if (!isOpenFor(player) || !player.hasPermission("fantasyadmin.view")) { return; }
                reviews = error == null ? counts : new long[]{-1,-1,-1,-1}; render();
            });
        } else if (target != null && (page == Page.PROFILE || page == Page.MONEY)) {
            services.tasks().then(services.economy().balances(target.uuid()), (value, error) -> {
                if (!isOpenFor(player) || !player.hasPermission("fantasyadmin.view")) { return; }
                if (error != null) { services.messages().send(player, "common.storage-error"); return; }
                balances = value; render();
            });
        }
    }

    @Override public void render() {
        clear();
        Player actor = Bukkit.getPlayer(viewer);
        if (actor == null) { return; }
        Messages m = services.messages();
        if (!actor.hasPermission("fantasyadmin.view")) {
            set(49, Icons.of(Material.BARRIER, m.plain("common.no-permission"), List.of()), (p,c) -> p.closeInventory());
            fill(Icons.filler()); return;
        }
        set(4, Icons.of(Material.NETHER_STAR, m.plain("admin.panel.heading", Messages.p("page", pageLabel())),
                m.lines("admin.panel.heading-lore", Messages.p("version", services.plugin().getPluginMeta().getVersion()))));
        button(45, Material.ARROW, "admin.panel.back", null, (p,c) -> {
            Page next = page == Page.CONFIRM ? Page.MONEY : page == Page.MONEY ? Page.PROFILE : page == Page.PROFILE ? Page.PLAYERS : Page.HOME;
            copy(next).open(p);
        });
        button(53, Material.BARRIER, "admin.panel.close", null, (p,c) -> p.closeInventory());
        switch (page) {
            case HOME -> home();
            case PLAYERS -> players();
            case PROFILE -> profile();
            case MONEY -> money();
            case CONFIRM -> confirmation();
            case ITEMS -> items();
            case STATIONS -> stations();
        }
        fill(Icons.filler());
    }

    private String pageLabel() {
        return switch (page) {
            case HOME -> "ภาพรวม"; case PLAYERS -> "ผู้เล่น"; case PROFILE -> target.name();
            case MONEY -> "ปรับเงิน"; case CONFIRM -> "ตรวจและยืนยัน"; case ITEMS -> "แม่แบบไอเทม"; case STATIONS -> "จุดบริการ";
        };
    }

    private void home() {
        button(10, Material.PLAYER_HEAD, "admin.panel.players", null, (p,c) -> copy(Page.PLAYERS).open(p));
        button(12, Material.GOLD_INGOT, "admin.panel.economy", null, (p,c) -> copy(Page.PLAYERS).open(p));
        button(14, Material.BOOK, "admin.panel.items", null, (p,c) -> copy(Page.ITEMS).open(p));
        button(16, Material.COMPASS, "admin.panel.stations", null, (p,c) -> copy(Page.STATIONS).open(p));
        String[] groups = {"mail", "exchange", "craft", "repair"};
        Material[] icons = {Material.BARREL, Material.EMERALD, Material.CRAFTING_TABLE, Material.ANVIL};
        for (int i=0; i<groups.length; i++) {
            String group = groups[i];
            String count = reviews == null ? "กำลังอ่าน" : reviews[i] < 0 ? "อ่านไม่ได้" : Long.toString(reviews[i]);
            set(28+i, Icons.of(icons[i], services.messages().plain("admin.panel.review-name", Messages.p("group", group)),
                    services.messages().lines("admin.panel.review-lore", Messages.p("count", count))),
                    guarded(null, (p,c) -> report(p, group, "review")));
        }
        button(32, Material.END_STONE_BRICKS, "admin.panel.dungeon", null, (p,c) -> report(p, "dungeon", "status"));
        set(34, Icons.of(Material.POTION, services.messages().plain("admin.panel.alchemy-name"),
                services.messages().lines("admin.panel.alchemy-lore", Messages.p("state", services.alchemy().enabled() ? "พร้อมใน source" : "ปิด"),
                        Messages.p("count", services.alchemy().recipes().size()))));
        button(48, Material.REDSTONE_TORCH, "admin.panel.doctor", null, (p,c) -> report(p, "doctor"));
        button(49, Material.CLOCK, "admin.panel.refresh", null, (p,c) -> copy(page).open(p));
        button(50, Material.WRITABLE_BOOK, "admin.panel.audit", "fantasyadmin.audit", (p,c) -> report(p, "audit"));
    }

    private void players() {
        var online = Bukkit.getOnlinePlayers().stream().sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        paginate(online.size());
        for (int i=0; i<CONTENT.length && index*CONTENT.length+i<online.size(); i++) {
            Player entry = online.get(index*CONTENT.length+i);
            var known = new PlayerStore.Known(entry.getUniqueId(), entry.getName());
            set(CONTENT[i], Icons.head(entry, services.messages().plain("admin.panel.player-name", Messages.p("player", known.name())),
                    services.messages().lines("admin.panel.player-lore", Messages.p("uuid", known.uuid()))),
                    guarded(null, (p,c) -> { var next = copy(Page.PROFILE); next.target = known; next.open(p); }));
        }
        button(48, Material.NAME_TAG, "admin.panel.search", null, (p,c) -> admin.input().ask(p, "admin.panel.input.player", "fantasyadmin.view", query -> {
            var next = copy(Page.PLAYERS); next.open(p); next.search(p, query);
        }));
    }

    private void profile() {
        targetCard(13);
        button(28, Material.GOLD_INGOT, "admin.panel.adjust", "fantasyadmin.economy.adjust", (p,c) -> copy(Page.MONEY).open(p));
        button(30, Material.CHEST, "admin.panel.history", null, (p,c) -> report(p, "bank", target.uuid().toString()));
        button(32, Material.WRITABLE_BOOK, "admin.panel.audit", "fantasyadmin.audit", (p,c) -> report(p, "audit", target.uuid().toString()));
        button(48, Material.CLOCK, "admin.panel.refresh", null, (p,c) -> copy(page).open(p));
    }

    private void targetCard(int slot) {
        set(slot, Icons.of(Material.PLAYER_HEAD, services.messages().plain("admin.panel.player-name", Messages.p("player", target.name())),
                services.messages().lines("admin.panel.balance-lore", Messages.p("uuid", target.uuid()),
                        Messages.p("gold", balances == null ? "…" : Money.format(balances.gold())),
                        Messages.p("bank", balances == null ? "…" : Money.format(balances.bank())),
                        Messages.p("redcoin", balances == null ? "…" : Money.format(balances.red())))));
    }

    private void money() {
        targetCard(13);
        Messages m = services.messages();
        Bucket[] buckets = Bucket.values();
        Material[] icons = {Material.GOLD_INGOT, Material.CHEST, Material.REDSTONE};
        for (int i=0; i<buckets.length; i++) {
            Bucket choice = buckets[i];
            set(20+i, Icons.of(icons[i], m.plain("admin.panel.bucket-name", Messages.p("bucket", choice.key())),
                    m.lines("admin.panel.bucket-lore", Messages.p("selected", choice == bucket ? "เลือกอยู่" : "คลิกเลือก"))),
                    guarded("fantasyadmin.economy.adjust", (p,c) -> { bucket = choice; render(); }));
        }
        set(24, Icons.of(give ? Material.LIME_DYE : Material.RED_DYE, m.plain("admin.panel.direction-name", Messages.p("direction", give ? "เพิ่ม" : "ลด")),
                m.lines("admin.panel.direction-lore")), guarded("fantasyadmin.economy.adjust", (p,c) -> { give = !give; render(); }));
        set(29, Icons.of(Material.PAPER, m.plain("admin.panel.amount-name", Messages.p("amount", amount == null ? "ยังไม่กรอก" : Money.format(amount))),
                m.lines("admin.panel.amount-lore", Messages.p("limit", Money.format(services.settings().maxTransaction())))),
                guarded("fantasyadmin.economy.adjust", (p,c) -> admin.input().ask(p, "admin.panel.input.amount", "fantasyadmin.economy.adjust", value -> {
                    var next = copy(Page.MONEY); var parsed = Money.parsePositive(value);
                    if (parsed.isPresent() && parsed.getAsLong() <= services.settings().maxTransaction()) { next.amount = parsed.getAsLong(); }
                    else { next.amount = null; m.send(p, "admin.eco.limit"); }
                    next.open(p);
                })));
        set(33, Icons.of(Material.WRITABLE_BOOK, m.plain("admin.panel.reason-name"), m.lines("admin.panel.reason-lore", Messages.p("reason", reason == null ? "ยังไม่กรอก" : reason))),
                guarded("fantasyadmin.economy.adjust", (p,c) -> admin.input().ask(p, "admin.panel.input.reason", "fantasyadmin.economy.adjust", value -> {
                    var next = copy(Page.MONEY);
                    if (value.length() >= 3) { next.reason = value; } else { next.reason = null; m.send(p, "admin.reason-required"); }
                    next.open(p);
                })));
        button(49, Material.LIME_DYE, "admin.panel.preview", "fantasyadmin.economy.adjust", (p,c) -> {
            if (amount == null || reason == null) { m.send(p, "admin.panel.incomplete"); return; }
            setBusy(true);
            admin.previewEconomy(p, target, bucket, give ? amount : -amount, reason, prepared -> {
                setBusy(false);
                if (!isOpenFor(p) || !p.hasPermission("fantasyadmin.view") || !p.hasPermission("fantasyadmin.economy.adjust")) {
                    if (prepared != null) { admin.cancelPreview(p, prepared.token()); } return;
                }
                if (prepared == null) { render(); return; }
                var next = copy(Page.CONFIRM); next.preview = prepared; next.open(p);
            });
        });
    }

    private void confirmation() {
        Messages m = services.messages();
        set(22, Icons.of(Material.GOLD_INGOT, m.plain("admin.panel.confirm-name"), m.lines("admin.panel.confirm-lore",
                Messages.p("player", preview.target().name()), Messages.p("uuid", preview.target().uuid()),
                Messages.p("bucket", preview.bucket().key()), Messages.p("delta", (preview.delta()>0 ? "+" : "")+Money.format(preview.delta())),
                Messages.p("before", Money.format(preview.before())), Messages.p("after", Money.format(preview.after())),
                Messages.p("reason", preview.reason()))));
        button(49, Material.LIME_DYE, "admin.panel.apply", "fantasyadmin.economy.adjust", (p,c) -> {
            setBusy(true);
            admin.execute(p, new String[]{"confirm", preview.token()});
            p.closeInventory();
        });
    }

    private void items() {
        var templates = services.items().templates().values().stream().sorted(Comparator.comparing(t -> t.id())).toList();
        paginate(templates.size());
        for (int i=0; i<CONTENT.length && index*CONTENT.length+i<templates.size(); i++) {
            var item = templates.get(index*CONTENT.length+i);
            set(CONTENT[i], Icons.of(Material.BOOK, services.messages().plain("admin.panel.template-name", Messages.p("id", item.id())),
                    services.messages().lines("admin.panel.template-lore", Messages.p("version", item.version()), Messages.p("material", item.material()),
                            Messages.p("effect", item.potion() == null ? item.describeEnchantments() : item.potion().description()))));
        }
    }

    private void stations() {
        var records = services.stations().records().stream().sorted(Comparator.comparing(r -> r.actionId())).toList();
        paginate(records.size());
        for (int i=0; i<CONTENT.length && index*CONTENT.length+i<records.size(); i++) {
            var station = records.get(index*CONTENT.length+i);
            set(CONTENT[i], Icons.of(Material.COMPASS, services.messages().plain("admin.panel.station-name", Messages.p("action", station.actionId())),
                    services.messages().lines("admin.panel.station-lore", Messages.p("id", station.id()), Messages.p("kind", station.kind()),
                            Messages.p("world", station.worldName()), Messages.p("x", station.x()), Messages.p("y", station.y()), Messages.p("z", station.z()),
                            Messages.p("yaw", station.yaw()), Messages.p("state", services.actions().isImplemented(station.actionId()) ? "มีบริการใน source" : "ยังไม่เชื่อม"))));
        }
    }

    private void paginate(int size) {
        if (size == 0) { set(22, Icons.of(Material.PAPER, services.messages().plain("admin.panel.empty"), List.of())); }
        int pages = Math.max(1, (size+CONTENT.length-1)/CONTENT.length);
        index = Math.min(index, pages-1);
        set(49, Icons.of(Material.PAPER, services.messages().plain("admin.panel.page", Messages.p("page", index+1), Messages.p("pages", pages)), List.of()));
        if (index>0) { button(47, Material.ARROW, "admin.panel.previous", null, (p,c) -> { var next=copy(page); next.index=index-1; next.open(p); }); }
        if (index+1<pages) { button(51, Material.ARROW, "admin.panel.next", null, (p,c) -> { var next=copy(page); next.index=index+1; next.open(p); }); }
    }

    private void button(int slot, Material material, String key, String permission, Action action) {
        var lore = services.messages().lines(key+".lore");
        Player actor = Bukkit.getPlayer(viewer);
        if (permission != null && (actor == null || !actor.hasPermission(permission))) { lore.add(services.messages().plain("common.no-permission")); }
        set(slot, Icons.of(material, services.messages().plain(key+".name"), lore), guarded(permission, action));
    }

    private Action guarded(String permission, Action action) {
        return (p,c) -> {
            if (!p.hasPermission("fantasyadmin.view") || (permission != null && !p.hasPermission(permission))) {
                services.messages().send(p, "common.no-permission"); return;
            }
            action.run(p,c);
        };
    }

    private void report(Player player, String... args) { player.closeInventory(); admin.execute(player, args); }
    @Override public void closed(Player player) { if (preview != null) { admin.cancelPreview(player, preview.token()); } }
}
