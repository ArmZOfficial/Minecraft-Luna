package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * ธนาคาร: ฝาก/ถอน gold ระหว่าง wallet ↔ bank (ไม่สร้างเงินใหม่) และดูประวัติ
 * ระหว่างรอฐานข้อมูล เมนูเป็น busy ทั้งหน้า กดซ้ำไม่ได้
 */
public final class BankMenu extends Menu {

    private static final int[] DEPOSIT_SLOTS = {19, 20, 21};
    private static final int[] WITHDRAW_SLOTS = {28, 29, 30};

    private final Services services;
    private Balances balances;

    public BankMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("bank.menu.title"));
        this.services = services;
        this.balances = services.economy().cached(viewer).orElse(null);
    }

    @Override
    public void open(Player player) {
        super.open(player);
        // โหลดยอดล่าสุดจากฐานข้อมูลแล้ววาดใหม่
        services.tasks().then(services.economy().balances(viewer), (fresh, error) -> {
            if (fresh != null) {
                balances = fresh;
                if (isOpenFor(player)) {
                    render();
                }
            }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        Player player = Bukkit.getPlayer(viewer);
        if (player == null) {
            return;
        }
        String gold = balances == null ? "…" : Money.format(balances.gold());
        String bank = balances == null ? "…" : Money.format(balances.bank());
        String red = balances == null ? "…" : Money.format(balances.red());
        set(4, Icons.head(player, m.plain("bank.menu.balance.name"),
                m.lines("bank.menu.balance.lore", Messages.p("wallet", gold), Messages.p("bank", bank), Messages.p("redcoin", red))));

        List<Long> quick = services.settings().bankQuickAmounts();
        Material[] deposits = {Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK};
        for (int i = 0; i < Math.min(quick.size(), DEPOSIT_SLOTS.length); i++) {
            long amount = quick.get(i);
            set(DEPOSIT_SLOTS[i], Icons.of(deposits[i], m.plain("bank.menu.deposit.name", Messages.p("amount", Money.format(amount))),
                    m.lines("bank.menu.deposit.lore")), (p, c) -> run(p, true, amount));
            set(WITHDRAW_SLOTS[i], Icons.of(deposits[i], m.plain("bank.menu.withdraw.name", Messages.p("amount", Money.format(amount))),
                    m.lines("bank.menu.withdraw.lore")), (p, c) -> run(p, false, amount));
        }
        set(22, Icons.of(Material.CHEST, m.plain("bank.menu.deposit-all.name"), m.lines("bank.menu.deposit-all.lore")),
                (p, c) -> run(p, true, EconomyStore.ALL));
        set(31, Icons.of(Material.ENDER_CHEST, m.plain("bank.menu.withdraw-all.name"), m.lines("bank.menu.withdraw-all.lore")),
                (p, c) -> run(p, false, EconomyStore.ALL));
        set(24, Icons.of(Material.BOOK, m.plain("bank.menu.history.name"), m.lines("bank.menu.history.lore")),
                (p, c) -> {
                    p.closeInventory();
                    BankHistory.show(services, p, p.getUniqueId(), p.getName());
                });
        set(25, Icons.of(Material.OAK_SIGN, m.plain("bank.menu.rules.name"), m.lines("bank.menu.rules.lore",
                Messages.p("percent", services.settings().deathRule(player.getWorld().getName()).goldLossPercent()))));
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()),
                (p, c) -> new MainMenu(viewer, services).open(p));
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        if (isBusy()) {
            set(13, Icons.of(Material.CLOCK, m.plain("menu.busy"), List.of()));
        }
        fill(Icons.filler());
    }

    private void run(Player player, boolean deposit, long amount) {
        if (services.economy().isBusy(player.getUniqueId())) {
            return;
        }
        setBusy(true);
        render();
        CompletableFuture<TxResult> future = deposit
                ? services.economy().deposit(player.getUniqueId(), amount)
                : services.economy().withdraw(player.getUniqueId(), amount);
        services.tasks().then(future, (result, error) -> {
            setBusy(false);
            if (result != null) {
                balances = result.after();
            }
            BankFeedback.send(services, player, deposit, result, error);
            if (isOpenFor(player)) {
                render();
            }
        });
    }
}
