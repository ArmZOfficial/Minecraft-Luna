package com.armzofficial.fantasycore.menu;

import com.armzofficial.fantasycore.Services;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.exchange.ExchangeService;
import com.armzofficial.fantasycore.item.NativeEnchants;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** รายการสูตร → preview วัตถุดิบ/รางวัล/จำนวน → ยืนยัน; ไม่มีเส้นทางตัดของจากไอคอนรายการ */
public final class ExchangeMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private final Services services;
    private final ExchangeService exchange;
    private Map<String, Integer> usage;
    private ExchangeService.Recipe selected;
    private int batch = 1;
    private int page;

    public ExchangeMenu(UUID viewer, Services services) {
        this(viewer, services, services.exchange());
    }

    public ExchangeMenu(UUID viewer, Services services, ExchangeService exchange) {
        super(viewer, 6, services.messages().plain(exchange.key("menu.title")));
        this.services = services;
        this.exchange = exchange;
    }

    @Override
    public void open(Player player) {
        super.open(player);
        refresh(player);
    }

    private void refresh(Player player) {
        usage = null;
        services.tasks().then(exchange.usage(viewer), (fresh, error) -> {
            if (error != null) {
                services.messages().send(player, "common.storage-error");
            } else {
                usage = fresh;
            }
            if (isOpenFor(player)) {
                render();
            }
        });
    }

    @Override
    public void render() {
        clear();
        Messages m = services.messages();
        List<ExchangeService.Recipe> recipes = exchange.recipes();
        if (!exchange.enabled()) {
            set(22, Icons.of(Material.BARRIER, m.plain(exchange.key("disabled")), List.of()));
        } else if (selected == null) {
            int pages = Math.max(1, (recipes.size() + SLOTS.length - 1) / SLOTS.length);
            page = Math.min(page, pages - 1);
            set(4, Icons.of(Material.BOOK, m.plain(exchange.key("menu.list.name")),
                    m.lines(exchange.key("menu.list.lore"), Messages.p("page", page + 1), Messages.p("pages", pages))));
            for (int i = 0; i < SLOTS.length && page * SLOTS.length + i < recipes.size(); i++) {
                ExchangeService.Recipe recipe = recipes.get(page * SLOTS.length + i);
                set(SLOTS[i], recipeIcon(recipe, m.plain(exchange.key("menu.recipe.name"), Messages.p("name", recipe.name())),
                        enchantedLore(recipe, m.lines(exchange.key("menu.recipe.lore"), Messages.p("inputs", recipe.describeInputs(1)), Messages.p("price", recipe.price()),
                                Messages.p("outputs", recipe.describeOutputs(1)), Messages.p("used", used(recipe)),
                                Messages.p("limit", recipe.dailyLimit())))), (p, c) -> {
                    selected = recipe;
                    batch = 1;
                    render();
                });
            }
            if (page > 0) {
                set(48, Icons.of(Material.ARROW, m.plain(exchange.key("menu.previous")), List.of()), (p, c) -> {
                    page--;
                    render();
                });
            }
            if (page + 1 < pages) {
                set(50, Icons.of(Material.ARROW, m.plain(exchange.key("menu.next")), List.of()), (p, c) -> {
                    page++;
                    render();
                });
            }
        } else {
            set(13, recipeIcon(selected, m.plain(exchange.key("menu.recipe.name"), Messages.p("name", selected.name())),
                    enchantedLore(selected, m.lines(exchange.key("menu.preview"), Messages.p("inputs", selected.describeInputs(batch)), Messages.p("price", selected.price()),
                            Messages.p("available", exchange.crafting() ? exchange.describeAvailable(Bukkit.getPlayer(viewer), selected) : ""),
                            Messages.p("outputs", selected.describeOutputs(batch)), Messages.p("batch", batch),
                            Messages.p("used", used(selected)), Messages.p("limit", selected.dailyLimit())))));
            if (!exchange.crafting()) {
                set(20, Icons.of(Material.RED_DYE, m.plain(exchange.key("menu.less")), List.of()), (p, c) -> {
                    batch = Math.max(1, batch - 1);
                    render();
                });
                set(24, Icons.of(Material.LIME_DYE, m.plain(exchange.key("menu.more")), List.of()), (p, c) -> {
                    batch = Math.min(16, batch + 1);
                    render();
                });
            }
            if (usage != null && usage.getOrDefault(selected.id(), 0) + batch <= selected.dailyLimit()) {
                set(31, Icons.of(Material.EMERALD, m.plain(exchange.key("menu.confirm.name")), m.lines(exchange.key("menu.confirm.lore"))),
                        (p, c) -> {
                            ExchangeService.Recipe recipe = selected;
                            int confirmedBatch = batch;
                            setBusy(true);
                            render();
                            exchange.exchange(p, recipe, confirmedBatch,
                                    () -> isOpenFor(p) && selected == recipe && batch == confirmedBatch, () -> {
                                        setBusy(false);
                                        if (isOpenFor(p)) {
                                            refresh(p);
                                            render();
                                        }
                                    });
                        });
            } else {
                set(31, Icons.of(Material.GRAY_DYE, m.plain(usage == null ? "menu.busy" : exchange.key("quota")), List.of()));
            }
        }
        set(45, Icons.of(Material.ARROW, m.plain("menu.back"), List.of()), (p, c) -> {
            if (selected == null) {
                new MainMenu(viewer, services).open(p);
            } else {
                selected = null;
                render();
            }
        });
        set(49, Icons.of(Material.BARRIER, m.plain("menu.close"), List.of()), (p, c) -> p.closeInventory());
        set(53, Icons.of(Material.BARREL, m.plain("mail.menu.open.name"), m.lines("mail.menu.open.lore")),
                (p, c) -> services.actions().open(p, "mail.main", com.armzofficial.fantasycore.station.ActionRegistry.Source.MENU));
        if (isBusy()) {
            set(31, Icons.of(Material.CLOCK, m.plain("menu.busy"), List.of()));
        }
        fill(Icons.filler());
    }

    private Object used(ExchangeService.Recipe recipe) {
        return usage == null ? "…" : usage.getOrDefault(recipe.id(), 0);
    }

    /** Display-only icon: potion data is real, but it has no Core PDC/serial or delivery path. */
    private ItemStack recipeIcon(ExchangeService.Recipe recipe, Component name, List<Component> lore) {
        boolean potion = recipe.template() != null && recipe.template().potion() != null;
        ItemStack icon = Icons.of(potion ? Material.POTION : recipe.icon(), name, lore);
        if (potion) {
            PotionMeta meta = (PotionMeta) icon.getItemMeta();
            recipe.template().potion().apply(meta);
            meta.setEnchantmentGlintOverride(recipe.template().glint());
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private List<Component> enchantedLore(ExchangeService.Recipe recipe, List<Component> lore) {
        if (recipe.template() != null) {
            if (recipe.template().potion() != null) {
                lore.add(services.messages().plain("alchemy.menu.effect", Messages.p("effect", recipe.template().potion().description())));
                return lore;
            }
            var enchants = recipe.template().enchantments();
            if (enchants.isEmpty()) {
                lore.add(services.messages().plain("craft.menu.enchant", Messages.p("enchant", recipe.describeEnchantments())));
            } else for (var entry : enchants.entrySet()) {
                lore.add(services.messages().plain("craft.menu.enchant", Messages.p("enchant",
                        NativeEnchants.describe(Map.of(entry.getKey(), entry.getValue())))));
            }
        }
        return lore;
    }
}
