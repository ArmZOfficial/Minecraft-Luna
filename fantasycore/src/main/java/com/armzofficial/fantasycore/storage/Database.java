package com.armzofficial.fantasycore.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * SQLite หนึ่ง connection ที่ทุกการเข้าถึงต้องผ่าน lock เดียว
 * <p>
 * - งานจากผู้เล่น/คำสั่งเรียก {@link #async} ให้รันบน thread "FantasyCore-DB"
 * - API แบบ synchronous (เช่น Vault) เรียก {@link #transaction} ตรงได้ เพราะ lock คุมลำดับให้
 * - เงินเก็บเป็น INTEGER เท่านั้น ไม่มี floating point
 */
public final class Database implements AutoCloseable {

    private final Connection connection;
    private final ExecutorService executor;
    private final Object lock = new Object();
    private volatile boolean closed;

    private Database(Connection connection) {
        this.connection = connection;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FantasyCore-DB");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static Database open(Path file) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("ไม่พบ SQLite JDBC driver (Paper ควรมีมาให้)", e);
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new SQLException("สร้างโฟลเดอร์ฐานข้อมูลไม่ได้: " + file, e);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        }
        connection.setAutoCommit(true);
        return new Database(connection);
    }

    /** อ่านหรือเขียนคำสั่งเดียวแบบ auto-commit */
    public <T> T read(SqlFunction<Connection, T> work) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            return work.apply(connection);
        }
    }

    /** ทำงานทั้งหมดใน transaction เดียว; error ใด ๆ จะ rollback ทั้งชุด */
    public <T> T transaction(SqlFunction<Connection, T> work) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** รันงานฐานข้อมูลนอก main thread แล้วคืน future (ผลลัพธ์ยังอยู่บน thread DB) */
    public <T> CompletableFuture<T> async(SqlCallable<T> work) {
        return CompletableFuture.supplyAsync(unchecked(work), executor);
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() throws SQLException {
        if (closed) {
            throw new SQLException("ฐานข้อมูลปิดแล้ว");
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (lock) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ปิดตอน shutdown แล้ว ไม่มีอะไรให้กู้
            }
        }
    }

    private static <T> Supplier<T> unchecked(SqlCallable<T> work) {
        return () -> {
            try {
                return work.call();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        };
    }

    @FunctionalInterface
    public interface SqlCallable<T> {
        T call() throws SQLException;
    }
}
