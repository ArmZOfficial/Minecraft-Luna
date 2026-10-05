package com.armzofficial.fantasycore.exchange;

import com.armzofficial.fantasycore.audit.AuditEntry;
import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.economy.Bucket;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.economy.OpMeta;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.item.ItemInstanceStore;
import com.armzofficial.fantasycore.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Durable journal ของการตัดของ — ไม่มี Bukkit API; ผลลัพธ์ถูกตรึงก่อนตัด ไม่อ่านสูตรใหม่ตอน recovery */
public final class ExchangeStore {
    public enum Kind {
        EXCHANGE("exchange"), CRAFT("craft");
        private final String key;
        Kind(String key) { this.key = key; }
        public String key() { return key; }
    }

    public record Output(String label, byte[] data, UUID serial, String template, int version) {
        public Output(String label, byte[] data) { this(label, data, null, null, 0); }
        public Output { data = data == null ? null : data.clone(); }
        @Override public byte[] data() { return data == null ? null : data.clone(); }
    }

    public record Request(String opId, UUID player, String recipe, int version, int batch, String period,
                          int dailyLimit, String inputs, byte[] snapshot, List<Output> outputs, long goldCost) {
        public Request(String opId, UUID player, String recipe, int version, int batch, String period,
                       int dailyLimit, String inputs, byte[] snapshot, List<Output> outputs) {
            this(opId, player, recipe, version, batch, period, dailyLimit, inputs, snapshot, outputs, 0);
        }
        public Request {
            snapshot = snapshot == null ? null : snapshot.clone();
            outputs = outputs == null ? null : List.copyOf(outputs);
        }
        @Override public byte[] snapshot() { return snapshot == null ? null : snapshot.clone(); }
    }

    public enum PrepareResult { PREPARED, DUPLICATE, BUSY, QUOTA, FUNDS, LIMIT }

    public record Completion(boolean changed, List<Long> mailIds) {
    }

    public record Review(String opId, UUID player, String recipe, int version, int batch, String period,
                         String inputs, long createdAt, long goldCost, String outputs) {
    }

    public record Recovery(int cancelled, int review) {
    }

    private final Database database;
    private final MailStore mail;
    private final LongSupplier clock;
    private final EconomyStore economy;
    private final Kind kind;

    public ExchangeStore(Database database, MailStore mail, LongSupplier clock) {
        this(database, mail, clock, new EconomyStore(database, clock, Long.MAX_VALUE), Kind.EXCHANGE);
    }

