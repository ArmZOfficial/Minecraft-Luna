package com.armzofficial.fantasycore.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** ทุก key ที่โค้ดใช้ต้องมีใน messages_th.yml — ผู้เล่นจะไม่เห็น "[ข้อความหาย]" */
class MessageKeysTest {

    private static final Pattern LITERAL = Pattern.compile(
            "\"((?:common|core|service|menu|bank|economy|death|travel|spawn|rtp|home|land|admin)\\.[a-z0-9._-]+)\"");

    /** string ที่หน้าตาเหมือน key แต่เป็น action ID / ชนิด operation / audit action */
    private static final Set<String> NOT_MESSAGES = Set.of(
            "menu.main", "bank.main", "home.main", "land.main", "travel.rtp", "travel.spawn",
            "bank.deposit", "bank.withdraw", "death.loss", "economy.adjust", "admin.adjust",
            // path ใน config.yml
            "economy.currency.gold");

    /** key ที่ประกอบขึ้นตอนรัน */
    private static final List<String> DYNAMIC = List.of(
            "admin.status.ready", "admin.status.broken", "admin.status.fix", "admin.status.missing", "admin.status.off",
            "menu.main.explore.name", "menu.main.explore.lore", "menu.main.rewards.name", "menu.main.rewards.lore",
            "menu.main.land.name", "menu.main.land.lore", "menu.main.skins.name", "menu.main.skins.lore",
            "menu.main.exchange.name", "menu.main.exchange.lore", "menu.main.paint.name", "menu.main.paint.lore",
            "menu.main.upgrade.name", "menu.main.upgrade.lore", "menu.main.bank.name", "menu.main.bank.lore",
            "menu.main.spawn.name", "menu.main.spawn.lore", "prefix");

    @Test
    void everyUsedKeyExists() throws IOException {
        Set<String> defined = yamlKeys(Path.of("src/main/resources/messages_th.yml"));
        Set<String> used = new TreeSet<>(DYNAMIC);
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("Settings.java")) {
                    continue; // path ของ config.yml ไม่ใช่ข้อความ
                }
                Matcher matcher = LITERAL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!NOT_MESSAGES.contains(key) && !key.endsWith(".")) {
                        used.add(key);
                    }
                }
            }
        }
        Set<String> missing = new TreeSet<>(used);
        missing.removeAll(defined);
        // key ฐานที่โค้ดต่อท้าย .name/.lore เอง (เช่น menu.main.bank) นับว่ามีถ้ามีหัวข้อนั้น
        missing.removeIf(key -> defined.stream().anyMatch(d -> d.startsWith(key + ".")));
        assertTrue(missing.isEmpty(), "messages_th.yml ขาด key: " + missing);
    }

    /** อ่าน key แบบจุดจาก YAML ที่ใช้การย่อหน้า 2 ช่อง (พอสำหรับไฟล์ข้อความนี้) */
    static Set<String> yamlKeys(Path file) throws IOException {
        Set<String> keys = new HashSet<>();
        Deque<String[]> stack = new ArrayDeque<>(); // {indent, name}
        boolean inBlock = false;
        int blockIndent = 0;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (raw.isBlank() || raw.stripLeading().startsWith("#")) {
                continue;
            }
            int indent = raw.length() - raw.stripLeading().length();
            if (inBlock && indent > blockIndent) {
                continue;
            }
            inBlock = false;
            String line = raw.strip();
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).replace("'", "").replace("\"", "");
            while (!stack.isEmpty() && Integer.parseInt(stack.peek()[0]) >= indent) {
                stack.pop();
            }
            StringBuilder path = new StringBuilder();
            stack.descendingIterator().forEachRemaining(e -> path.append(e[1]).append('.'));
            path.append(name);
            String rest = line.substring(colon + 1).strip();
            if (rest.isEmpty()) {
                stack.push(new String[]{String.valueOf(indent), name});
            } else {
                keys.add(path.toString());
                if (rest.startsWith("|")) {
                    inBlock = true;
                    blockIndent = indent;
                }
            }
        }
        return keys;
    }
}
