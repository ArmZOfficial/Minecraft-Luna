package com.armzofficial.fantasycore.reward;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardAndMailStoreTest {

    private Database database;
    private EconomyStore economy;
    private MailStore mail;
    private RewardStore rewards;
    private final AtomicLong clock = new AtomicLong(10_000);
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    /** รอบตามตารางทดลองใน CASUAL-SURVIVAL §4 (ของเป็น byte สมมติ) */
    private final List<RewardStore.DayReward> cycle = List.of(
            new RewardStore.DayReward(1, 100, 0, List.of()),
            new RewardStore.DayReward(2, 150, 0, List.of(new RewardStore.RewardItem("ขนมปัง ×8", new byte[]{1, 2, 3}))),
            new RewardStore.DayReward(3, 200, 0, List.of()),
            new RewardStore.DayReward(4, 250, 0, List.of()),
            new RewardStore.DayReward(5, 300, 0, List.of(new RewardStore.RewardItem("แท่งเหล็ก ×4", new byte[]{4}))),
            new RewardStore.DayReward(6, 350, 0, List.of()),
            new RewardStore.DayReward(7, 500, 1, List.of()));

    @BeforeEach
    void setUp() throws Exception {
        database = TestDatabases.fresh();
        economy = new EconomyStore(database, clock::incrementAndGet, 1_000_000_000L);
        mail = new MailStore(database, clock::incrementAndGet);
        rewards = new RewardStore(database, economy, mail, clock::incrementAndGet);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void oncePerBangkokDay() throws Exception {
        RewardStore.ClaimResult first = rewards.claim(player, "daily", "2026-10-05", cycle);
        assertEquals(RewardStore.Status.CLAIMED, first.status());
        assertEquals(1, first.cycleIndex());
        assertEquals(new Balances(100, 0, 0), first.after());
        RewardStore.ClaimResult again = rewards.claim(player, "daily", "2026-10-05", cycle);
        assertEquals(RewardStore.Status.ALREADY_CLAIMED, again.status());
        assertEquals(100, economy.balances(player).gold());
        RewardStore.ProgramStatus status = rewards.status(player, "daily", "2026-10-05", 7);
        assertTrue(status.claimedThisPeriod());
        assertEquals(1, status.totalClaims());
        assertEquals(1, status.nextIndex());
        assertEquals(2, rewards.status(player, "daily", "2026-10-06", 7).nextIndex());
    }

    @Test
    void cycleIsCumulativeNotConsecutiveAndWraps() throws Exception {
        // ข้ามวันได้ ไม่รีเซ็ตรอบ และไม่ได้รับย้อนหลังวันที่ขาด
        String[] days = {"2026-10-01", "2026-10-03", "2026-10-09", "2026-10-10", "2026-10-20", "2026-10-21", "2026-10-22", "2026-11-01"};
        int[] expected = {1, 2, 3, 4, 5, 6, 7, 1};
        for (int i = 0; i < days.length; i++) {
            assertEquals(expected[i], rewards.claim(player, "daily", days[i], cycle).cycleIndex());
        }
        // 100+150+200+250+300+350+500+100 = 1,950 ทอง และเงินแดง 1
        assertEquals(new Balances(1_950, 0, 1), economy.balances(player));
        assertEquals(2, mail.countPending(player));
    }

    @Test
    void itemsGoToMailboxInSameTransaction() throws Exception {
        rewards.claim(player, "daily", "2026-10-01", cycle);
        RewardStore.ClaimResult day2 = rewards.claim(player, "daily", "2026-10-02", cycle);
        assertEquals(1, day2.mailIds().size());
        List<MailStore.MailItem> pending = mail.pending(player, 10);
        assertEquals(1, pending.size());
        assertEquals("reward.daily", pending.getFirst().source());
        assertEquals(day2.opId(), pending.getFirst().sourceRef());
        assertEquals(3, pending.getFirst().data().length);
    }

    @Test
    void concurrentClaimsGiveExactlyOneReward() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<RewardStore.ClaimResult>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(pool.submit(() -> rewards.claim(player, "daily", "2026-10-05", cycle)));
        }
        int claimed = 0;
        for (Future<RewardStore.ClaimResult> future : futures) {
            if (future.get().status() == RewardStore.Status.CLAIMED) {
                claimed++;
            }
        }
        pool.shutdown();
        assertEquals(1, claimed);
        assertEquals(100, economy.balances(player).gold());
    }

    @Test
    void failedCreditRollsBackTheWholeClaim() throws Exception {
        List<RewardStore.DayReward> tooBig = List.of(new RewardStore.DayReward(1, 2_000_000_000L, 0,
                List.of(new RewardStore.RewardItem("x", new byte[]{9}))));
        boolean threw = false;
        try {
            rewards.claim(player, "daily", "2026-10-05", tooBig);
        } catch (java.sql.SQLException expected) {
            threw = true;
        }
        assertTrue(threw);
        // สิทธิ์วันนี้ยังไม่ถูกใช้ ไม่มีเงิน ไม่มีของค้าง
        assertFalse(rewards.status(player, "daily", "2026-10-05", 1).claimedThisPeriod());
        assertEquals(Balances.ZERO, economy.balances(player));
        assertEquals(0, mail.countPending(player));
    }

    @Test
    void mailClaimStateMachineAndCrashQuarantine() throws Exception {
        long a = mail.enqueue(player, "test", "op-a", "A", new byte[]{1}, null);
        long b = mail.enqueue(player, "test", "op-b", "B", new byte[]{2}, null);
        // รับ A สำเร็จ
        assertTrue(mail.beginClaim(a, player, "claim-a"));
        assertFalse(mail.beginClaim(a, player, "claim-a2"));          // จองซ้ำไม่ได้
        assertFalse(mail.beginClaim(b, UUID.randomUUID(), "x"));      // คนอื่นรับของเราไม่ได้
        mail.finishClaim(a, "claim-a");
        // B เริ่มรับแล้วเซิร์ฟดับก่อนบันทึกผล
        assertTrue(mail.beginClaim(b, player, "claim-b"));
        assertEquals(1, mail.quarantineInterrupted());
        assertEquals(0, mail.countPending(player));
        assertEquals(1, mail.countReview());
        AuditEntry audit = new AuditEntry(null, "console", "mail.release", "mail#" + b, null, "ตรวจแล้วยังไม่ได้ของ");
        assertTrue(mail.resolveReview(b, true, audit));
        assertFalse(mail.resolveReview(b, true, audit));               // ตัดสินซ้ำไม่ได้
        assertEquals(1, mail.countPending(player));
        // ยกเลิกการจองคืนเป็น PENDING
        assertTrue(mail.beginClaim(b, player, "claim-b2"));
        mail.abortClaim(b, "claim-b2");
        assertEquals(1, mail.countPending(player));
    }
}
