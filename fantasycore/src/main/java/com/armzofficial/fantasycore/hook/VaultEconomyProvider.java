package com.armzofficial.fantasycore.hook;

import com.armzofficial.fantasycore.economy.EconomyService;
import com.armzofficial.fantasycore.economy.TxResult;
import com.armzofficial.fantasycore.util.Money;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * เปิดเฉพาะ gold.wallet ผ่าน Vault — เงินฝากและเงินแดงไม่ถูกปลั๊กอินอื่นแตะผ่าน Vault
 * <p>
 * กติกาปัดเศษ (เงิน FantasyCore เป็นจำนวนเต็ม): deposit ปัดลง, withdraw ปัดขึ้น
 * ปลั๊กอินอื่นจึงไม่สามารถสร้างเงินจากเศษทศนิยมได้
 */
final class VaultEconomyProvider implements Economy {

    private final Plugin plugin;
    private final EconomyService economy;
    private final String singular;

    VaultEconomyProvider(Plugin plugin, EconomyService economy, String singular) {
        this.plugin = plugin;
        this.economy = economy;
        this.singular = singular;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "FantasyCore";
    }

    @Override
    public boolean hasBankSupport() {
        // ธนาคารของ Luma เป็นบัญชีส่วนตัวใน Core ไม่ใช่ shared bank แบบ Vault
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        return Money.format((long) Math.floor(amount)) + " " + singular;
    }

    @Override
    public String currencyNamePlural() {
        return singular;
    }

    @Override
    public String currencyNameSingular() {
        return singular;
    }

    // ------------------------------------------------------------ accounts

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        return resolve(playerName) != null;
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) {
        return resolve(playerName) != null;
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    // ------------------------------------------------------------ balance

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) {
            return 0;
        }
        // ปลั๊กอินแสดงผล (TAB/scoreboard) เรียกถี่มาก: ผู้เล่นออนไลน์ใช้ cache ที่อัปเดตทุก commit
        var cached = economy.cached(player.getUniqueId());
        if (cached.isPresent()) {
            return cached.get().gold();
        }
        return databaseGold(player);
    }

    private double databaseGold(OfflinePlayer player) {
        try {
            return economy.balancesSync(player.getUniqueId()).gold();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Vault getBalance ล้มเหลว", e);
            return 0;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        return getBalance(resolve(playerName));
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!Double.isFinite(amount) || player == null) {
            return false;
        }
        // เป็นการตัดสินใจเรื่องเงิน จึงอ่านฐานข้อมูลจริง (withdraw ยังตรวจซ้ำแบบ atomic อีกชั้น)
        return databaseGold(player) >= Math.ceil(amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        return has(resolve(playerName), amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    // ------------------------------------------------------------ withdraw / deposit

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return fail(amount, 0, "ไม่พบผู้เล่น");
        }
        if (!Double.isFinite(amount) || amount < 0) {
            return fail(amount, getBalance(player), "จำนวนเงินไม่ถูกต้อง");
        }
        long whole = (long) Math.ceil(amount);
        if (whole == 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        }
        try {
            TxResult result = economy.adjustSync(player.getUniqueId(), -whole, "vault.withdraw");
            return toResponse(result, whole);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Vault withdraw ล้มเหลว", e);
            return fail(amount, 0, "ระบบเงินขัดข้อง");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(resolve(playerName), amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return fail(amount, 0, "ไม่พบผู้เล่น");
        }
        if (!Double.isFinite(amount) || amount < 0) {
            return fail(amount, getBalance(player), "จำนวนเงินไม่ถูกต้อง");
        }
        long whole = (long) Math.floor(amount);
        if (whole == 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        }
        try {
            TxResult result = economy.adjustSync(player.getUniqueId(), whole, "vault.deposit");
            return toResponse(result, whole);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Vault deposit ล้มเหลว", e);
            return fail(amount, 0, "ระบบเงินขัดข้อง");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(resolve(playerName), amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    // ------------------------------------------------------------ shared banks (ไม่รองรับ)

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    @Deprecated
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    // ------------------------------------------------------------ helpers

    private static OfflinePlayer resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        OfflinePlayer online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    private static EconomyResponse toResponse(TxResult result, long whole) {
        double balance = result.after() == null ? 0 : result.after().gold();
        return switch (result.status()) {
            case OK -> new EconomyResponse(whole, balance, EconomyResponse.ResponseType.SUCCESS, null);
            case INSUFFICIENT_FUNDS -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "เงินไม่พอ");
            case LIMIT_EXCEEDED -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "เกินเพดานต่อรายการ");
            default -> new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "ทำรายการไม่ได้: " + result.status());
        };
    }

    private static EconomyResponse fail(double amount, double balance, String message) {
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, message);
    }

    private static EconomyResponse notImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "FantasyCore ไม่รองรับ shared bank ของ Vault");
    }
}
