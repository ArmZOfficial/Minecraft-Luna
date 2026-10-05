package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import org.bukkit.Material;
import java.util.List;
import java.util.UUID;

public final class DungeonMenu extends Menu {
    private final Services services;
    public DungeonMenu(UUID viewer,Services services) { super(viewer,3,services.messages().plain("dungeon.menu.title")); this.services=services; }
    @Override public void render() {
        clear(); Messages m=services.messages();
        set(4,Icons.of(Material.END_STONE_BRICKS,m.plain("dungeon.menu.info-name"),m.lines("dungeon.menu.info-lore",Messages.p("status",services.dungeon().status()),Messages.p("duration",services.dungeon().durationMinutes()))));
        set(11,Icons.of(Material.IRON_SWORD,m.plain("dungeon.menu.join-name"),m.lines("dungeon.menu.join-lore")),(p,c) -> services.dungeon().join(p));
        set(15,Icons.of(Material.OAK_DOOR,m.plain("dungeon.menu.leave-name"),m.lines("dungeon.menu.leave-lore")),(p,c) -> { p.closeInventory(); services.dungeon().leave(p); });
        set(22,Icons.of(Material.BARRIER,m.plain("menu.close"),List.of()),(p,c) -> p.closeInventory()); fill(Icons.filler());
    }
}
