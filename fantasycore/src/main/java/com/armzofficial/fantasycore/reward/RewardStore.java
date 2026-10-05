package com.armzofficial.fantasycore.reward;

import com.armzofficial.fantasycore.economy.Balances;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * รางวัลรายวันแบบรอบสะสม (CASUAL-SURVIVAL §4) — ไม่มี Bukkit API เพื่อทดสอบได้ตรง ๆ
 * <p>
 * รับได้ 1 ครั้งต่อ (ผู้เล่น, program, วันตามเวลาไทย) บังคับด้วย PRIMARY KEY
 * สิทธิ์ + เงิน + ของในกล่องจดหมาย commit ใน transaction เดียว: สำเร็จทั้งหมดหรือไม่เกิดอะไรเลย
 * ของไม่ใส่ inventory ตรงจากที่นี่ — ไปรอในกล่องจดหมายแล้วค่อยส่งต่อ ทำให้ inventory เต็ม/เซิร์ฟดับไม่ทำของหาย
 */
public final class RewardStore {

    /** ของหนึ่งชิ้นในรางวัล: label สำหรับแสดงผล + ItemStack ที่ serialize แล้ว */
    public record RewardItem(String label, byte[] data) {
    }

    /** รางวัลของครั้งที่ index (เริ่ม 1) ในรอบ */
    public record DayReward(int index, long gold, long red, List<RewardItem> items) {
    }

    public enum Status {
        CLAIMED,
        ALREADY_CLAIMED,
        EMPTY_PROGRAM
    }

    public record ClaimResult(Status status, int cycleIndex, DayReward reward, Balances after, List<Long> mailIds,
                              String opId) {
    }

    /**
     * @param totalClaims จำนวนครั้งที่เคยรับทั้งหมด
     * @param nextIndex   ครั้งที่จะได้รับต่อไปในรอบ (เริ่ม 1)
     */
    public record ProgramStatus(int totalClaims, boolean claimedThisPeriod, int nextIndex) {
    }

    private final Database database;
    private final EconomyStore economy;
    private final MailStore mail;
    private final LongSupplier clock;

    public RewardStore(Database database, EconomyStore economy, MailStore mail, LongSupplier clock) {
        this.database = database;
        this.economy = economy;
        this.mail = mail;
        this.clock = clock;
    }

    public ProgramStatus status(UUID player, String program, String period, int cycleLength) throws SQLException {
        return database.read(connection -> {
            int total = countClaims(connection, player, program);
            boolean claimed = claimedIn(connection, player, program, period);
            int next = claimed ? (total - 1) % cycleLength + 1 : total % cycleLength + 1;
            return new ProgramStatus(total, claimed, next);
        });
    }

    public ClaimResult claim(UUID player, String program, String period, List<DayReward> cycle) throws SQLException {
        if (cycle.isEmpty()) {
            return new ClaimResult(Status.EMPTY_PROGRAM, 0, null, null, List.of(), null);
        }
        String opId = OpMeta.newOpId();
        return database.transaction(connection -> {
            if (claimedIn(connection, player, program, period)) {
                return new ClaimResult(Status.ALREADY_CLAIMED, 0, null, economy.balancesIn(connection, player), List.of(), null);
            }
            int index = countClaims(connection, player, program) % cycle.size() + 1;
            DayReward reward = cycle.get(index - 1);
            long now = clock.getAsLong();
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO reward_claims(player_uuid, program, period, cycle_index, op_id, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, player.toString());
                ps.setString(2, program);
                ps.setString(3, period);
                ps.setInt(4, index);
                ps.setString(5, opId);
                ps.setLong(6, now);
                ps.executeUpdate();
            }
            String reason = "รางวัล " + program + " ครั้งที่ " + index + " (" + period + ")";
            credit(connection, player, Bucket.GOLD_WALLET, reward.gold(), opId + ":gold", program, reason, period);
            credit(connection, player, Bucket.RED_WALLET, reward.red(), opId + ":red", program, reason, period);
            List<Long> mailIds = new ArrayList<>();
            for (RewardItem item : reward.items()) {
                mailIds.add(mail.enqueueIn(connection, player, "reward." + program, opId, item.label(), item.data()));
            }
            return new ClaimResult(Status.CLAIMED, index, reward, economy.balancesIn(connection, player), mailIds, opId);
        });
    }

    private void credit(Connection connection, UUID player, Bucket bucket, long amount, String opId, String program,
                        String reason, String period) throws SQLException {
        if (amount <= 0) {
            return;
        }
        TxResult result = economy.adjustIn(connection, player, bucket, amount, null,
                new OpMeta(opId, "reward." + program, "system", reason, period), null);
        if (!result.ok()) {
            // โยน error เพื่อ rollback ทั้ง transaction (สิทธิ์ไม่ถูกใช้ ไม่มีของค้าง)
            throw new SQLException("ให้รางวัล " + bucket.key() + " ไม่สำเร็จ: " + result.status());
        }
    }

    private static int countClaims(Connection connection, UUID player, String program) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM reward_claims WHERE player_uuid = ? AND program = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, program);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean claimedIn(Connection connection, UUID player, String program, String period) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM reward_claims WHERE player_uuid = ? AND program = ? AND period = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, program);
            ps.setString(3, period);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
