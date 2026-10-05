package com.armzofficial.fantasycore.exchange;

import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.mail.MailService;
import com.armzofficial.fantasycore.economy.EconomyService;
import com.armzofficial.fantasycore.item.ItemTemplate;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import com.armzofficial.fantasycore.station.StationService;
import com.armzofficial.fantasycore.reward.RewardService;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.util.Tasks;
import com.armzofficial.fantasycore.util.PlayerDataSaving;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** Bukkit inventory ถูกอ่าน/แก้บน main thread; journal และ mailbox อยู่บน DB thread */
public final class ExchangeService {
    public record ResultItem(Material material, int amount) {
    }

    public record Recipe(String id, int version, String name, Material icon, int dailyLimit,
                         List<ExchangePlanner.Input> inputs, List<ResultItem> outputs, long price, ItemTemplate template) {
        public String describeInputs(int batch) {
            return String.join(" + ", inputs.stream().map(i -> i.material() + " ×" + i.amount() * batch).toList());
        }

        public String describeOutputs(int batch) {
            if (template != null) { return name + " ×1 · อุปกรณ์รูน"; }
            return String.join(" + ", outputs.stream().map(i -> i.material().name() + " ×" + i.amount() * batch).toList());
        }

        public String describeEnchantments() {
            return template == null ? "" : template.describeEnchantments();
        }
    }

    private record SlotChange(int slot, ItemStack before, ItemStack after) {
    }

    private final Plugin plugin;
    private final Messages messages;
    private final ExchangeStore store;
    private final Database database;
    private final Tasks tasks;
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();
    private final Set<UUID> pending = new HashSet<>(); // main thread เท่านั้น
    private boolean enabled;
    private final ItemTemplateService items;
    private final EconomyService economy;
    private final StationService stations;
    private final long maxTransaction;

    public ExchangeService(Plugin plugin, Messages messages, ExchangeStore store, Database database, Tasks tasks) {
        this(plugin, messages, store, database, tasks, null, null, null, 0);
    }

