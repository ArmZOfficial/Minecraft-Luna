package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.entity.Player;

/** ข้อความผลฝาก/ถอน ใช้ร่วมกันระหว่างเมนูกับคำสั่ง /bank */
public final class BankFeedback {

    private BankFeedback() {
    }

    public static void send(Services services, Player player, boolean deposit, TxResult result, Throwable error) {
        Messages m = services.messages();
        if (!player.isOnline()) {
            return;
        }
        if (error != null) {
            m.send(player, "common.storage-error");
            return;
        }
        if (result == null) {
            m.send(player, "common.busy");
            return;
        }
        String gold = Money.format(result.after().gold());
        String bank = Money.format(result.after().bank());
        switch (result.status()) {
            case OK -> m.send(player, deposit ? "bank.deposited" : "bank.withdrew",
                    Messages.p("amount", Money.format(result.amount())), Messages.p("wallet", gold), Messages.p("bank", bank));
            case NOTHING -> m.send(player, deposit ? "bank.nothing-to-deposit" : "bank.nothing-to-withdraw");
            case INSUFFICIENT_FUNDS -> m.send(player, deposit ? "bank.not-enough-wallet" : "bank.not-enough-bank",
                    Messages.p("wallet", gold), Messages.p("bank", bank));
            case LIMIT_EXCEEDED -> m.send(player, "bank.limit",
                    Messages.p("max", Money.format(services.settings().maxTransaction())));
            default -> m.send(player, "bank.failed", Messages.p("status", result.status().name()));
        }
    }
}
