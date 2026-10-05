package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.reward.RewardService;
import com.armzofficial.fantasycore.reward.RewardStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** ปฏิทินรางวัลรายวัน: ครั้งที่รับแล้วในรอบ / ครั้งที่รับได้วันนี้ / ครั้งถัดไป */
public final class RewardMenu extends Menu {

    private static final int[] DAY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final Services services;
    private RewardStore.ProgramStatus status;

    public RewardMenu(UUID viewer, Services services) {
        super(viewer, 6, services.messages().plain("reward.menu.title"));
        this.services = services;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        refresh(player);
    }

    private void refresh(Player player) {
        services.tasks().then(services.rewards().status(viewer), (fresh, error) -> {
            if (error != null) {
                services.messages().send(player, "common.storage-error");
                return;
            }
            status = fresh;
            if (isOpenFor(player)) {
                render();
            }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        RewardService rewards = services.rewards();
        List<RewardStore.DayReward> cycle = rewards.cycle();
        int size = Math.max(1, cycle.size());
        String reset = RewardService.formatDuration(RewardService.untilReset());
        set(4, Icons.of(Material.CLOCK, m.plain("reward.menu.info.name"), m.lines("reward.menu.info.lore",
                Messages.p("total", status == null ? "…" : status.totalClaims()), Messages.p("cycle", size),
                Messages.p("time", reset))));

        // จำนวนครั้งที่รับแล้วในรอบปัจจุบัน
        int doneInCycle = 0;
        if (status != null) {
            doneInCycle = status.claimedThisPeriod() ? (status.totalClaims() - 1) % size + 1 : status.totalClaims() % size;
        }
        for (int i = 0; i < Math.min(cycle.size(), DAY_SLOTS.length); i++) {
            RewardStore.DayReward day = cycle.get(i);
            int index = i + 1;
            String state;
            Material icon;
            if (status == null) {
                state = "reward.menu.day.loading";
                icon = Material.CHEST;
            } else if (index <= doneInCycle) {
                state = "reward.menu.day.done";
                icon = Material.LIME_STAINED_GLASS_PANE;
            } else if (index == doneInCycle + 1 && !status.claimedThisPeriod()) {
                state = "reward.menu.day.today";
                icon = Material.GOLD_BLOCK;
            } else {
                state = "reward.menu.day.later";
                icon = Material.CHEST;
            }
            set(DAY_SLOTS[i], Icons.of(icon, m.plain("reward.menu.day.name", Messages.p("index", index)),
                    m.lines(state, Messages.p("reward", rewards.describe(day)))));
        }

        if (status != null && !status.claimedThisPeriod() && rewards.enabled()) {
            set(31, Icons.of(Material.EMERALD, m.plain("reward.menu.claim.name"), m.lines("reward.menu.claim.lore")),
                    (p, c) -> {
                        setBusy(true);
                        render();
                        rewards.claim(p, () -> {
                            setBusy(false);
                            if (isOpenFor(p)) {
                                refresh(p);
                            }
                        });
                    });
        } else if (status != null) {
            set(31, Icons.of(Material.GRAY_DYE, m.plain("reward.menu.claimed.name"),
                    m.lines("reward.menu.claimed.lore", Messages.p("time", reset))));
        }
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> new MainMenu(viewer, services).open(p));
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        set(53, Icons.of(Material.BARREL, m.plain("mail.menu.open.name"), m.lines("mail.menu.open.lore")),
                (p, c) -> new MailMenu(viewer, services).open(p));
        if (isBusy()) {
            set(40, Icons.of(Material.CLOCK, m.plain("menu.busy"), List.of()));
        }
        fill(Icons.filler());
    }
}
