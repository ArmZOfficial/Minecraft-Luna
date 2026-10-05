package com.armzofficial.fantasycore.station;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.claim.LandInfo;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.menu.BankMenu;
import com.armzofficial.fantasycore.menu.HomesMenu;
import com.armzofficial.fantasycore.menu.MailMenu;
import com.armzofficial.fantasycore.menu.MainMenu;
import com.armzofficial.fantasycore.menu.RewardMenu;
import com.armzofficial.fantasycore.menu.RtpMenu;
import com.armzofficial.fantasycore.travel.SpawnTravel;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * ทะเบียน action ID ที่ NPC/เมนู/คำสั่งเรียกใช้ (ชื่อตาม INTERIOR-AND-MAP-PLAN §16 เช่น bank.main)
 * NPC ไม่หักหรือเพิ่มเงินเอง — แค่เปิดบริการของ Core ผ่าน action เดียวกับเมนู
 */
public final class ActionRegistry {

    public enum Source {
        MENU,
        STATION,
        COMMAND
    }

    public record ActionDef(String id, String permission, boolean requiresStation, String remotePermission,
                            BiConsumer<Player, Source> handler) {
    }

    public static final Pattern ID_FORMAT = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)+");

    /** action ที่มีในแผนแต่ยังไม่มีระบบ — วาง NPC ได้ แต่คลิกแล้วบอกว่า "ยังไม่เชื่อม" */
    private static final Set<String> PLANNED = Set.of(
            "reward.progress", "reward.online", "quest.exchange", "quest.gold", "quest.red",
            "cosmetic.item_skin", "cosmetic.title", "equipment.upgrade", "repair.main", "craft.main", "canvas.main",
            "market.main", "market.orders", "guild.main", "jobs.main", "pet.main", "afk.main",
            "dungeon.main", "boss.main", "pvp.main");

    private final Supplier<Services> services;
    private final Map<String, ActionDef> actions = new LinkedHashMap<>();

    public ActionRegistry(Supplier<Services> services) {
        this.services = services;
        register(new ActionDef("menu.main", "fantasy.menu", false, null,
                (p, s) -> new MainMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("navigation.main", "fantasy.menu", false, null,
                (p, s) -> new MainMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("bank.main", "fantasy.bank.use", true, "fantasy.bank.remote",
                (p, s) -> new BankMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("travel.rtp", "fantasy.rtp.use", false, null,
                (p, s) -> new RtpMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("travel.spawn", "fantasy.spawn", false, null,
                (p, s) -> SpawnTravel.teleport(services.get(), p)));
        register(new ActionDef("home.main", "fantasy.home.use", false, null,
                (p, s) -> new HomesMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("reward.daily", "fantasy.rewards", false, null,
                (p, s) -> new RewardMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("mail.main", "fantasy.mail", false, null,
                (p, s) -> new MailMenu(p.getUniqueId(), services.get()).open(p)));
        register(new ActionDef("land.main", "fantasy.land.use", false, null,
                (p, s) -> {
                    p.closeInventory();
                    LandInfo.show(services.get(), p);
                }));
    }

    private void register(ActionDef def) {
        actions.put(def.id(), def);
    }

    public boolean isImplemented(String id) {
        return actions.containsKey(id);
    }

    public boolean isKnown(String id) {
        return actions.containsKey(id) || PLANNED.contains(id);
    }

    public Set<String> knownIds() {
        Set<String> ids = new TreeSet<>(actions.keySet());
        ids.addAll(PLANNED);
        return ids;
    }

    public Set<String> implementedIds() {
        return actions.keySet();
    }

    public void open(Player player, String id, Source source) {
        Messages m = services.get().messages();
        ActionDef def = actions.get(id);
        if (def == null) {
            m.send(player, PLANNED.contains(id) ? "service.not-connected" : "service.unknown", Messages.p("action", id));
            return;
        }
        if (!player.hasPermission(def.permission())) {
            m.send(player, "common.no-permission");
            return;
        }
        if (def.requiresStation() && source != Source.STATION
                && (def.remotePermission() == null || !player.hasPermission(def.remotePermission()))
                && !services.get().stations().isNear(player, id)) {
            m.send(player, "service.go-to-station", Messages.p("action", id));
            return;
        }
        def.handler().accept(player, source);
    }
}
