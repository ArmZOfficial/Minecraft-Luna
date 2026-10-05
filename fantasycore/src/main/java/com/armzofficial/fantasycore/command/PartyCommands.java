package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.dungeon.PartyRoster;
import com.armzofficial.fantasycore.menu.PartyMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public final class PartyCommands {
    private PartyCommands() {}
    public static String name(UUID id) {
        Player p=Bukkit.getPlayer(id); return p==null?id.toString().substring(0,8):p.getName();
    }
    public static void execute(Services s,Player player,String[] args) {
        Messages m=s.messages(); UUID id=player.getUniqueId(); PartyRoster roster=s.dungeon().parties();
        if(!player.hasPermission("fantasy.party")) { m.send(player,"common.no-permission"); return; }
        if(args.length==0 || args[0].equalsIgnoreCase("list")) { new PartyMenu(id,s).open(player); return; }
        if(s.dungeon().playing(id)) { m.send(player,"party.locked"); return; }
        String sub=args[0].toLowerCase(Locale.ROOT); PartyRoster.Result result;
        switch(sub) {
            case "invite" -> {
                if(args.length!=2) { m.send(player,"party.usage"); return; }
                Player target=Bukkit.getPlayerExact(args[1]);
                if(target==null || !target.hasPermission("fantasy.party") || s.dungeon().playing(target.getUniqueId())) { m.send(player,"party.invalid-target"); return; }
                result=roster.invite(id,target.getUniqueId());
                if(result==PartyRoster.Result.OK) { m.send(target,"party.invited",Messages.p("player",player.getName())); }
            }
            case "accept" -> result=roster.accept(id);
            case "leave" -> result=roster.leave(id);
            case "disband" -> result=roster.disband(id);
            case "kick" -> {
                if(args.length!=2) { m.send(player,"party.usage"); return; }
                UUID target=roster.party(id).stream().flatMap(p -> p.members().stream())
                        .filter(member -> name(member).equalsIgnoreCase(args[1]) || member.toString().equalsIgnoreCase(args[1])).findFirst().orElse(null);
                result=target==null?PartyRoster.Result.INVALID_TARGET:roster.kick(id,target);
            }
            default -> { m.send(player,"party.usage"); return; }
        }
        String key=switch(result) {
            case OK -> "party.updated"; case NOT_LEADER -> "party.not-leader";
            case ALREADY_JOINED -> "party.already-joined"; case FULL -> "party.full";
            case LOCKED -> "party.locked"; case NO_INVITE -> "party.no-invite";
            case NO_PARTY -> "party.no-party"; case INVALID_TARGET -> "party.invalid-target";
        };
        m.send(player,key);
        if(result==PartyRoster.Result.OK) { new PartyMenu(id,s).open(player); }
    }
}