    public ExchangeService(Plugin plugin, Messages messages, ExchangeStore store, Database database, Tasks tasks,
                           ItemTemplateService items, EconomyService economy, StationService stations, long maxTransaction) {
        this.plugin = plugin;
        this.messages = messages;
        this.store = store;
        this.database = database;
        this.tasks = tasks;
        this.items = items;
        this.economy = economy;
        this.stations = stations;
        this.maxTransaction = maxTransaction;
        load(YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), crafting() ? "crafting.yml" : "exchanges.yml")));
    }

    public boolean crafting() { return store.kind() == ExchangeStore.Kind.CRAFT; }

    public String key(String suffix) { return store.kind().key() + "." + suffix; }

    private String permission() { return crafting() ? "fantasy.craft.use" : "fantasy.exchange"; }

    private boolean canUse(Player player) {
        return player.hasPermission(permission()) && (!crafting() || player.hasPermission("fantasy.craft.remote")
                || stations.isNear(player, "craft.main"));
    }

    public ExchangeStore store() {
        return store;
    }

    public List<Recipe> recipes() {
        return List.copyOf(recipes.values());
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }

    public boolean enabled() {
        return enabled && !recipes.isEmpty() && PlayerDataSaving.enabled();
    }

    public CompletableFuture<Map<String, Integer>> usage(UUID player) {
        String period = RewardService.period();
        return database.async(() -> store.usage(player, period));
    }

    /** ตัวเลขใน preview เท่านั้น; ยืนยันจริงยังใช้ planner + snapshot ตรวจซ้ำ */
    public String describeAvailable(Player player, Recipe recipe) {
        if (player == null) { return "…"; }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && !item.isEmpty() && !item.hasItemMeta()) {
                counts.merge(item.getType().name(), item.getAmount(), Integer::sum);
            }
        }
        return String.join(" · ", recipe.inputs().stream().map(i -> i.material() + " "
                + counts.getOrDefault(i.material(), 0) + "/" + i.amount()
                + " (ขาด " + Math.max(0, i.amount() - counts.getOrDefault(i.material(), 0)) + ")").toList());
    }

    public void exchange(Player player, Recipe recipe, int batch, BooleanSupplier stillConfirmed, Runnable done) {
        if (!player.hasPermission(permission())) {
            messages.send(player, "common.no-permission");
            done.run();
            return;
        }
        if (!canUse(player)) {
            messages.send(player, "service.go-to-station", Messages.p("action", "craft.main"));
            done.run();
            return;
        }
        if (!enabled() || recipe == null || recipes.get(recipe.id()) != recipe || batch < 1 || batch > (crafting() ? 1 : 16)) {
            messages.send(player, key("disabled"));
            done.run();
            return;
        }
        UUID owner = player.getUniqueId();
        if (!pending.add(owner)) {
            messages.send(player, key("busy"));
            done.run();
            return;
        }
        Runnable finish = () -> {
            pending.remove(owner);
            if (crafting()) { economy.load(owner); }
            done.run();
        };
        String opId = UUID.randomUUID().toString();
        List<SlotChange> changes;
        ExchangeStore.Request request;
        try {
            changes = plan(player, recipe, batch);
            if (changes.isEmpty()) {
                messages.send(player, key("missing"), Messages.p("inputs", recipe.describeInputs(batch)));
                finish.run();
                return;
            }
            request = new ExchangeStore.Request(opId, owner, recipe.id(), recipe.version(), batch, RewardService.period(),
                    recipe.dailyLimit(), recipe.describeInputs(batch), snapshot(changes), outputs(recipe, batch), recipe.price());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "สร้าง exchange snapshot ไม่สำเร็จ", e);
            messages.send(player, "common.storage-error");
            finish.run();
            return;
        }
        tasks.then(database.async(() -> store.prepare(request)), (result, error) -> {
            if (error != null) {
                // ยังไม่ตัดของ; หาก prepare commit ไปแล้วก็ยกเลิกได้อย่างปลอดภัย
                cancelUntouched(player, opId, finish);
            } else if (result != ExchangeStore.PrepareResult.PREPARED) {
                messages.send(player, key(switch (result) {
                    case QUOTA -> "quota";
                    case FUNDS, LIMIT -> "funds";
                    default -> "busy";
                }));
                finish.run();
            } else if (!valid(player, changes, stillConfirmed)) {
                cancelUntouched(player, opId, finish);
            } else {
                tasks.then(database.async(() -> store.beginConsume(opId)), (started, beginError) -> {
                    if (beginError != null || !Boolean.TRUE.equals(started) || !valid(player, changes, stillConfirmed)) {
                        cancelUntouched(player, opId, finish);
                        return;
                    }
                    // CONSUMING ถูก persist แล้ว. จากจุดนี้ห้าม auto-refund/ยกเลิกเมื่อไม่ทราบผล
                    try {
                        for (SlotChange change : changes) {
                            player.getInventory().setItem(change.slot(), change.after() == null ? null : change.after().clone());
                        }
                        player.saveData();
                    } catch (RuntimeException e) {
                        plugin.getLogger().log(Level.SEVERE, "ตัด/บันทึก inventory ไม่สำเร็จ — ตรวจ exchange " + opId, e);
                        review(player, opId, finish);
                        return;
                    }
                    tasks.then(database.async(() -> store.complete(opId)), (completion, completeError) -> {
                        if (completeError != null || !completion.changed()) {
                            review(player, opId, finish);
                            return;
                        }
                        messages.send(player, key("success"), Messages.p("name", recipe.name()),
                                Messages.p("batch", batch), Messages.p("op", opId));
                        // รางวัลรอใน /mail ให้ผู้เล่นรับเอง; ไม่ใส่ของหรือทิ้งลงพื้นจาก exchange
                        finish.run();
                    });
                });
            }
        });
    }

    private void cancelUntouched(Player player, String opId, Runnable finish) {
        tasks.then(database.async(() -> store.cancelUntouched(opId)), (cancelled, error) -> {
            messages.send(player, error == null ? key("changed") : "common.storage-error");
            finish.run();
        });
    }

    private void review(Player player, String opId, Runnable finish) {
        tasks.then(database.async(() -> store.markReview(opId)), (marked, error) -> {
            messages.send(player, key("review"), Messages.p("op", opId));
            finish.run();
        });
    }

    private boolean valid(Player player, List<SlotChange> changes, BooleanSupplier stillConfirmed) {
        return PlayerDataSaving.enabled()
                && player.isOnline() && !player.isDead() && canUse(player) && stillConfirmed.getAsBoolean()
                && changes.stream().allMatch(c -> c.before().equals(player.getInventory().getItem(c.slot())));
    }

    private static List<SlotChange> plan(Player player, Recipe recipe, int batch) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        List<ExchangePlanner.Slot> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(36, storage.length); i++) {
            ItemStack item = storage[i];
            if (item != null && !item.isEmpty()) {
                slots.add(new ExchangePlanner.Slot(i, item.getType().name(), item.getAmount(), !item.hasItemMeta()));
            }
        }
        return ExchangePlanner.plan(recipe.inputs(), batch, slots).map(takes -> takes.stream().map(t -> {
            ItemStack before = storage[t.slot()].clone();
            int remaining = before.getAmount() - t.amount();
            ItemStack after = remaining == 0 ? null : before.clone();
            if (after != null) {
                after.setAmount(remaining);
            }
            return new SlotChange(t.slot(), before, after);
        }).toList()).orElse(List.of());
    }

    /** snapshot format v1: int version, int count, แล้ว {int slot, bytes before, bytes after}; null = length 0 */
    private static byte[] snapshot(List<SlotChange> changes) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1);
            out.writeInt(changes.size());
            for (SlotChange change : changes) {
                out.writeInt(change.slot());
                for (ItemStack item : new ItemStack[]{change.before(), change.after()}) {
                    byte[] data = item == null ? new byte[0] : item.serializeAsBytes();
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("serialize snapshot", e);
        }
    }

    private List<ExchangeStore.Output> outputs(Recipe recipe, int batch) {
        if (crafting()) {
            UUID serial = UUID.randomUUID();
            ItemStack item = items.create(recipe.template(), serial);
            return List.of(new ExchangeStore.Output(MailService.describe(item), item.serializeAsBytes(), serial,
                    recipe.template().id(), recipe.template().version()));
        }
        List<ExchangeStore.Output> outputs = new ArrayList<>();
        for (ResultItem result : recipe.outputs()) {
            int remaining = result.amount() * batch;
            while (remaining > 0) {
                int size = Math.min(remaining, result.material().getMaxStackSize());
                ItemStack item = new ItemStack(result.material(), size);
                outputs.add(new ExchangeStore.Output(MailService.describe(item), item.serializeAsBytes()));
                remaining -= size;
            }
        }
        return List.copyOf(outputs);
    }

    private void load(YamlConfiguration config) {
        enabled = config.getBoolean("enabled", true);
        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) {
            problems.add("ไม่พบ recipes");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            try {
                if (!id.matches("[a-z0-9_]{1,48}")) {
                    throw new IllegalArgumentException("id ต้องเป็น a-z/0-9/_ ไม่เกิน 48 ตัว");
                }
                int version = integer(section, "version", 1, 1_000_000);
                int limit = integer(section, "daily-limit", 1, 1000);
                String name = section.getString("name", id);
                if (name.isBlank() || name.length() > 80) {
                    throw new IllegalArgumentException("name ต้องยาว 1–80 ตัว");
                }
                Material icon = material(section.getString("icon", "BOOK"));
                ConfigurationSection in = section.getConfigurationSection("inputs");
                ConfigurationSection out = section.getConfigurationSection("outputs");
                if (in == null || in.getKeys(false).isEmpty() || in.getKeys(false).size() > 8
                        || (!crafting() && (out == null || out.getKeys(false).isEmpty() || out.getKeys(false).size() > 8))) {
                    throw new IllegalArgumentException("inputs/outputs ต้องมีอย่างละ 1–8 material");
                }
                List<ExchangePlanner.Input> inputs = new ArrayList<>();
                Set<Material> unique = new HashSet<>();
                for (String key : in.getKeys(false)) {
                    Material type = material(key);
                    if (!unique.add(type)) {
                        throw new IllegalArgumentException("input material ซ้ำ");
                    }
                    inputs.add(new ExchangePlanner.Input(type.name(), integer(in, key, 1, 2304)));
                }
                List<ResultItem> results = new ArrayList<>();
                unique.clear();
                ItemTemplate template = null;
                long price = 0;
                if (crafting()) {
                    if (!"core".equals(section.getString("output.provider", "core"))) {
                        throw new IllegalArgumentException("รุ่นนี้รองรับ output.provider: core เท่านั้น — ItemsCore รอ bridge ที่ผ่านทดสอบ");
                    }
                    if ((!section.isInt("gold-cost") && !section.isLong("gold-cost"))
                            || section.getLong("gold-cost") < 0 || section.getLong("gold-cost") > maxTransaction) {
                        throw new IllegalArgumentException("gold-cost ต้องเป็นจำนวนเต็ม 0–" + maxTransaction);
                    }
                    price = section.getLong("gold-cost");
                    template = items.template(section.getString("output.template")).orElseThrow(
                            () -> new IllegalArgumentException("ไม่พบ output.template ใน items.yml"));
                    if (!template.serialized() || integer(section, "output.version", 1, 1_000_000) != template.version()) {
                        throw new IllegalArgumentException("output ต้องเป็น template แบบ serialized และ version ตรง items.yml");
                    }
                } else for (String key : out.getKeys(false)) {
                    Material type = material(key);
                    if (!unique.add(type)) {
                        throw new IllegalArgumentException("output material ซ้ำ");
                    }
                    results.add(new ResultItem(type, integer(out, key, 1, type.getMaxStackSize())));
                }
                recipes.put(id, new Recipe(id, version, name, icon, limit, List.copyOf(inputs), List.copyOf(results), price, template));
            } catch (IllegalArgumentException e) {
                problems.add(id + ": " + e.getMessage() + " — ปิดเฉพาะสูตรนี้");
            }
        }
        if (enabled && recipes.isEmpty()) {
            problems.add("ไม่มีสูตรที่เปิดใช้งานได้");
        }
    }

    private static Material material(String name) {
        Material type = name == null ? null : Material.matchMaterial(name);
        if (type == null || !type.isItem() || type.isAir()) {
            throw new IllegalArgumentException("material ไม่ถูกต้อง: " + name);
        }
        return type;
    }

    private static int integer(ConfigurationSection section, String key, int min, int max) {
        if (!section.isInt(key) || section.getInt(key) < min || section.getInt(key) > max) {
            throw new IllegalArgumentException(key + " ต้องเป็นจำนวนเต็ม " + min + "–" + max);
        }
        return section.getInt(key);
    }
}
