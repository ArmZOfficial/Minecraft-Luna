package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** เลือกโลกที่จะสุ่มวาร์ป (เฉพาะ allowlist ใน config) */
public final class RtpMenu extends Menu {

    private static final int[] SLOTS = {20, 22, 24, 30, 32};
    private static final Material[] ICONS = {Material.GRASS_BLOCK, Material.IRON_PICKAXE, Material.MAP, Material.OAK_SAPLING, Material.COMPASS};

    private final Services services;

    public RtpMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("rtp.menu.title"));
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
        long cooldown = services.rtp().cooldownLeftCached(player);
        set(4, Icons.of(Material.OAK_SIGN, m.plain("rtp.menu.info.name"), m.lines("rtp.menu.info.lore",
                Messages.p("cooldown", services.settings().rtpCooldownSeconds()),
                Messages.p("warmup", services.settings().warmupSeconds()),
                Messages.p("combat", services.settings().combatLockSeconds()))));
        int index = 0;
        for (Settings.RtpProfile profile : services.settings().rtpProfiles().values()) {
            if (index >= SLOTS.length) {
                break;
            }
            boolean loaded = Bukkit.getWorld(profile.world()) != null;
            String status = !loaded ? "rtp.menu.status.missing"
                    : cooldown > 0 ? "rtp.menu.status.cooldown" : "rtp.menu.status.ready";
            List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>(m.lines("rtp.menu.profile.lore",
                    Messages.p("min", Money.format(profile.minRadius())), Messages.p("max", Money.format(profile.maxRadius()))));
            lore.add(m.plain(status, Messages.p("seconds", cooldown)));
            String id = profile.id();
            set(SLOTS[index], Icons.of(ICONS[index], m.plain("rtp.menu.profile.name", Messages.p("world", profile.display())), lore),
                    (p, c) -> {
                        p.closeInventory();
                        services.rtp().start(p, id);
                    });
            index++;
        }
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> new MainMenu(viewer, services).open(p));
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        fill(Icons.filler());
    }
}
