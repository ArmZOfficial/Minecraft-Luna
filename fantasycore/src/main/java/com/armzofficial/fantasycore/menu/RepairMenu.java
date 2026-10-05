package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.repair.RepairService;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** ก่อน/หลัง ราคา และ confirm แยกกัน; ไม่รับ item ที่ลากใส่เมนู */
public final class RepairMenu extends Menu {
    private final Services services;
    private RepairService.Quote quote;
    private boolean loading;

    public RepairMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("repair.menu.title"));
        this.services = services;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        refresh(player);
    }

    private void refresh(Player player) {
        quote = null;
        loading = true;
        setBusy(true);
        render();
        services.repair().preview(player, fresh -> {
            quote = fresh;
            loading = false;
            setBusy(false);
            if (isOpenFor(player)) { render(); }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        set(4, Icons.of(Material.ANVIL, m.plain("repair.menu.info.name"), m.lines("repair.menu.info.lore")));
        if (quote != null) {
            var item = quote.item();
            set(12, Icons.of(item.before().getType(), m.plain("repair.menu.before.name"), m.lines("repair.menu.before.lore",
                    Messages.p("material", item.before().getType()), Messages.p("current", item.remaining()),
                    Messages.p("max", item.maxDamage()), Messages.p("provider", item.identity().provider()))));
            set(14, Icons.of(item.repaired().getType(), m.plain("repair.menu.after.name"), m.lines("repair.menu.after.lore",
                    Messages.p("max", item.maxDamage()), Messages.p("price", Money.format(quote.price())),
                    Messages.p("wallet", Money.format(quote.wallet())))));
            set(13, Icons.of(Material.ARROW, m.plain("repair.menu.arrow"), List.of()));
            set(31, Icons.of(Material.EMERALD, m.plain("repair.menu.confirm.name"), m.lines("repair.menu.confirm.lore")), (p, c) -> {
                RepairService.Quote confirmed = quote;
                setBusy(true);
                render();
                services.repair().repair(p, confirmed, () -> isOpenFor(p) && quote == confirmed, () -> {
                    setBusy(false);
                    if (isOpenFor(p)) { refresh(p); }
                });
            });
        } else {
            set(22, Icons.of(Material.BARRIER, m.plain(loading ? "menu.busy" : "repair.menu.no-item"), List.of()));
        }
        set(40, Icons.of(Material.CLOCK, m.plain("repair.menu.refresh"), List.of()), (p, c) -> refresh(p));
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> new MainMenu(viewer, services).open(p));
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        if (isBusy()) { set(31, Icons.of(Material.CLOCK, m.plain("menu.busy"), List.of())); }
        fill(Icons.filler());
    }
}
