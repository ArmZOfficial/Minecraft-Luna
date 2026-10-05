package com.armzofficial.fantasycore.economy;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
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
 * เจ้าของยอดเงินแหล่งเดียวของเซิร์ฟ (ไม่มี Bukkit API ในคลาสนี้ เพื่อทดสอบได้ตรง ๆ)
 * <p>
 * ทุกการเปลี่ยนยอดทำใน transaction เดียว: ตรวจ op ID ซ้ำ → ตรวจยอด → update → บันทึก operation + ledger (+ audit)
 */
public final class EconomyStore {

    /** ใช้กับ {@link #transfer} เพื่อย้าย "ทั้งหมด" ที่มีอยู่ ณ ตอน commit */
    public static final long ALL = -1L;

    private final Database database;
    private final LongSupplier clock;
    private final long maxTransaction;

    public EconomyStore(Database database, LongSupplier clock, long maxTransaction) {
        this.database = database;
        this.clock = clock;
        this.maxTransaction = maxTransaction;
    }

    public Balances balances(UUID player) throws SQLException {
        return database.read(connection -> readBalances(connection, player));
    }

    /** ย้ายเงินระหว่างช่องของผู้เล่นคนเดียวกัน เช่น ฝาก/ถอนธนาคาร — ไม่สร้างเงินใหม่ */
    public TxResult transfer(UUID player, Bucket from, Bucket to, long amount, OpMeta meta) throws SQLException {
        if (from == to) {
            throw new IllegalArgumentException("from == to");
        }
        if (amount != ALL && amount <= 0) {
            return new TxResult(TxResult.Status.INVALID_AMOUNT, 0, balances(player), meta.opId());
        }
        return database.transaction(connection -> {
            if (operationExists(connection, meta.opId())) {
                return new TxResult(TxResult.Status.DUPLICATE, 0, readBalances(connection, player), meta.opId());
            }
            long now = clock.getAsLong();
            ensureAccount(connection, player, from, now);
            ensureAccount(connection, player, to, now);
            long fromBalance = balance(connection, player, from);
            long moved = amount == ALL ? fromBalance : amount;
            if (moved == 0) {
                return new TxResult(TxResult.Status.NOTHING, 0, readBalances(connection, player), meta.opId());
            }
            if (moved > maxTransaction) {
                return new TxResult(TxResult.Status.LIMIT_EXCEEDED, 0, readBalances(connection, player), meta.opId());
            }
            if (fromBalance < moved) {
                return new TxResult(TxResult.Status.INSUFFICIENT_FUNDS, 0, readBalances(connection, player), meta.opId());
            }
            long toBalance;
            try {
                toBalance = Math.addExact(balance(connection, player, to), moved);
            } catch (ArithmeticException overflow) {
                return new TxResult(TxResult.Status.LIMIT_EXCEEDED, 0, readBalances(connection, player), meta.opId());
            }
            long fromAfter = fromBalance - moved;
            setBalance(connection, player, from, fromAfter, now);
            setBalance(connection, player, to, toBalance, now);
            insertOperation(connection, meta, player, now);
            insertLedger(connection, meta, player, from, -moved, fromAfter, now);
            insertLedger(connection, meta, player, to, moved, toBalance, now);
            return new TxResult(TxResult.Status.OK, moved, readBalances(connection, player), meta.opId());
        });
    }

    /**
     * เพิ่ม/ลดยอดหนึ่งช่อง (รางวัล, ค่าบริการ, แอดมินปรับ, Vault)
     *
     * @param expectedBefore ถ้าไม่ null ยอดก่อนทำต้องเท่าค่านี้ (ใช้กับ preview → apply ของแอดมิน)
     * @param audit          ถ้าไม่ null เขียน audit ใน transaction เดียวกัน
     */
    public TxResult adjust(UUID player, Bucket bucket, long delta, Long expectedBefore, OpMeta meta, AuditEntry audit)
            throws SQLException {
        return database.transaction(connection -> adjustIn(connection, player, bucket, delta, expectedBefore, meta, audit));
    }

    /**
     * เหมือน {@link #adjust} แต่ทำใน transaction ที่ผู้เรียกเปิดอยู่แล้ว (เช่น รับรางวัลรายวัน: บันทึกสิทธิ์ + เงิน + จดหมาย พร้อมกัน)
     * ผลที่ไม่ใช่ OK ไม่เขียนอะไรนอกจากสร้างบัญชีว่าง ผู้เรียกตัดสินใจเองว่าจะ rollback ทั้งชุดหรือไม่
     */
    public TxResult adjustIn(Connection connection, UUID player, Bucket bucket, long delta, Long expectedBefore,
                             OpMeta meta, AuditEntry audit) throws SQLException {
        if (delta == 0 || delta == Long.MIN_VALUE) {
            return new TxResult(TxResult.Status.INVALID_AMOUNT, 0, readBalances(connection, player), meta.opId());
        }
        if (Math.abs(delta) > maxTransaction) {
            return new TxResult(TxResult.Status.LIMIT_EXCEEDED, 0, readBalances(connection, player), meta.opId());
        }
        if (operationExists(connection, meta.opId())) {
            return new TxResult(TxResult.Status.DUPLICATE, 0, readBalances(connection, player), meta.opId());
        }
        long now = clock.getAsLong();
        ensureAccount(connection, player, bucket, now);
        long before = balance(connection, player, bucket);
        if (expectedBefore != null && before != expectedBefore) {
            return new TxResult(TxResult.Status.BALANCE_CHANGED, 0, readBalances(connection, player), meta.opId());
        }
        long after;
        try {
            after = Math.addExact(before, delta);
        } catch (ArithmeticException overflow) {
            return new TxResult(TxResult.Status.LIMIT_EXCEEDED, 0, readBalances(connection, player), meta.opId());
        }
        if (after < 0) {
            return new TxResult(TxResult.Status.INSUFFICIENT_FUNDS, 0, readBalances(connection, player), meta.opId());
        }
        setBalance(connection, player, bucket, after, now);
        insertOperation(connection, meta, player, now);
        insertLedger(connection, meta, player, bucket, delta, after, now);
        if (audit != null) {
            AuditStore.insert(connection, audit, meta.opId(), now);
        }
        return new TxResult(TxResult.Status.OK, Math.abs(delta), readBalances(connection, player), meta.opId());
    }

