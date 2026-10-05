package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.util.Money;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /menu — 54 ช่อง หน้าหลัก 7 ปุ่มตาม CASUAL-SURVIVAL §2 + ธนาคาร/จุดเกิด
 * ปุ่มของระบบที่ยังไม่เชื่อมแสดงสถานะ "ยังไม่เชื่อม" ไม่มีปุ่ม success ปลอม
 */
public final class MainMenu extends Menu {

    private final Services services;

    public MainMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("menu.main.title"));
        this.services = services;
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        Player player = Bukkit.getPlayer(viewer);
        if (player == null) {
            return;
        }
        Balances balances = services.economy().cached(viewer).orElse(null);
        String gold = balances == null ? "…" : Money.format(balances.gold());
        String bank = balances == null ? "…" : Money.format(balances.bank());
        String red = balances == null ? "…" : Money.format(balances.red());
        set(4, Icons.head(player, m.plain("menu.main.profile.name", Messages.p("player", player.getName())),
                m.lines("menu.main.profile.lore", Messages.p("wallet", gold), Messages.p("bank", bank), Messages.p("redcoin", red))));

        // แถว 2: 7 ระบบตามภาพของผู้ใช้
        button(19, Material.COMPASS, "travel.rtp", "menu.main.explore");
        button(20, Material.CHEST, "reward.daily", "menu.main.rewards");
        button(21, Material.RED_BED, "home.main", "menu.main.land");
        button(22, Material.ARMOR_STAND, "cosmetic.item_skin", "menu.main.skins");
        button(23, Material.WRITABLE_BOOK, "quest.exchange", "menu.main.exchange");
        button(24, Material.PAINTING, "canvas.main", "menu.main.paint");
        button(25, Material.ANVIL, "equipment.upgrade", "menu.main.upgrade");
        // แถว 3: ธนาคาร (ต้องอยู่ที่ธนาคารหรือมีสิทธิ์ใช้ทางไกล) และกลับจุดเกิด
        button(30, Material.GOLD_INGOT, "bank.main", "menu.main.bank");
        button(32, Material.LODESTONE, "travel.spawn", "menu.main.spawn");
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        fill(Icons.filler());
    }

    private void button(int slot, Material icon, String actionId, String key) {
        Messages m = services.messages();
        ActionRegistry actions = services.actions();
        String statusKey = actions.isImplemented(actionId) ? "menu.status.ready" : "menu.status.not-connected";
        List<Component> lore = new ArrayList<>(m.lines(key + ".lore"));
        lore.add(Component.empty());
        lore.add(m.plain(statusKey));
        set(slot, Icons.of(icon, m.plain(key + ".name"), lore),
                (player, click) -> actions.open(player, actionId, ActionRegistry.Source.MENU));
    }
}
