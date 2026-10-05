package com.armzofficial.fantasycore.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** โหลด items.yml และสร้าง/อ่านไอเทมที่มี template/serial ใน PersistentDataContainer */
public final class ItemTemplateService {

    private static final Pattern ID_FORMAT = Pattern.compile("[a-z0-9_]{1,48}");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final NamespacedKey templateKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey serialKey;
    private final Map<String, ItemTemplate> templates;
    private final List<String> problems = new ArrayList<>();

    public ItemTemplateService(Plugin plugin) {
        this.templateKey = new NamespacedKey(plugin, "template");
        this.versionKey = new NamespacedKey(plugin, "template_version");
        this.serialKey = new NamespacedKey(plugin, "serial");
        this.templates = Collections.unmodifiableMap(load(new File(plugin.getDataFolder(), "items.yml")));
    }

    private Map<String, ItemTemplate> load(File file) {
        Map<String, ItemTemplate> map = new LinkedHashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("templates");
        if (section == null) {
            problems.add("items.yml ไม่มีหัวข้อ templates");
            return map;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(id);
            if (t == null) {
                continue;
            }
            if (!ID_FORMAT.matcher(id).matches()) {
                problems.add("template ID '" + id + "' ต้องเป็น a-z 0-9 _ เท่านั้น");
                continue;
            }
            Material material = Material.matchMaterial(t.getString("material", ""));
            if (material == null || !material.isItem() || material.isAir()) {
                problems.add(id + ": material ไม่ถูกต้อง");
                continue;
            }
            int version = t.getInt("version", 1);
            if (version < 1) {
                problems.add(id + ": version ต้อง >= 1");
                continue;
            }
            map.put(id, new ItemTemplate(id, version, material, t.getString("name", id), t.getStringList("lore"),
                    t.getBoolean("serialized", true), t.getBoolean("glint", false)));
        }
        return map;
    }

    public Map<String, ItemTemplate> templates() {
        return templates;
    }

    public List<String> problems() {
        return List.copyOf(problems);
    }

    public Optional<ItemTemplate> template(String id) {
        return Optional.ofNullable(id == null ? null : templates.get(id.toLowerCase(Locale.ROOT)));
    }

    public ItemStack create(ItemTemplate template, UUID serial) {
        ItemStack item = new ItemStack(template.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(template.name()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : template.lore()) {
            lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        if (serial != null) {
            lore.add(MM.deserialize("<dark_gray>#" + serial.toString().substring(0, 8)).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (template.glint()) {
            meta.setEnchantmentGlintOverride(true);
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(templateKey, PersistentDataType.STRING, template.id());
        pdc.set(versionKey, PersistentDataType.INTEGER, template.version());
        if (serial != null) {
            pdc.set(serialKey, PersistentDataType.STRING, serial.toString());
        }
        item.setItemMeta(meta);
        return item;
    }

    /** อ่านตัวตนของไอเทมจาก PDC (ฝั่ง server) — ไอเทมที่แค่ชื่อเหมือนจะได้ empty */
    public Optional<Identity> identify(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(templateKey, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        Integer version = pdc.get(versionKey, PersistentDataType.INTEGER);
        String serial = pdc.get(serialKey, PersistentDataType.STRING);
        UUID serialId = null;
        if (serial != null) {
            try {
                serialId = UUID.fromString(serial);
            } catch (IllegalArgumentException ignored) {
                // serial ผิดรูปแบบ = ไม่ใช่ของที่ Core ออก
            }
        }
        return Optional.of(new Identity(id, version == null ? 0 : version, serialId));
    }

    public record Identity(String templateId, int version, UUID serial) {
    }
}
