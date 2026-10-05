package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.command.PartyCommands;
import com.armzofficial.fantasycore.config.Messages;
import org.bukkit.Material;
import java.util.*;

public final class PartyMenu extends Menu {
    private final Services services;
    public PartyMenu(UUID viewer,Services services) { super(viewer,3,services.messages().plain("party.menu.title")); this.services=services; }
    @Override public void render() {
        clear(); var m=services.messages(); var roster=services.dungeon().parties(); var party=roster.party(viewer);
        set(4,Icons.of(Material.NETHER_STAR,m.plain("party.menu.info-name"),m.lines("party.menu.info-lore",
                Messages.p("count",party.map(p -> p.members().size()).orElse(0)),
                Messages.p("leader",party.map(p -> PartyCommands.name(p.leader())).orElse("—")),
                Messages.p("status",roster.locked(viewer)?"กำลังลงดัน":"จัดทีมได้"))));
        if(party.isPresent()) {
            int slot=10; for(UUID member:party.get().members()) {
                set(slot++,Icons.of(Material.PLAYER_HEAD,m.plain("party.menu.member-name",Messages.p("player",PartyCommands.name(member))),List.of()));
            }
        }
        if(roster.invitedBy(viewer).isPresent()) {
            set(16,Icons.of(Material.EMERALD,m.plain("party.menu.accept-name"),m.lines("party.menu.accept-lore",
                    Messages.p("player",PartyCommands.name(roster.invitedBy(viewer).orElseThrow())))),
                    (p,c) -> PartyCommands.execute(services,p,new String[]{"accept"}));
        }
        set(20,Icons.of(Material.OAK_DOOR,m.plain("party.menu.leave-name"),m.lines("party.menu.leave-lore")),
                (p,c) -> PartyCommands.execute(services,p,new String[]{"leave"}));
        set(22,Icons.of(Material.COMPASS,m.plain("party.menu.dungeon-name"),List.of()),
                (p,c) -> new DungeonMenu(viewer,services).open(p));
        set(24,Icons.of(Material.BARRIER,m.plain("menu.close"),List.of()),(p,c) -> p.closeInventory());
        fill(Icons.filler());
    }
}
