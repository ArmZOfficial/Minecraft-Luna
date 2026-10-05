package com.armzofficial.fantasycore.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/** Adapter ที่รองรับจริงใน v0.4: vanilla + Core; unknown provider/components fail closed */
public final class ItemAdapter {
    private static final Set<DataComponentType> ALLOWED_EDITS = Set.of(DataComponentTypes.DAMAGE,
            DataComponentTypes.ENCHANTMENTS, DataComponentTypes.CUSTOM_NAME, DataComponentTypes.REPAIR_COST);
    private final ItemTemplateService templates;

    public record Candidate(int slot, ItemIdentityPolicy.Identity identity, int damage, int maxDamage,
                            ItemStack before, ItemStack repaired) {
        public int remaining() {
            return maxDamage - damage;
        }
    }

    public ItemAdapter(ItemTemplateService templates) {
        this.templates = templates;
    }

    /** ต้องเรียกบน main thread; snapshots ไม่ใช่ inventory reference */
    public Candidate inspect(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack before = player.getInventory().getItem(slot);
        if (before == null || before.isEmpty() || before.getAmount() != 1) {
            throw new IllegalArgumentException("ถืออุปกรณ์เพียง 1 ชิ้นในมือหลักก่อน");
        }
        before = before.clone();
        Integer max = before.getData(DataComponentTypes.MAX_DAMAGE);
        Integer damage = before.getData(DataComponentTypes.DAMAGE);
        if (max == null || max < 1 || before.hasData(DataComponentTypes.UNBREAKABLE)) {
            throw new IllegalArgumentException("ของชิ้นนี้ไม่มีความทนทานที่ซ่อมได้");
        }
        int used = damage == null ? 0 : damage;
        if (used < 0 || used >= max) {
            throw new IllegalArgumentException("ค่าความเสียหายผิดช่วง — ต้องตรวจไอเทมก่อน");
        }
        ItemIdentityPolicy.Identity identity;
        ItemStack baseline;
        if (templates.hasIdentityFields(before)) {
            var marker = templates.identify(before).orElseThrow(() -> new IllegalArgumentException("ข้อมูล Core เสียรูป"));
            var template = templates.template(marker.templateId()).orElseThrow(() -> new IllegalArgumentException("ไม่พบแม่แบบ Core"));
            if (!template.serialized() || marker.serial() == null || marker.version() != template.version()
                    || before.getType() != template.material()) {
                throw new IllegalArgumentException("แม่แบบ/เวอร์ชัน/material/serial ของ Core ไม่ตรง");
            }
            identity = new ItemIdentityPolicy.Identity("core", marker.templateId(), marker.version(), marker.serial());
            baseline = templates.create(template, marker.serial());
            if (!uniqueInInventory(player, identity)) {
                throw new IllegalArgumentException("พบ serial ซ้ำใน inventory — โปรดแจ้งทีมงาน");
            }
        } else {
            identity = new ItemIdentityPolicy.Identity("vanilla", null, 0, null);
            baseline = new ItemStack(before.getType());
        }
        if (!before.matchesWithoutData(baseline, ALLOWED_EDITS, false)) {
            throw new IllegalArgumentException("มีข้อมูล provider/model/stats ที่ adapter รุ่นนี้ยังไม่รองรับ");
        }
        ItemStack repaired = before.clone();
        repaired.setData(DataComponentTypes.DAMAGE, 0);
        if (!before.matchesWithoutData(repaired, Set.of(DataComponentTypes.DAMAGE), false)) {
            throw new IllegalStateException("repair changed components other than damage");
        }
        return new Candidate(slot, identity, used, max, before, repaired);
    }

    public boolean uniqueInInventory(Player player, ItemIdentityPolicy.Identity identity) {
        if (identity.serial() == null) {
            return true;
        }
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && templates.identify(item).map(i -> identity.serial().equals(i.serial())).orElse(false)) {
                if (item.getAmount() != 1 || ++count > 1) {
                    return false;
                }
            }
        }
        return count == 1;
    }
}
