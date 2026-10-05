package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.LedgerEntry;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** แสดงประวัติ ledger ในแชท (เวลา Asia/Bangkok พร้อม op ID ย่อสำหรับแอดมินค้น) */
public final class BankHistory {

    public static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BANGKOK);

    private BankHistory() {
    }

    public static void show(Services services, CommandSender to, UUID player, String name) {
        Messages m = services.messages();
        services.tasks().then(services.economy().history(player, 10), (entries, error) -> {
            if (error != null) {
                m.send(to, "common.storage-error");
                return;
            }
            if (entries.isEmpty()) {
                m.send(to, "bank.history.empty", Messages.p("player", name));
                return;
            }
            m.send(to, "bank.history.header", Messages.p("player", name));
            for (LedgerEntry entry : entries) {
                to.sendMessage(m.plain("bank.history.line",
                        Messages.p("time", TIME.format(Instant.ofEpochMilli(entry.createdAt()))),
                        Messages.p("bucket", entry.bucket().key()),
                        Messages.p("delta", (entry.delta() > 0 ? "+" : "") + Money.format(entry.delta())),
                        Messages.p("after", Money.format(entry.balanceAfter())),
                        Messages.p("reason", entry.reason()),
                        Messages.p("op", entry.opId().length() > 8 ? entry.opId().substring(0, 8) : entry.opId())));
            }
        });
    }
}
