package com.armzofficial.fantasycore.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** โหลด items.yml และสร้าง/อ่านไอเทมที่มี template/serial ใน PersistentDataContainer */
public final class ItemTemplateService {

    private static final Pattern ID_FORMAT = Pattern.compile("[a-z0-9_]{1,48}");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final NamespacedKey templateKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey serialKey;
    private final Map<String, ItemTemplate> templates;
    private final Map<String, Map<Integer, ItemTemplate>> revisions = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public ItemTemplateService(Plugin plugin) {
        this(plugin.getName().toLowerCase(Locale.ROOT), readFile(new File(plugin.getDataFolder(), "items.yml")),
                ItemTemplateService::validateMaterialAndEnchants);
    }

    // Parser ใช้ได้โดยไม่เริ่ม Minecraft; validation ที่ต้องอ่าน registry อยู่บน main thread ตอน enable
    ItemTemplateService(String namespace, YamlConfiguration yaml, Consumer<ItemTemplate> validator) {
        this.templateKey = new NamespacedKey(namespace, "template");
        this.versionKey = new NamespacedKey(namespace, "template_version");
        this.serialKey = new NamespacedKey(namespace, "serial");
        this.templates = Collections.unmodifiableMap(load(yaml, validator));
    }

    private static YamlConfiguration readFile(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalArgumentException("อ่าน items.yml ไม่ได้ — ไม่เปิดระบบด้วยแม่แบบที่โหลดไม่ครบ", e);
        }
        return yaml;
    }

    private Map<String, ItemTemplate> load(YamlConfiguration yaml, Consumer<ItemTemplate> validator) {
        Map<String, ItemTemplate> map = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("templates");
        if (section == null) {
            problems.add("items.yml ไม่มีหัวข้อ templates");
            return map;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(id);
            if (t == null) {
                problems.add(id + ": ต้องเป็นหัวข้อแม่แบบ");
                continue;
            }
            if (!ID_FORMAT.matcher(id).matches()) {
                problems.add("template ID '" + id + "' ต้องเป็น a-z 0-9 _ เท่านั้น");
                continue;
            }
            Map<Integer, ItemTemplate> versions = new LinkedHashMap<>();
            revisions.put(id, versions);
            try {
                ItemTemplate current = readTemplate(id, t, validator, false);
                map.put(id, current);
                versions.put(current.version(), current);
            } catch (IllegalArgumentException e) {
                problems.add(id + ": " + e.getMessage() + " — ปิดแม่แบบปัจจุบัน");
            }
            ConfigurationSection old = t.getConfigurationSection("revisions");
            if (t.contains("revisions") && old == null) {
                problems.add(id + ": revisions ต้องเป็นหัวข้อเวอร์ชันเก่า");
            }
            if (old != null) for (String key : old.getKeys(false)) {
                try {
                    if (!key.matches("[1-9][0-9]{0,5}|1000000")) {
                        throw new IllegalArgumentException("revision key ต้องเป็นเลขเวอร์ชัน 1–1000000 ไม่เติมศูนย์นำหน้า");
                    }
                    int version = Integer.parseInt(key);
                    if (version >= version(t)) {
                        throw new IllegalArgumentException("revision ต้องเก่ากว่า version ปัจจุบัน");
                    }
                    ConfigurationSection snapshot = old.getConfigurationSection(key);
                    if (snapshot == null) {
                        throw new IllegalArgumentException("revision ต้องเก็บแม่แบบเต็ม");
                    }
                    ItemTemplate archived = readTemplate(id, snapshot, validator, true);
                    if (archived.version() != version) {
                        throw new IllegalArgumentException("revision key กับ version ไม่ตรงกัน");
                    }
                    versions.put(version, archived);
                } catch (IllegalArgumentException e) {
                    problems.add(id + " revision " + key + ": " + e.getMessage() + " — ปิดเฉพาะ revision นี้");
                }
            }
        }
        return map;
    }

    private static int version(ConfigurationSection t) {
        if (!t.contains("version")) { return 1; } // ค่า default ของ config v0.1–v0.5
        if (!t.isInt("version") || t.getInt("version") < 1 || t.getInt("version") > 1_000_000) {
            throw new IllegalArgumentException("version ต้องเป็นจำนวนเต็ม 1–1000000");
        }
        return t.getInt("version");
    }

    private static ItemTemplate readTemplate(String id, ConfigurationSection t, Consumer<ItemTemplate> validator, boolean archive) {
        Material material = Material.matchMaterial(t.getString("material", ""));
        if (material == null) { throw new IllegalArgumentException("material ไม่ถูกต้อง"); }
        ConfigurationSection enchants = t.getConfigurationSection("enchantments");
        if (t.contains("enchantments") && enchants == null) {
            throw new IllegalArgumentException("enchantments ต้องเป็นหัวข้อ key: level");
        }
        if (archive && List.of("version", "material", "name", "lore", "serialized", "glint").stream().anyMatch(key -> !t.contains(key))) {
            throw new IllegalArgumentException("revision ต้องเก็บ version/material/name/lore/serialized/glint เต็ม ไม่สืบทอดจากรุ่นใหม่");
        }
        if ((t.contains("name") && (!t.isString("name") || t.getString("name").isBlank()))
                || (t.contains("lore") && (!t.isList("lore") || t.getList("lore").stream().anyMatch(line -> !(line instanceof String))))
                || (t.contains("serialized") && !t.isBoolean("serialized")) || (t.contains("glint") && !t.isBoolean("glint"))) {
            throw new IllegalArgumentException("name/lore/serialized/glint มีชนิดข้อมูลไม่ถูกต้อง");
        }
        ItemTemplate template = new ItemTemplate(id, version(t), material, t.getString("name", id), t.getStringList("lore"),
                t.getBoolean("serialized", true), t.getBoolean("glint", false),
                NativeEnchants.parse(enchants == null ? Map.of() : enchants.getValues(false)));
        validator.accept(template);
        return template;
    }

    private static void validateMaterialAndEnchants(ItemTemplate template) {
        if (!template.material().isItem() || template.material().isAir()) { throw new IllegalArgumentException("material ไม่ใช่ item ที่ใช้ได้"); }
        ItemStack probe = new ItemStack(template.material());
        List<Enchantment> accepted = new ArrayList<>();
        for (var entry : template.enchantments().entrySet()) {
            Enchantment enchant = enchant(entry.getKey());
            if (entry.getValue() < enchant.getStartLevel() || entry.getValue() > enchant.getMaxLevel()
                    || !enchant.canEnchantItem(probe)) {
                throw new IllegalArgumentException(entry.getKey() + " ใช้กับ material/level นี้ไม่ได้");
            }
            if (accepted.stream().anyMatch(other -> enchant.conflictsWith(other) || other.conflictsWith(enchant))) {
                throw new IllegalArgumentException("enchant ขัดกัน: " + entry.getKey());
            }
            accepted.add(enchant);
        }
    }

    private static Enchantment enchant(String key) {
        Enchantment enchant = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.fromString(key));
        if (enchant == null) { throw new IllegalArgumentException("ไม่พบ enchant ใน Paper registry: " + key); }
        return enchant;
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

    /** ซ่อมตาม revision ที่ออกจริงเท่านั้น; ห้าม fallback ไปแม่แบบใหม่เมื่อไม่พบรุ่นเก่า */
    public Optional<ItemTemplate> template(String id, int version) {
        var versions = id == null ? null : revisions.get(id.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(versions == null ? null : versions.get(version));
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
        for (var entry : template.enchantments().entrySet()) {
            if (!meta.addEnchant(enchant(entry.getKey()), entry.getValue(), false)) {
                throw new IllegalStateException("ใส่ enchant ไม่สำเร็จ: " + entry.getKey());
            }
        }
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
        String id = pdc.has(templateKey, PersistentDataType.STRING) ? pdc.get(templateKey, PersistentDataType.STRING) : null;
        if (id == null) {
            return Optional.empty();
        }
        Integer version = pdc.has(versionKey, PersistentDataType.INTEGER) ? pdc.get(versionKey, PersistentDataType.INTEGER) : null;
        String serial = pdc.has(serialKey, PersistentDataType.STRING) ? pdc.get(serialKey, PersistentDataType.STRING) : null;
        UUID serialId = null;
        if (serial != null) {
            try {
                serialId = UUID.fromString(serial);
                if (!serialId.toString().equals(serial)) {
                    serialId = null;
                }
            } catch (IllegalArgumentException ignored) {
                // serial ผิดรูปแบบ = ไม่ใช่ของที่ Core ออก
            }
        }
        return Optional.of(new Identity(id, version == null ? 0 : version, serialId));
    }

    public record Identity(String templateId, int version, UUID serial) {
    }

    /** รวม marker ที่ชนิดข้อมูลผิดด้วย เพื่อไม่ให้ไอเทม Core เสียรูป fallback เป็น vanilla */
    public boolean hasIdentityFields(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        var keys = item.getPersistentDataContainer().getKeys();
        return keys.contains(templateKey) || keys.contains(versionKey) || keys.contains(serialKey);
    }
}
