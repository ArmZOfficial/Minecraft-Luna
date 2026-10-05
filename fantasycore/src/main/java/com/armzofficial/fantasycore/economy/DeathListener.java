package com.armzofficial.fantasycore.economy;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.util.Money;
import com.armzofficial.fantasycore.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * กติกาตายต่อโลก (SERVER-SYSTEMS-PLAN §7): หัก gold.wallet ตามเปอร์เซ็นต์ปัดลง
 * เงินฝาก/เงินแดงไม่เสีย และเลือก keep-inventory ต่อโลกได้ — handler นี้หักเงินที่เดียว
 */
public final class DeathListener implements Listener {

    private final Settings settings;
    private final Messages messages;
    private final EconomyService economy;
    private final Tasks tasks;
    private final java.util.function.Predicate<World> trainingWorld;

    public DeathListener(Settings settings, Messages messages, EconomyService economy, Tasks tasks, java.util.function.Predicate<World> trainingWorld) {
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
        this.tasks = tasks;
        this.trainingWorld = trainingWorld;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String worldName = player.getWorld().getName();
        Settings.DeathRule rule = trainingWorld.test(player.getWorld()) ? new Settings.DeathRule(0,true) : settings.deathRule(worldName);
        if (rule.keepInventory()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        if (rule.goldLossPercent() <= 0) {
            return;
        }
        UUID id = player.getUniqueId();
        tasks.then(economy.deathLoss(id, rule.goldLossPercent(), worldName), (result, error) -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) {
                return;
            }
            if (error != null) {
                messages.send(online, "common.storage-error");
                return;
            }
            if (result.ok()) {
                messages.send(online, "death.gold-lost", Messages.p("amount", Money.format(result.amount())),
                        Messages.p("percent", rule.goldLossPercent()), Messages.p("wallet", Money.format(result.after().gold())),
                        Messages.p("bank", Money.format(result.after().bank())));
            }
        });
    }
}
