package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.mail.MailService;
import com.armzofficial.fantasycore.mail.MailStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** กล่องจดหมาย: คลิกเพื่อรับทีละชิ้น หรือรับทั้งหมด — ไอคอนเป็นสำเนาเพื่อแสดงผล หยิบออกจากหน้าต่างไม่ได้ */
public final class MailMenu extends Menu {

    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(BankHistory.BANGKOK);

    private final Services services;
    private List<MailStore.MailItem> items;

    public MailMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("mail.menu.title"));
        this.services = services;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        refresh(player);
    }

    private void refresh(Player player) {
        services.tasks().then(services.mail().pending(viewer), (list, error) -> {
            if (error != null) {
                services.messages().send(player, "common.storage-error");
                return;
            }
            items = list;
            setBusy(false);
            if (isOpenFor(player)) {
                render();
            }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        Player viewerPlayer = Bukkit.getPlayer(viewer);
        if (viewerPlayer == null) {
            return;
        }
        set(4, Icons.of(Material.BARREL, m.plain("mail.menu.info.name"),
                m.lines("mail.menu.info.lore", Messages.p("count", items == null ? "…" : items.size()))));
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), CONTENT.length); i++) {
                MailStore.MailItem mail = items.get(i);
                set(CONTENT[i], preview(mail, viewerPlayer.getInventory()), (p, c) -> {
                    setBusy(true);
                    services.mail().claimOne(p, mail, result -> {
                        switch (result) {
                            case CLAIMED -> m.send(p, "mail.claimed-one", Messages.p("item", mail.label()));
                            case FULL -> m.send(p, "mail.full");
                            default -> {
                            }
                        }
                        refresh(p);
                    });
                });
            }
            if (items.isEmpty()) {
                set(22, Icons.of(Material.PAPER, m.plain("mail.menu.empty.name"), m.lines("mail.menu.empty.lore")));
            } else {
                set(49, Icons.of(Material.HOPPER, m.plain("mail.menu.claim-all.name"), m.lines("mail.menu.claim-all.lore")),
                        (p, c) -> {
                            setBusy(true);
                            services.mail().claimAll(p, summary -> {
                                m.send(p, summary.stoppedByFullInventory() ? "mail.claimed-partial" : "mail.claimed-all",
                                        Messages.p("count", summary.claimed()), Messages.p("left", summary.remaining()));
                                refresh(p);
                            });
                        });
            }
        }
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> new MainMenu(viewer, services).open(p));
        set(53, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        if (isBusy()) {
            set(4, Icons.of(Material.CLOCK, m.plain("menu.busy"), List.of()));
        }
        fill(Icons.filler());
    }

    private ItemStack preview(MailStore.MailItem mail, PlayerInventory inventory) {
        Messages m = services.messages();
        ItemStack icon;
        try {
            icon = ItemStack.deserializeBytes(mail.data());
        } catch (RuntimeException e) {
            return Icons.of(Material.BARRIER, m.plain("mail.menu.corrupt.name", Messages.p("id", mail.id())), List.of());
        }
        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.hasLore() && meta.lore() != null ? meta.lore() : List.of());
        lore.add(Component.empty());
        lore.addAll(m.lines("mail.menu.entry.lore", Messages.p("source", mail.source()),
                Messages.p("time", TIME.format(Instant.ofEpochMilli(mail.createdAt()))),
                Messages.p("fits", MailService.fits(inventory, icon) ? "มีที่ว่าง" : "กระเป๋าเต็ม")));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }
}
