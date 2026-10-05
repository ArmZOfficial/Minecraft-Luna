package com.armzofficial.fantasycore.claim;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import org.bukkit.entity.Player;

import java.util.Optional;

/** /land ใน v0.1: แสดงแปลงตรงที่ยืน (ขนาดจาก bounds จริง) — ซื้อ/อัปเกรด/สมาชิกผ่าน Core อยู่ phase ถัดไป */
public final class LandInfo {

    private LandInfo() {
    }

    public static void show(Services services, Player player) {
        Messages m = services.messages();
        ClaimAdapter claims = services.claims();
        if (!claims.available()) {
            m.send(player, "land.provider-missing");
            return;
        }
        Optional<ClaimInfo> claim = claims.playerClaimAt(player.getLocation(), player);
        if (claim.isEmpty()) {
            m.send(player, "land.none-here", Messages.p("world", player.getWorld().getName()));
        } else {
            ClaimInfo info = claim.get();
            String role = info.owner() ? "land.role.owner" : info.member() ? "land.role.member" : "land.role.visitor";
            m.send(player, "land.here", Messages.p("region", info.regionId()), Messages.p("x", info.sizeX()),
                    Messages.p("z", info.sizeZ()), Messages.p("miny", info.minY()), Messages.p("maxy", info.maxY()),
                    Messages.c("role", m.plain(role)));
        }
        m.send(player, "land.next-phase");
    }
}