    /** อ่านยอดภายใน transaction ที่เปิดอยู่ */
    public Balances balancesIn(Connection connection, UUID player) throws SQLException {
        return readBalances(connection, player);
    }

    /** หักทองที่พกตามเปอร์เซ็นต์เมื่อตาย; เงินฝากและเงินแดงไม่ถูกแตะ */
    public TxResult deathLoss(UUID player, int percent, OpMeta meta) throws SQLException {
        return database.transaction(connection -> {
            if (operationExists(connection, meta.opId())) {
                return new TxResult(TxResult.Status.DUPLICATE, 0, readBalances(connection, player), meta.opId());
            }
            long now = clock.getAsLong();
            ensureAccount(connection, player, Bucket.GOLD_WALLET, now);
            long before = balance(connection, player, Bucket.GOLD_WALLET);
            long loss = DeathPolicy.loss(before, percent);
            if (loss == 0) {
                return new TxResult(TxResult.Status.NOTHING, 0, readBalances(connection, player), meta.opId());
            }
            long after = before - loss;
            setBalance(connection, player, Bucket.GOLD_WALLET, after, now);
            insertOperation(connection, meta, player, now);
            insertLedger(connection, meta, player, Bucket.GOLD_WALLET, -loss, after, now);
            return new TxResult(TxResult.Status.OK, loss, readBalances(connection, player), meta.opId());
        });
    }

    public List<LedgerEntry> history(UUID player, int limit) throws SQLException {
        return database.read(connection -> {
            List<LedgerEntry> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, op_id, bucket, delta, balance_after, reason, ref, created_at FROM ledger "
                            + "WHERE player_uuid = ? ORDER BY id DESC LIMIT ?")) {
                ps.setString(1, player.toString());
                ps.setInt(2, Math.max(1, Math.min(limit, 100)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LedgerEntry(rs.getLong(1), rs.getString(2), Bucket.fromKey(rs.getString(3)),
                                rs.getLong(4), rs.getLong(5), rs.getString(6), rs.getString(7), rs.getLong(8)));
                    }
                }
            }
            return entries;
        });
    }

    // ---------------------------------------------------------------- SQL helpers

    private static Balances readBalances(Connection connection, UUID player) throws SQLException {
        long gold = 0;
        long bank = 0;
        long red = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT bucket, balance FROM accounts WHERE player_uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    switch (Bucket.fromKey(rs.getString(1))) {
                        case GOLD_WALLET -> gold = rs.getLong(2);
                        case GOLD_BANK -> bank = rs.getLong(2);
                        case RED_WALLET -> red = rs.getLong(2);
                    }
                }
            }
        }
        return new Balances(gold, bank, red);
    }

    private static boolean operationExists(Connection connection, String opId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM operations WHERE op_id = ?")) {
            ps.setString(1, opId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void ensureAccount(Connection connection, UUID player, Bucket bucket, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO accounts(player_uuid, bucket, balance, updated_at) VALUES (?, ?, 0, ?)")) {
            ps.setString(1, player.toString());
            ps.setString(2, bucket.key());
            ps.setLong(3, now);
            ps.executeUpdate();
        }
    }

    private static long balance(Connection connection, UUID player, Bucket bucket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM accounts WHERE player_uuid = ? AND bucket = ?")) {
            ps.setString(1, player.toString());
            ps.setString(2, bucket.key());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private static void setBalance(Connection connection, UUID player, Bucket bucket, long value, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE accounts SET balance = ?, updated_at = ? WHERE player_uuid = ? AND bucket = ?")) {
            ps.setLong(1, value);
            ps.setLong(2, now);
            ps.setString(3, player.toString());
            ps.setString(4, bucket.key());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("ไม่พบบัญชี " + player + "/" + bucket.key());
            }
        }
    }

    private static void insertOperation(Connection connection, OpMeta meta, UUID player, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO operations(op_id, kind, player_uuid, actor, status, detail, created_at) "
                        + "VALUES (?, ?, ?, ?, 'COMMITTED', ?, ?)")) {
            ps.setString(1, meta.opId());
            ps.setString(2, meta.kind());
            ps.setString(3, player.toString());
            ps.setString(4, meta.actor());
            ps.setString(5, meta.reason());
            ps.setLong(6, now);
            ps.executeUpdate();
        }
    }

    private static void insertLedger(Connection connection, OpMeta meta, UUID player, Bucket bucket, long delta,
                                     long after, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ledger(op_id, player_uuid, bucket, delta, balance_after, reason, ref, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, meta.opId());
            ps.setString(2, player.toString());
            ps.setString(3, bucket.key());
            ps.setLong(4, delta);
            ps.setLong(5, after);
            ps.setString(6, meta.kind() + ": " + meta.reason());
            ps.setString(7, meta.ref());
            ps.setLong(8, now);
            ps.executeUpdate();
        }
    }
}
