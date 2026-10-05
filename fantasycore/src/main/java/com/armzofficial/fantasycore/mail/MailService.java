package com.armzofficial.fantasycore.mail;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ส่งของเข้า inventory หรือกล่องจดหมาย — ไม่ทิ้งของลงพื้นโดยไม่ตั้งใจ (SERVER-SYSTEMS §6 ข้อ 6)
 * งาน inventory ทำบน main thread เท่านั้น งานฐานข้อมูลทำบน thread DB
 */
public final class MailService {

    /** ผลการรับของจากกล่อง */
    public record ClaimSummary(int claimed, int remaining, boolean stoppedByFullInventory) {
    }

    private final Logger log;
    private final Messages messages;
    private final MailStore store;
    private final Database database;
    private final Tasks tasks;

    public MailService(Logger log, Messages messages, MailStore store, Database database, Tasks tasks) {
        this.log = log;
        this.messages = messages;
        this.store = store;
        this.database = database;
        this.tasks = tasks;
    }

    public MailStore store() {
        return store;
    }

    public CompletableFuture<Integer> countPending(UUID player) {
        return database.async(() -> store.countPending(player));
    }

    public CompletableFuture<List<MailStore.MailItem>> pending(UUID player) {
        return database.async(() -> store.pending(player, 54));
    }

