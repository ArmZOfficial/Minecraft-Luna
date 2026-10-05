package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.station.ActionRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /fantasycore (alias /fc)
 * - ไม่มีอาร์กิวเมนต์: เวอร์ชัน
 * - action &lt;id&gt;: ทางเข้าสำหรับ NPC ของ Citizens (/npc command add -p fc action bank.main)
 *   บริการที่ต้องอยู่ที่สถานี จะตรวจระยะ anchor ทุกครั้ง พิมพ์เองจากที่ไกลไม่ได้
 */
public final class CoreCommand implements TabExecutor {

    private final Services services;

    public CoreCommand(Services services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        Messages m = services.messages();
        if (args.length >= 2 && args[0].equalsIgnoreCase("action")) {
            if (!(sender instanceof Player player)) {
                m.send(sender, "common.players-only");
                return true;
            }
            String id = args[1].toLowerCase(Locale.ROOT);
            services.actions().open(player, id, ActionRegistry.Source.COMMAND);
            return true;
        }
        m.send(sender, "core.version", Messages.p("version", services.plugin().getPluginMeta().getVersion()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        if (args.length == 1) {
            return List.of("action");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("action")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return services.actions().implementedIds().stream().filter(id -> id.startsWith(prefix)).sorted().toList();
        }
        return List.of();
    }
}
