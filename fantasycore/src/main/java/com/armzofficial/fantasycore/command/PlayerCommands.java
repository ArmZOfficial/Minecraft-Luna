package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.menu.BankFeedback;
import com.armzofficial.fantasycore.menu.BankHistory;
import com.armzofficial.fantasycore.menu.HomesMenu;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * คำสั่งผู้เล่น: /menu /bank /balance /sethome /home /delhome /homes /rtp /spawn /land /rewards /mail
 * ทุกคำสั่งเรียกบริการเดียวกับเมนู/NPC — ไม่มีเส้นทางข้ามกติกา
 */
public final class PlayerCommands implements TabExecutor {

    private final Services services;

    public PlayerCommands(Services services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        Messages m = services.messages();
        if (!(sender instanceof Player player)) {
            m.send(sender, "common.players-only");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "menu" -> services.actions().open(player, "menu.main", ActionRegistry.Source.COMMAND);
            case "bank" -> bank(player, args);
            case "balance" -> balance(player);
            case "sethome" -> {
                if (!player.hasPermission("fantasy.home.use")) {
                    m.send(player, "common.no-permission");
                    return true;
                }
                String name = args.length >= 1 ? args[0] : null;
                boolean confirm = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
                services.homes().setHome(player, name, confirm);
            }
            case "home" -> {
                if (!player.hasPermission("fantasy.home.use")) {
                    m.send(player, "common.no-permission");
                    return true;
                }
                services.homes().teleportByName(player, args.length >= 1 ? args[0] : null,
                        () -> new HomesMenu(player.getUniqueId(), services).open(player));
            }
            case "delhome" -> {
                if (args.length < 1) {
                    m.send(player, "home.usage-delete");
                    return true;
                }
                services.homes().delete(player, args[0]);
            }
            case "homes" -> services.actions().open(player, "home.main", ActionRegistry.Source.COMMAND);
            case "rtp" -> {
                if (args.length >= 1) {
                    services.rtp().start(player, args[0]);
                } else {
                    services.actions().open(player, "travel.rtp", ActionRegistry.Source.COMMAND);
                }
            }
            case "spawn" -> services.actions().open(player, "travel.spawn", ActionRegistry.Source.COMMAND);
            case "land" -> services.actions().open(player, "land.main", ActionRegistry.Source.COMMAND);
            case "exchange" -> services.actions().open(player, "quest.exchange", ActionRegistry.Source.COMMAND);
            case "rewards" -> {
                if (args.length >= 1 && args[0].equalsIgnoreCase("claim")) {
                    services.rewards().claim(player, () -> {
                    });
                } else {
                    services.actions().open(player, "reward.daily", ActionRegistry.Source.COMMAND);
                }
            }
            case "mail" -> {
                if (args.length >= 1 && args[0].equalsIgnoreCase("all")) {
                    services.mail().claimAll(player, summary -> services.messages().send(player,
                            summary.stoppedByFullInventory() ? "mail.claimed-partial" : "mail.claimed-all",
                            Messages.p("count", summary.claimed()), Messages.p("left", summary.remaining())));
                } else {
                    services.actions().open(player, "mail.main", ActionRegistry.Source.COMMAND);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void bank(Player player, String[] args) {
        Messages m = services.messages();
        if (args.length == 0) {
            services.actions().open(player, "bank.main", ActionRegistry.Source.COMMAND);
            return;
        }
        // คำสั่งย่อยใช้กติกาเดียวกับเมนู: ต้องอยู่ที่ธนาคารหรือมีสิทธิ์ทางไกลตามแรงค์
        if (!player.hasPermission("fantasy.bank.use")) {
            m.send(player, "common.no-permission");
            return;
        }
        if (!player.hasPermission("fantasy.bank.remote") && !services.stations().isNear(player, "bank.main")) {
            m.send(player, "service.go-to-station", Messages.p("action", "bank.main"));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "deposit", "d", "ฝาก" -> money(player, true, args);
            case "withdraw", "w", "ถอน" -> money(player, false, args);
            case "history", "h", "ประวัติ" -> BankHistory.show(services, player, player.getUniqueId(), player.getName());
            default -> m.send(player, "bank.usage");
        }
    }

    private void money(Player player, boolean deposit, String[] args) {
        Messages m = services.messages();
        if (args.length < 2) {
            m.send(player, "bank.usage");
            return;
        }
        long amount;
        if (Money.isAll(args[1])) {
            amount = EconomyStore.ALL;
        } else {
            OptionalLong parsed = Money.parsePositive(args[1]);
            if (parsed.isEmpty()) {
                m.send(player, "common.invalid-amount", Messages.p("input", args[1]));
                return;
            }
            amount = parsed.getAsLong();
        }
        var future = deposit ? services.economy().deposit(player.getUniqueId(), amount)
                : services.economy().withdraw(player.getUniqueId(), amount);
        services.tasks().then(future, (result, error) -> BankFeedback.send(services, player, deposit, result, error));
    }

    private void balance(Player player) {
        services.tasks().then(services.economy().balances(player.getUniqueId()), (balances, error) -> {
            if (error != null) {
                services.messages().send(player, "common.storage-error");
                return;
            }
            services.messages().send(player, "economy.balance", Messages.p("wallet", Money.format(balances.gold())),
                    Messages.p("bank", Money.format(balances.bank())), Messages.p("redcoin", Money.format(balances.red())));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (name.equals("bank") && args.length == 1) {
            options.addAll(List.of("deposit", "withdraw", "history"));
        } else if (name.equals("bank") && args.length == 2) {
            options.add("all");
            services.settings().bankQuickAmounts().forEach(v -> options.add(String.valueOf(v)));
        } else if (name.equals("rewards") && args.length == 1) {
            options.add("claim");
        } else if (name.equals("mail") && args.length == 1) {
            options.add("all");
        } else if (name.equals("rtp") && args.length == 1) {
            options.addAll(services.settings().rtpProfiles().keySet());
        } else if (name.equals("sethome") && args.length == 2) {
            options.add("confirm");
        } else if ((name.equals("home") || name.equals("delhome")) && args.length == 1 && sender instanceof Player player) {
            // ใช้ชื่อที่โหลดล่าสุด ไม่ query ฐานข้อมูลบน main thread
            options.addAll(services.homes().cachedNames(player.getUniqueId()));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