    public ExchangeStore(Database database, MailStore mail, LongSupplier clock, EconomyStore economy, Kind kind) {
        this.database = database;
        this.mail = mail;
        this.clock = clock;
        this.economy = economy;
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public PrepareResult prepare(Request r) throws SQLException {
        if (r.opId() == null || r.opId().isBlank() || r.opId().length() > 100 || r.player() == null || r.recipe() == null
                || !r.recipe().matches("[a-z0-9_]{1,48}") || r.version() < 1 || r.batch() < 1 || r.batch() > 16
                || r.dailyLimit() < 1 || r.dailyLimit() > 1000 || r.period() == null
                || !r.period().matches("\\d{4}-\\d{2}-\\d{2}") || r.inputs() == null || r.snapshot() == null
                || r.snapshot().length == 0 || r.outputs() == null || r.outputs().isEmpty() || r.outputs().size() > 128
                || r.goldCost() < 0
                || r.outputs().stream().anyMatch(o -> o == null || o.label() == null || o.data() == null || o.data().length == 0)) {
            throw new IllegalArgumentException("invalid exchange request");
        }
        if (kind == Kind.CRAFT) {
            if (r.batch() != 1 || r.outputs().size() != 1 || r.outputs().getFirst().serial() == null
                    || r.outputs().getFirst().template() == null
                    || !r.outputs().getFirst().template().matches("[a-z0-9_]{1,48}") || r.outputs().getFirst().version() < 1) {
                throw new IllegalArgumentException("craft requires one serialized Core output");
            }
        } else if (r.goldCost() != 0 || r.outputs().stream().anyMatch(o -> o.serial() != null || o.template() != null || o.version() != 0)) {
            throw new IllegalArgumentException("exchange only supports free vanilla outputs");
        }
        return database.transaction(c -> {
            if (exists(c, "SELECT 1 FROM exchange_operations WHERE op_id = ?", r.opId())) {
                return PrepareResult.DUPLICATE;
            }
            if (exists(c, "SELECT 1 FROM exchange_operations WHERE player_uuid = ? AND state IN ('PREPARED','CONSUMING','REVIEW')",
                    r.player().toString())) {
                return PrepareResult.BUSY;
            }
            if (usedIn(c, r.player(), r.recipe(), r.period()) + r.batch() > r.dailyLimit()) {
                return PrepareResult.QUOTA;
            }
            if (r.goldCost() > 0) {
                TxResult charge = economy.adjustIn(c, r.player(), Bucket.GOLD_WALLET, -r.goldCost(), null,
                        new OpMeta(r.opId() + ":charge", "craft.charge", "system", "craft reservation", r.opId()), null);
                if (!charge.ok()) {
                    if (charge.status() == TxResult.Status.INSUFFICIENT_FUNDS) { return PrepareResult.FUNDS; }
                    if (charge.status() == TxResult.Status.LIMIT_EXCEEDED) { return PrepareResult.LIMIT; }
                    throw new SQLException("craft charge failed: " + charge.status());
                }
            }
            long now = clock.getAsLong();
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO exchange_operations(op_id, player_uuid, recipe_id, recipe_version, batch, period,
                    state, inputs, slot_snapshot, created_at, updated_at, kind, gold_cost)
                    VALUES (?, ?, ?, ?, ?, ?, 'PREPARED', ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, r.opId());
                ps.setString(2, r.player().toString());
                ps.setString(3, r.recipe());
                ps.setInt(4, r.version());
                ps.setInt(5, r.batch());
                ps.setString(6, r.period());
                ps.setString(7, r.inputs());
                ps.setBytes(8, r.snapshot());
                ps.setLong(9, now);
                ps.setLong(10, now);
                ps.setString(11, kind.key());
                ps.setLong(12, r.goldCost());
                ps.executeUpdate();
            }
            int ordinal = 0;
            for (Output output : r.outputs()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO exchange_outputs(op_id, ordinal, label, item_data, serial, template_id, template_version) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, r.opId());
                    ps.setInt(2, ordinal++);
                    ps.setString(3, output.label());
                    ps.setBytes(4, output.data());
                    ps.setString(5, output.serial() == null ? null : output.serial().toString());
                    ps.setString(6, output.template());
                    ps.setInt(7, output.version());
                    ps.executeUpdate();
                }
            }
            audit(c, r.opId(), r.player(), action("prepare"), r.inputs() + "; gold " + r.goldCost(), "player confirm");
            return PrepareResult.PREPARED;
        });
    }

    public boolean beginConsume(String opId) throws SQLException {
        return transition(opId, "PREPARED", "CONSUMING");
    }

    /** ใช้ได้เฉพาะเมื่อผู้เรียกยืนยันว่าการตัด inventory ยังไม่เริ่ม */
    public boolean cancelUntouched(String opId) throws SQLException {
        return database.transaction(c -> {
            boolean changed = changeIn(c, opId, "PREPARED", "CANCELLED")
                    || changeIn(c, opId, "CONSUMING", "CANCELLED");
            if (changed) {
                refundIn(c, opId);
                audit(c, opId, null, action("cancel"), "no inventory mutation", "validation changed");
            }
            return changed;
        });
    }

    /** เรียกหลังขอ saveData แล้ว; state + mailbox + audit เป็น DB transaction เดียว แต่ playerdata เป็นอีกระบบ */
    public Completion complete(String opId) throws SQLException {
        return database.transaction(c -> {
            if (!changeIn(c, opId, "CONSUMING", "COMMITTED")) {
                return new Completion(false, List.of());
            }
            List<Long> ids = enqueueIn(c, opId);
            audit(c, opId, null, action("commit"), "mail ids " + ids, "saveData returned; disk health requires monitoring");
            return new Completion(true, ids);
        });
    }

    public boolean markReview(String opId) throws SQLException {
        return transition(opId, "CONSUMING", "REVIEW");
    }

    public Recovery quarantineInterrupted() throws SQLException {
        return database.transaction(c -> {
            List<String> untouched = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT op_id FROM exchange_operations WHERE state = 'PREPARED' AND kind = ?")) {
                ps.setString(1, kind.key());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { untouched.add(rs.getString(1)); }
                }
            }
            for (String op : untouched) {
                changeIn(c, op, "PREPARED", "CANCELLED");
                refundIn(c, op);
                audit(c, op, null, action("recover_cancel"), "no inventory mutation", "startup recovery");
            }
            int cancelled = untouched.size();
            int review = recoverIn(c, "CONSUMING", "REVIEW");
            return new Recovery(cancelled, review);
        });
    }

    public Map<String, Integer> usage(UUID player, String period) throws SQLException {
        return database.read(c -> {
            Map<String, Integer> result = new HashMap<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT recipe_id, SUM(batch) FROM exchange_operations
                    WHERE player_uuid = ? AND period = ? AND kind = ? AND state <> 'CANCELLED' GROUP BY recipe_id
                    """)) {
                ps.setString(1, player.toString());
                ps.setString(2, period);
                ps.setString(3, kind.key());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getInt(2));
                    }
                }
            }
            return Map.copyOf(result);
        });
    }

    public List<Review> review(int limit) throws SQLException {
        return database.read(c -> {
            List<Review> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM exchange_operations WHERE state = 'REVIEW' AND kind = ? ORDER BY created_at LIMIT ?")) {
                ps.setString(1, kind.key());
                ps.setInt(2, Math.max(1, Math.min(limit, 100)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Review(rs.getString("op_id"), UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("recipe_id"), rs.getInt("recipe_version"), rs.getInt("batch"),
                                rs.getString("period"), rs.getString("inputs"), rs.getLong("created_at"), rs.getLong("gold_cost"), outputSummaryIn(c, rs.getString("op_id"))));
                    }
                }
            }
            return List.copyOf(rows);
        });
    }

    public boolean isReview(String opId) throws SQLException {
        return findReview(opId).isPresent();
    }

    public Optional<Review> findReview(String opId) throws SQLException {
        return database.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM exchange_operations WHERE op_id = ? AND state = 'REVIEW' AND kind = ?")) {
                ps.setString(1, opId);
                ps.setString(2, kind.key());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new Review(opId, UUID.fromString(rs.getString("player_uuid")), rs.getString("recipe_id"),
                            rs.getInt("recipe_version"), rs.getInt("batch"), rs.getString("period"), rs.getString("inputs"),
                            rs.getLong("created_at"), rs.getLong("gold_cost"), outputSummaryIn(c, opId)));
                }
            }
        });
    }

    public int countReview() throws SQLException {
        return database.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM exchange_operations WHERE state = 'REVIEW' AND kind = ?")) {
                ps.setString(1, kind.key());
                try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
            }
        });
    }

    /** complete=true เฉพาะทีมงานตรวจแล้วว่าของถูกตัดจริง; false เมื่อของไม่ถูกตัดหรือชดเชยแล้ว */
    public Completion resolveReview(String opId, boolean complete, AuditEntry entry) throws SQLException {
        if (entry == null || entry.reason() == null || entry.reason().trim().length() < 3) {
            throw new IllegalArgumentException("reason required");
        }
        return database.transaction(c -> {
            if (!changeIn(c, opId, "REVIEW", complete ? "COMMITTED" : "CANCELLED")) {
                return new Completion(false, List.of());
            }
            List<Long> ids = complete ? enqueueIn(c, opId) : List.of();
            if (!complete) { refundIn(c, opId); }
            AuditStore.insert(c, entry, opId, clock.getAsLong());
            return new Completion(true, ids);
        });
    }

    private List<Long> enqueueIn(Connection c, String opId) throws SQLException {
        UUID player;
        try (PreparedStatement ps = c.prepareStatement("SELECT player_uuid FROM exchange_operations WHERE op_id = ?")) {
            ps.setString(1, opId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("exchange operation missing");
                }
                player = UUID.fromString(rs.getString(1));
            }
        }
        List<Output> outputs = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT label, item_data, serial, template_id, template_version FROM exchange_outputs WHERE op_id = ? ORDER BY ordinal")) {
            ps.setString(1, opId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    outputs.add(new Output(rs.getString(1), rs.getBytes(2), rs.getString(3) == null ? null : UUID.fromString(rs.getString(3)),
                            rs.getString(4), rs.getInt(5)));
                }
            }
        }
        if (outputs.isEmpty()) {
            throw new SQLException("exchange outputs missing");
        }
        List<Long> ids = new ArrayList<>();
        for (int ordinal = 0; ordinal < outputs.size(); ordinal++) {
            Output output = outputs.get(ordinal);
            if (output.serial() != null) {
                ItemInstanceStore.issueIn(c, output.serial(), output.template(), output.version(), player, opId,
                        "craft", "MAILED", clock.getAsLong());
            }
            long id = mail.enqueueIn(c, player, kind == Kind.CRAFT ? "craft" : "quest.exchange", opId, output.label(), output.data());
            ids.add(id);
            try (PreparedStatement ps = c.prepareStatement("UPDATE exchange_outputs SET mail_id = ? WHERE op_id = ? AND ordinal = ?")) {
                ps.setLong(1, id);
                ps.setString(2, opId);
                ps.setInt(3, ordinal);
                ps.executeUpdate();
            }
        }
        return List.copyOf(ids);
    }

    private boolean transition(String opId, String from, String to) throws SQLException {
        return database.transaction(c -> changeIn(c, opId, from, to));
    }

    private boolean changeIn(Connection c, String opId, String from, String to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE exchange_operations SET state = ?, updated_at = ? WHERE op_id = ? AND state = ? AND kind = ?")) {
            ps.setString(1, to);
            ps.setLong(2, clock.getAsLong());
            ps.setString(3, opId);
            ps.setString(4, from);
            ps.setString(5, kind.key());
            return ps.executeUpdate() == 1;
        }
    }

    private int recoverIn(Connection c, String from, String to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE exchange_operations SET state = ?, updated_at = ? WHERE state = ? AND kind = ?")) {
            ps.setString(1, to);
            ps.setLong(2, clock.getAsLong());
            ps.setString(3, from);
            ps.setString(4, kind.key());
            return ps.executeUpdate();
        }
    }

    private int usedIn(Connection c, UUID player, String recipe, String period) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(batch), 0) FROM exchange_operations
                WHERE player_uuid = ? AND recipe_id = ? AND period = ? AND kind = ? AND state <> 'CANCELLED'
                """)) {
            ps.setString(1, player.toString());
            ps.setString(2, recipe);
            ps.setString(3, period);
            ps.setString(4, kind.key());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean exists(Connection c, String sql, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void audit(Connection c, String opId, UUID player, String action, String detail, String reason) throws SQLException {
        AuditStore.insert(c, new AuditEntry(player == null ? null : player.toString(), "system", action,
                player == null ? opId : player.toString(), detail, reason), opId, clock.getAsLong());
    }

    private String action(String suffix) {
        return (kind == Kind.CRAFT ? "craft." : "quest.exchange.") + suffix;
    }

    private static String outputSummaryIn(Connection c, String opId) throws SQLException {
        List<String> labels = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT label, serial, template_id, template_version FROM exchange_outputs WHERE op_id = ? ORDER BY ordinal")) {
            ps.setString(1, opId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    labels.add(rs.getString(1) + (rs.getString(2) == null ? "" : " · " + rs.getString(3) + " v" + rs.getInt(4) + " #" + rs.getString(2)));
                }
            }
        }
        return String.join(" + ", labels);
    }

    private void refundIn(Connection c, String opId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT player_uuid, gold_cost FROM exchange_operations WHERE op_id = ? AND kind = ?")) {
            ps.setString(1, opId);
            ps.setString(2, kind.key());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong(2) == 0) { return; }
                TxResult refund = economy.adjustIn(c, UUID.fromString(rs.getString(1)), Bucket.GOLD_WALLET, rs.getLong(2), null,
                        new OpMeta(opId + ":refund", "craft.refund", "system", "cancel untouched craft", opId), null);
                if (!refund.ok()) { throw new SQLException("craft refund failed: " + refund.status()); }
            }
        }
    }
}
