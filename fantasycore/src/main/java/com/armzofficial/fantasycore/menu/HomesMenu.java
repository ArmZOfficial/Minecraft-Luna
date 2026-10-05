package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.home.HomeRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** รายการบ้าน — คลิกซ้ายเพื่อวาร์ป (ไม่มีการลบด้วยคลิกขวา ตาม ADMIN-PANEL-SPEC) */
public final class HomesMenu extends Menu {

    /** พื้นที่เนื้อหา 7×4 */
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private final Services services;
    private List<HomeRecord> homes;

    public HomesMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("home.menu.title"));
        this.services = services;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        services.tasks().then(services.homes().list(viewer), (list, error) -> {
            if (error != null) {
                services.messages().send(player, "common.storage-error");
                return;
            }
            homes = list;
            if (isOpenFor(player)) {
                render();
            }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        Player player = Bukkit.getPlayer(viewer);
        if (player == null) {
            return;
        }
        int limit = services.homes().limit(player);
        set(4, Icons.of(Material.OAK_SIGN, m.plain("home.menu.info.name"),
                m.lines("home.menu.info.lore", Messages.p("count", homes == null ? "…" : homes.size()),
                        Messages.p("limit", limit), Messages.p("worlds", String.join(", ", services.settings().homeWorlds())))));
        if (homes != null) {
            for (int i = 0; i < Math.min(homes.size(), CONTENT.length); i++) {
                HomeRecord home = homes.get(i);
                set(CONTENT[i], Icons.of(Material.RED_BED, m.plain("home.menu.entry.name", Messages.p("name", home.display())),
                                m.lines("home.menu.entry.lore", Messages.p("world", home.worldName()),
                                        Messages.p("coords", home.coords()))),
                        (p, c) -> {
                            p.closeInventory();
                            services.homes().teleport(p, home);
                        });
            }
            if (homes.isEmpty()) {
                set(22, Icons.of(Material.PAPER, m.plain("home.menu.empty.name"), m.lines("home.menu.empty.lore")));
            }
        }
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> new MainMenu(viewer, services).open(p));
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        fill(Icons.filler());
    }
}