    /** ส่งไอเทมให้คนออนไลน์: ใส่ inventory ได้เท่าไหร่ใส่ ส่วนที่เหลือเข้ากล่องจดหมาย (main thread) */
    public void deliverOrMail(Player player, List<ItemStack> items, String source, String sourceRef, String label,
                             AuditEntry audit, Consumer<Integer> mailedCount) {
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack item : items) {
            if (fits(player.getInventory(), item)) {
                overflow.addAll(player.getInventory().addItem(item.clone()).values());
            } else {
                overflow.add(item.clone());
            }
        }
        mailToPlayer(player.getUniqueId(), overflow, source, sourceRef, label, audit, mailedCount);
    }

    /** ใส่ของลงกล่องของผู้เล่น (ออนไลน์หรือไม่ก็ได้) */
    public void mailToPlayer(UUID player, List<ItemStack> items, String source, String sourceRef, String label,
                             AuditEntry audit, Consumer<Integer> mailedCount) {
        if (items.isEmpty()) {
            mailedCount.accept(0);
            return;
        }
        List<byte[]> data = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (ItemStack item : items) {
            data.add(item.serializeAsBytes());
            labels.add(label != null ? label : describe(item));
        }
        tasks.then(database.async(() -> {
            for (int i = 0; i < data.size(); i++) {
                store.enqueue(player, source, sourceRef, labels.get(i), data.get(i), i == 0 ? audit : null);
            }
            return data.size();
        }), (count, error) -> mailedCount.accept(error == null ? count : -1));
    }

    /** รับทุกชิ้นที่ PENDING ตามลำดับ หยุดเมื่อกระเป๋าเต็ม */
    public void claimAll(Player player, Consumer<ClaimSummary> done) {
        claimMatching(player, null, done);
    }

    /** รับเฉพาะรายการ id ที่ระบุ (เช่น ของจากรางวัลที่เพิ่งได้) */
    public void claimIds(Player player, Collection<Long> ids, Consumer<ClaimSummary> done) {
        claimMatching(player, List.copyOf(ids), done);
    }

    private void claimMatching(Player player, List<Long> ids, Consumer<ClaimSummary> done) {
        UUID id = player.getUniqueId();
        tasks.then(pending(id), (items, error) -> {
            if (error != null) {
                messages.send(player, "common.storage-error");
                done.accept(new ClaimSummary(0, 0, false));
                return;
            }
            List<MailStore.MailItem> queue = new ArrayList<>();
            for (MailStore.MailItem item : items) {
                if (ids == null || ids.contains(item.id())) {
                    queue.add(item);
                }
            }
            claimNext(player, queue, 0, 0, done);
        });
    }

    private void claimNext(Player player, List<MailStore.MailItem> queue, int index, int claimed, Consumer<ClaimSummary> done) {
        if (!player.isOnline()) {
            return;
        }
        if (index >= queue.size()) {
            done.accept(new ClaimSummary(claimed, 0, false));
            return;
        }
        MailStore.MailItem item = queue.get(index);
        claimOne(player, item, result -> {
            switch (result) {
                case CLAIMED -> claimNext(player, queue, index + 1, claimed + 1, done);
                case SKIPPED -> claimNext(player, queue, index + 1, claimed, done);
                case FULL -> done.accept(new ClaimSummary(claimed, queue.size() - index, true));
                case ERROR -> done.accept(new ClaimSummary(claimed, queue.size() - index, false));
            }
        });
    }

    public enum ClaimResult {
        CLAIMED, SKIPPED, FULL, ERROR
    }

    /**
     * รับหนึ่งรายการ: ตรวจที่ว่าง → จอง (CLAIMING) → ตรวจที่ว่างซ้ำ → ใส่ inventory → CLAIMED
     * ถ้าบันทึก CLAIMED ไม่สำเร็จ รายการจะค้าง CLAIMING และถูกย้ายไป REVIEW ตอนเปิดเซิร์ฟครั้งถัดไป (ไม่ปล่อยของซ้ำ)
     */
    public void claimOne(Player player, MailStore.MailItem item, Consumer<ClaimResult> done) {
        ItemStack stack;
        try {
            stack = ItemStack.deserializeBytes(item.data());
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "อ่านไอเทมในจดหมาย #" + item.id() + " ไม่ได้ (รุ่นข้อมูลไม่รองรับ?)", e);
            messages.send(player, "mail.corrupt", Messages.p("id", item.id()));
            done.accept(ClaimResult.ERROR);
            return;
        }
        if (!fits(player.getInventory(), stack)) {
            done.accept(ClaimResult.FULL);
            return;
        }
        String op = OpMeta.newOpId();
        UUID playerId = player.getUniqueId();
        tasks.then(database.async(() -> store.beginClaim(item.id(), playerId, op)), (reserved, error) -> {
            if (error != null) {
                messages.send(player, "common.storage-error");
                done.accept(ClaimResult.ERROR);
                return;
            }
            if (!Boolean.TRUE.equals(reserved)) {
                done.accept(ClaimResult.SKIPPED); // ถูกรับจากอีกหน้าต่าง/คำสั่งแล้ว
                return;
            }
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !fits(online.getInventory(), stack)) {
                tasks.then(database.async(() -> {
                    store.abortClaim(item.id(), op);
                    return null;
                }), (x, e) -> done.accept(online == null ? ClaimResult.ERROR : ClaimResult.FULL));
                return;
            }
            HashMap<Integer, ItemStack> leftover = online.getInventory().addItem(stack);
            List<ItemStack> rest = new ArrayList<>(leftover.values());
            tasks.then(database.async(() -> {
                store.finishClaim(item.id(), op);
                for (ItemStack extra : rest) {
                    // ไม่ควรเกิดเพราะตรวจที่ว่างแล้วใน tick เดียวกัน แต่ถ้าเกิด ส่วนเกินกลับเข้ากล่อง ไม่ตกพื้น
                    store.enqueue(playerId, item.source(), item.sourceRef(), item.label(), extra.serializeAsBytes(), null);
                }
                return null;
            }), (x, finishError) -> {
                if (finishError != null) {
                    log.severe("ส่งของจดหมาย #" + item.id() + " ให้ " + online.getName()
                            + " แล้ว แต่บันทึกสถานะไม่สำเร็จ — จะถูกย้ายไป REVIEW ตอนเปิดเซิร์ฟครั้งถัดไป");
                }
                done.accept(ClaimResult.CLAIMED);
            });
        });
    }

    /** มีที่พอใส่ item ทั้งก้อนหรือไม่ (นับช่องว่าง + ช่องที่ซ้อนได้ใน 36 ช่องหลัก) */
    public static boolean fits(PlayerInventory inventory, ItemStack item) {
        int need = item.getAmount();
        int max = item.getMaxStackSize();
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.isEmpty()) {
                need -= max;
            } else if (slot.isSimilar(item)) {
                need -= Math.max(0, max - slot.getAmount());
            }
            if (need <= 0) {
                return true;
            }
        }
        return need <= 0;
    }

    public static String describe(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        return name + " ×" + item.getAmount();
    }
}
