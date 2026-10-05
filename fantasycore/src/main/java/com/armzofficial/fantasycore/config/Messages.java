package com.armzofficial.fantasycore.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ข้อความภาษาไทยจาก messages_th.yml (MiniMessage)
 * ค่าที่ขาดในไฟล์ของเซิร์ฟจะใช้ค่าจาก jar เพื่อไม่ให้ผู้เล่นเห็น key ดิบ
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<String, String> templates;
    private final List<String> missing = new CopyOnWriteArrayList<>();

    private Messages(Map<String, String> templates) {
        this.templates = templates;
    }

    public static Messages load(JavaPlugin plugin) {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = plugin.getResource("messages_th.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String key : defaults.getKeys(true)) {
                    if (defaults.isString(key)) {
                        map.put(key, defaults.getString(key));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("อ่าน messages_th.yml ใน jar ไม่ได้: " + e.getMessage());
        }
        File file = new File(plugin.getDataFolder(), "messages_th.yml");
        if (file.exists()) {
            YamlConfiguration custom = YamlConfiguration.loadConfiguration(file);
            for (String key : custom.getKeys(true)) {
                if (custom.isString(key)) {
                    map.put(key, custom.getString(key));
                }
            }
        }
        return new Messages(map);
    }

    public static Messages ofTemplates(Map<String, String> templates) {
        return new Messages(new HashMap<>(templates));
    }

    public boolean has(String key) {
        return templates.containsKey(key);
    }

    public List<String> missingKeys() {
        return List.copyOf(missing);
    }

    /** ข้อความไม่มี prefix (ใช้กับชื่อ/lore ของไอคอน) — ปิดตัวเอียงอัตโนมัติ */
    public Component plain(String key, TagResolver... resolvers) {
        return MM.deserialize(template(key), resolvers).decoration(TextDecoration.ITALIC, false);
    }

    /** หลายบรรทัด: template ใช้ \n คั่น */
    public List<Component> lines(String key, TagResolver... resolvers) {
        List<Component> lines = new ArrayList<>();
        for (String line : template(key).split("\n", -1)) {
            lines.add(MM.deserialize(line, resolvers).decoration(TextDecoration.ITALIC, false));
        }
        return lines;
    }

    /** ข้อความแชทพร้อม prefix */
    public Component chat(String key, TagResolver... resolvers) {
        return MM.deserialize(template("prefix") + template(key), resolvers);
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(chat(key, resolvers));
    }

    public static TagResolver p(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    public static TagResolver c(String name, Component value) {
        return Placeholder.component(name, value);
    }

    private String template(String key) {
        String value = templates.get(key);
        if (value == null) {
            if (!missing.contains(key)) {
                missing.add(key);
            }
            return "<red>[ข้อความหาย: " + key.replace("<", "") + "]";
        }
        return value;
    }
}
