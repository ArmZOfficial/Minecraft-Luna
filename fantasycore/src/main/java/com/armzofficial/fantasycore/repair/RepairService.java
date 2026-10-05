package com.armzofficial.fantasycore.repair;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.EconomyService;
import com.armzofficial.fantasycore.item.ItemAdapter;
import com.armzofficial.fantasycore.station.StationService;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.util.Money;
import com.armzofficial.fantasycore.util.PlayerDataSaving;
import com.armzofficial.fantasycore.util.Tasks;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;

/** ซ่อมใน slot เดิม; จองเงินก่อนเริ่ม, คืนเมื่อยังไม่แตะ item, ไม่ auto-refund เมื่อไม่ทราบผล */
public final class RepairService {
    public record Quote(ItemAdapter.Candidate item, long price, long wallet, long expiresAt) {
    }

    private final Plugin plugin;
    private final Messages messages;
    private final Database database;
    private final Tasks tasks;
    private final ItemAdapter adapter;
    private final RepairStore store;
    private final EconomyService economy;
    private final StationService stations;
    private final Set<UUID> pending = new HashSet<>(); // main thread
    private final List<String> problems = new ArrayList<>();
    private boolean enabled;
    private long base;
    private long fullDamage;

    public RepairService(Plugin plugin, Messages messages, Database database, Tasks tasks, ItemAdapter adapter,
                         RepairStore store, EconomyService economy, StationService stations, long maxTransaction) {
        this.plugin = plugin;
        this.messages = messages;
        this.database = database;
        this.tasks = tasks;
        this.adapter = adapter;
        this.store = store;
        this.economy = economy;
        this.stations = stations;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "repair.yml"));
        enabled = config.getBoolean("enabled", true);
        if (!config.isInt("pricing.base-gold") || !config.isInt("pricing.full-damage-gold")) {
            problems.add("pricing ต้องเป็นจำนวนเต็ม — ปิด repair");
        } else {
            base = config.getInt("pricing.base-gold");
            fullDamage = config.getInt("pricing.full-damage-gold");
            if (base < 0 || base > 1_000_000_000L || fullDamage < 1 || fullDamage > 1_000_000_000L
                    || base + fullDamage > maxTransaction) {
                problems.add("ราคา base/full-damage ผิดช่วงหรือเกิน max-transaction — ปิด repair");
            }
        }
    }

    public RepairStore store() { return store; }
    public List<String> problems() { return List.copyOf(problems); }
    public boolean enabled() { return enabled && problems.isEmpty() && PlayerDataSaving.enabled(); }
    public boolean nativePolicyEnabled() { return enabled; }

    public void preview(Player player, Consumer<Quote> done) {
        if (!gate(player)) { done.accept(null); return; }
        ItemAdapter.Candidate item;
        long price;
        try {
            item = adapter.inspect(player);
            if (item.damage() == 0) {
                messages.send(player, "repair.full");
                done.accept(null);
                return;
            }
            price = RepairPrice.calculate(item.damage(), item.maxDamage(), base, fullDamage);
        } catch (IllegalArgumentException e) {
            messages.send(player, "repair.item-invalid", Messages.p("reason", e.getMessage()));
            done.accept(null);
            return;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "อ่านไอเทมซ่อมไม่ได้", e);
            messages.send(player, "common.storage-error");
            done.accept(null);
            return;
        }
        UUID owner = player.getUniqueId();
        tasks.then(database.async(() -> store.validIdentity(owner, item.identity())), (valid, error) -> {
            if (error != null || !Boolean.TRUE.equals(valid)) {
                messages.send(player, error != null ? "common.storage-error" : "repair.identity-invalid");
                done.accept(null);
                return;
            }
            tasks.then(economy.balances(owner), (balances, balanceError) -> {
                if (balanceError != null) {
                    messages.send(player, "common.storage-error");
                    done.accept(null);
                } else {
                    done.accept(new Quote(item, price, balances.gold(), System.currentTimeMillis() + 60_000));
                }
            });
        });
    }

    public void repair(Player player, Quote quote, BooleanSupplier stillConfirmed, Runnable done) {
        if (!gate(player)) { done.run(); return; }
        if (quote == null || !valid(player, quote, stillConfirmed)) {
            messages.send(player, "repair.changed");
            done.run();
            return;
        }
        UUID owner = player.getUniqueId();
        if (!pending.add(owner)) {
            messages.send(player, "repair.busy");
            done.run();
            return;
        }
        Runnable finish = () -> { pending.remove(owner); done.run(); };
        String opId = UUID.randomUUID().toString();
        ItemAdapter.Candidate item = quote.item();
        RepairStore.Request request;
        try {
            // ราคาไม่รับจาก client/เมนู: คำนวณใหม่จาก snapshot และ config ที่บริการโหลดเอง
            long price = RepairPrice.calculate(item.damage(), item.maxDamage(), base, fullDamage);
            if (price != quote.price()) {
                messages.send(player, "repair.changed");
                finish.run();
                return;
            }
            request = new RepairStore.Request(opId, owner, item.identity(), item.before().getType().name(), item.slot(),
                    item.damage(), item.maxDamage(), price, item.before().serializeAsBytes(), item.repaired().serializeAsBytes());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "สร้าง repair snapshot ไม่สำเร็จ", e);
            messages.send(player, "common.storage-error");
            finish.run();
            return;
        }
        tasks.then(database.async(() -> store.reserve(request)), (reservation, error) -> {
            if (error != null) {
                cancelUntouched(player, opId, finish);
                return;
            }
            economy.remember(owner, reservation.after());
            if (reservation.status() != RepairStore.Status.RESERVED) {
                String key = switch (reservation.status()) {
                    case FUNDS -> "repair.funds";
                    case INVALID_ITEM -> "repair.identity-invalid";
                    case LIMIT -> "repair.limit";
                    default -> "repair.busy";
                };
                messages.send(player, key);
                finish.run();
            } else if (!valid(player, quote, stillConfirmed)) {
                cancelUntouched(player, opId, finish);
            } else {
                tasks.then(database.async(() -> store.beginApply(opId)), (begun, beginError) -> {
                    if (beginError != null || !Boolean.TRUE.equals(begun) || !valid(player, quote, stillConfirmed)) {
                        cancelUntouched(player, opId, finish);
                        return;
                    }
                    // APPLYING persist ก่อนแก้ของ. error ตั้งแต่จุดนี้ต้อง REVIEW ไม่เดาว่าซ่อมหรือยัง
                    try {
                        player.getInventory().setItem(item.slot(), item.repaired().clone());
                        player.saveData();
                    } catch (RuntimeException e) {
                        plugin.getLogger().log(Level.SEVERE, "แก้/บันทึก item ไม่สำเร็จ op " + opId, e);
                        review(player, opId, finish);
                        return;
                    }
                    tasks.then(database.async(() -> store.complete(opId)), (completed, commitError) -> {
                        if (commitError != null || !Boolean.TRUE.equals(completed)) {
                            review(player, opId, finish);
                        } else {
                            messages.send(player, "repair.success", Messages.p("price", Money.format(quote.price())), Messages.p("op", opId));
                            finish.run();
                        }
                    });
                });
            }
        });
    }

    private boolean gate(Player player) {
        if (!player.hasPermission("fantasy.repair.use")) {
            messages.send(player, "common.no-permission");
            return false;
        }
        if (!enabled() || !player.isOnline() || player.isDead()) {
            messages.send(player, "repair.disabled");
            return false;
        }
        if (!near(player)) {
            messages.send(player, "service.go-to-station", Messages.p("action", "repair.main"));
            return false;
        }
        return true;
    }

    private boolean near(Player player) {
        return player.hasPermission("fantasy.repair.remote") || stations.isNear(player, "repair.main");
    }

    private boolean valid(Player player, Quote quote, BooleanSupplier stillConfirmed) {
        return enabled() && player.isOnline() && !player.isDead() && player.hasPermission("fantasy.repair.use")
                && near(player) && System.currentTimeMillis() <= quote.expiresAt() && stillConfirmed.getAsBoolean()
                && player.getInventory().getHeldItemSlot() == quote.item().slot()
                && quote.item().before().equals(player.getInventory().getItem(quote.item().slot()))
                && adapter.uniqueInInventory(player, quote.item().identity());
    }

    private void cancelUntouched(Player player, String opId, Runnable finish) {
        UUID owner = player.getUniqueId();
        tasks.then(database.async(() -> {
            store.cancelUntouched(opId);
            return economy.store().balances(owner);
        }), (after, error) -> {
            economy.remember(owner, after);
            messages.send(player, error == null ? "repair.changed" : "common.storage-error");
            finish.run();
        });
    }

    private void review(Player player, String opId, Runnable finish) {
        tasks.then(database.async(() -> store.markReview(opId)), (marked, error) -> {
            messages.send(player, "repair.review", Messages.p("op", opId));
            finish.run();
        });
    }
}
