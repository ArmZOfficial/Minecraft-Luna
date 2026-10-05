package com.armzofficial.fantasycore;

import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.claim.ClaimAdapter;
import com.armzofficial.fantasycore.claim.NoClaimAdapter;
import com.armzofficial.fantasycore.claim.WorldGuardClaimAdapter;
import com.armzofficial.fantasycore.command.AdminCommand;
import com.armzofficial.fantasycore.command.CoreCommand;
import com.armzofficial.fantasycore.command.PlayerCommands;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.combat.DepthMonsterService;
import com.armzofficial.fantasycore.dungeon.DungeonService;
import com.armzofficial.fantasycore.dungeon.DungeonStore;
import com.armzofficial.fantasycore.dungeon.DungeonGroupStore;
import com.armzofficial.fantasycore.dungeon.DungeonProtection;
import com.armzofficial.fantasycore.economy.DeathListener;
import com.armzofficial.fantasycore.economy.EconomyService;
import com.armzofficial.fantasycore.economy.EconomyStore;
import com.armzofficial.fantasycore.exchange.ExchangeService;
import com.armzofficial.fantasycore.exchange.ExchangeStore;
import com.armzofficial.fantasycore.hook.CitizensBridge;
import com.armzofficial.fantasycore.hook.PlaceholderHook;
import com.armzofficial.fantasycore.hook.VaultHook;
import com.armzofficial.fantasycore.home.HomeService;
import com.armzofficial.fantasycore.home.HomeStore;
import com.armzofficial.fantasycore.item.ItemInstanceStore;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import com.armzofficial.fantasycore.item.ItemAdapter;
import com.armzofficial.fantasycore.repair.RepairService;
import com.armzofficial.fantasycore.repair.RepairStore;
import com.armzofficial.fantasycore.repair.NativeRepairListener;
import com.armzofficial.fantasycore.mail.MailService;
import com.armzofficial.fantasycore.mail.MailStore;
import com.armzofficial.fantasycore.menu.MenuListener;
import com.armzofficial.fantasycore.reward.RewardService;
import com.armzofficial.fantasycore.reward.RewardStore;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.station.StationListener;
import com.armzofficial.fantasycore.station.StationService;
import com.armzofficial.fantasycore.station.StationStore;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.Migrations;
import com.armzofficial.fantasycore.storage.PlayerStore;
import com.armzofficial.fantasycore.travel.CombatTracker;
import com.armzofficial.fantasycore.travel.LandingValidator;
import com.armzofficial.fantasycore.travel.RtpService;
import com.armzofficial.fantasycore.travel.TeleportService;
import com.armzofficial.fantasycore.travel.TravelStore;
import com.armzofficial.fantasycore.util.Tasks;
import com.armzofficial.fantasycore.world.WorldService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;

/**
 * FantasyCore — แกนระบบของ Luma (SERVER-SYSTEMS-PLAN §6)
 * v0.1 = PoC: economy/bank/ledger/death + เมนู + NPC สถานี + item template + home + RTP + claim adapter
 * <p>
 * หลักการ fail closed: ถ้าเปิดฐานข้อมูลไม่ได้ ปลั๊กอินปิดตัวเอง ไม่ให้บริการเงินแบบครึ่ง ๆ กลาง ๆ
 */
public final class FantasyCorePlugin extends JavaPlugin {

    private Database database;
    private Services services;
    private TeleportService teleports;
    private boolean vaultHooked;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfMissing("messages_th.yml");
        saveIfMissing("items.yml");
        saveIfMissing("exchanges.yml");
        saveIfMissing("repair.yml");
        saveIfMissing("crafting.yml");
        saveIfMissing("monsters.yml");
        saveIfMissing("dungeons.yml");

        Settings settings = Settings.load(getConfig(), getLogger());
        Messages messages = Messages.load(this);
        Tasks tasks = new Tasks(this);

        try {
            database = Database.open(new File(getDataFolder(), settings.databaseFile()).toPath());
            int version = Migrations.apply(database);
            getLogger().info("ฐานข้อมูลพร้อม (schema v" + version + ")");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "เปิดฐานข้อมูลไม่ได้ — ปิด FantasyCore เพื่อไม่ให้ระบบเงินทำงานผิด", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerStore players = new PlayerStore(database);
        AuditStore audit = new AuditStore(database, System::currentTimeMillis);
        EconomyStore economyStore = new EconomyStore(database, System::currentTimeMillis, settings.maxTransaction());
        EconomyService economy = new EconomyService(database, economyStore);
        MailStore mailStore = new MailStore(database, System::currentTimeMillis);
        DungeonStore dungeonStore = new DungeonStore(database, mailStore, System::currentTimeMillis);
        DungeonGroupStore dungeonGroups = new DungeonGroupStore(database, mailStore, dungeonStore, System::currentTimeMillis);
        ExchangeStore exchangeStore = new ExchangeStore(database, mailStore, System::currentTimeMillis);
        ExchangeStore craftStore = new ExchangeStore(database, mailStore, System::currentTimeMillis, economyStore, ExchangeStore.Kind.CRAFT);
        RepairStore repairStore = new RepairStore(database, economyStore, System::currentTimeMillis);
        try {
            int abortedDungeons = dungeonGroups.recoverInterrupted();
            if (abortedDungeons > 0) { getLogger().warning("ยกเลิกรอบดันฝึกค้าง " + abortedDungeons + " รอบ — เก็บจุดกลับและ mail ที่ commit แล้วไว้"); }
            int interrupted = mailStore.quarantineInterrupted();
            ExchangeStore.Recovery recovery = exchangeStore.quarantineInterrupted();
            ExchangeStore.Recovery craftRecovery = craftStore.quarantineInterrupted();
            if (craftRecovery.cancelled() + craftRecovery.review() > 0) {
                getLogger().warning("Craft recovery: ยกเลิก/คืนเงินก่อนตัด " + craftRecovery.cancelled()
                        + ", ค้างระหว่างตัด " + craftRecovery.review() + " → /fa craft review");
            }
            RepairStore.Recovery repairRecovery = repairStore.quarantineInterrupted();
            if (repairRecovery.refunded() + repairRecovery.review() > 0) {
                getLogger().warning("Repair recovery: คืนค่าจองก่อนซ่อม " + repairRecovery.refunded()
                        + ", ค้างระหว่างซ่อม " + repairRecovery.review() + " → /fa repair review");
            }
            if (recovery.cancelled() + recovery.review() > 0) {
                getLogger().warning("Exchange recovery: ยกเลิกก่อนตัด " + recovery.cancelled()
                        + ", ค้างระหว่างตัด " + recovery.review() + " → /fa exchange review");
            }
            if (interrupted > 0) {
                getLogger().warning("จดหมาย " + interrupted + " รายการค้างกลางการส่งจากรอบก่อน → ย้ายไป REVIEW (ตรวจด้วย /fa mail review)");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ตรวจกล่องจดหมาย/การแลก/การคราฟต์/การซ่อมค้างไม่ได้ — ปิด FantasyCore", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MailService mail = new MailService(getLogger(), messages, mailStore, database, tasks);
        ExchangeService exchange = new ExchangeService(this, messages, exchangeStore, database, tasks);
        for (String problem : exchange.problems()) {
            getLogger().warning("exchanges.yml: " + problem);
        }
        RewardService rewards = new RewardService(getConfig().getConfigurationSection("rewards.daily"), messages,
                new RewardStore(database, economyStore, mailStore, System::currentTimeMillis), mail, database, tasks);
        for (String problem : rewards.problems()) {
            getLogger().warning("rewards.daily: " + problem);
        }

        ClaimAdapter claims = detectClaims(settings);
        getLogger().info("ระบบที่ดิน: " + claims.name());

        WorldService worlds = new WorldService(settings, getLogger());
        worlds.loadConfiguredWorlds();

        CombatTracker combat = new CombatTracker(settings.combatLockSeconds());
        LandingValidator landing = new LandingValidator(settings.safeMinY(), settings.safeMaxY());
        teleports = new TeleportService(this, messages, combat, tasks);
        HomeService homes = new HomeService(settings, messages, new HomeStore(database), database, claims, landing, teleports, tasks);
        RtpService rtp = new RtpService(this, settings, messages, landing, claims, teleports, new TravelStore(database), database, tasks);
        ItemTemplateService items = new ItemTemplateService(this);
        items.problems().forEach(p -> getLogger().warning("items.yml: " + p));
        StationService stations = new StationService(this, database, new StationStore(database), settings.stationRadius());
        ExchangeService craft = new ExchangeService(this, messages, craftStore, database, tasks, items, economy, stations, settings.maxTransaction());
        craft.problems().forEach(p -> getLogger().warning("crafting.yml: " + p));
        ItemAdapter itemAdapter = new ItemAdapter(items);
        RepairService repair = new RepairService(this, messages, database, tasks, itemAdapter, repairStore, economy,
                stations, settings.maxTransaction());
        repair.problems().forEach(p -> getLogger().warning("repair.yml: " + p));
        DepthMonsterService monsters = new DepthMonsterService(this);
        DungeonService dungeon = new DungeonService(this,messages,settings,database,dungeonGroups,tasks,teleports,landing);
        dungeon.problems().forEach(getLogger()::warning);
        monsters.problems().forEach(getLogger()::warning);
        ActionRegistry actions = new ActionRegistry(() -> services);
        Optional<CitizensBridge> citizens = CitizensBridge.detect(getLogger());
        citizens.ifPresent(c -> getLogger().info("พบ Citizens — ผูก NPC กับบริการได้ด้วย /fa npc bind <action>"));

        services = new Services(this, settings, messages, tasks, database, players, audit, economy, claims, worlds, landing,
                teleports, homes, rtp, items, new ItemInstanceStore(database), stations, actions, mail, rewards, exchange, craft, itemAdapter, repair, monsters, dungeon, citizens);
        register(monsters);
        monsters.start();

        tasks.then(stations.load(), (count, error) -> {
            if (count != null) {
                getLogger().info("โหลดจุดบริการ " + count + " จุด");
            }
        });

        register(combat, teleports, new MenuListener(this), new StationListener(stations, actions, citizens.orElse(null)),
                new NativeRepairListener(items, repair, messages),
                new DeathListener(settings, messages, economy, tasks, dungeon::owns), dungeon, new DungeonProtection(dungeon), new SessionListener(services));
        dungeon.start();

        PlayerCommands playerCommands = new PlayerCommands(services);
        for (String name : new String[]{"menu", "bank", "balance", "sethome", "home", "delhome", "homes", "rtp", "spawn", "land",
                "rewards", "mail", "exchange", "repair", "craft", "dungeon", "party"}) {
            bind(name, playerCommands);
        }
        bind("fantasycore", new CoreCommand(services));
        bind("fa", new AdminCommand(services));

        hookVault(economy);
        hookPlaceholders();

        // รองรับการเปิดปลั๊กอินระหว่างมีผู้เล่นออนไลน์ (เช่น staging)
        SessionListener session = new SessionListener(services);
        for (Player player : Bukkit.getOnlinePlayers()) {
            session.touch(player);
        }
        if (!messages.missingKeys().isEmpty()) {
            getLogger().warning("messages_th.yml ขาด key: " + messages.missingKeys());
        }
    }

    @Override
    public void onDisable() {
        if (services != null) { services.dungeon().close(); services.monsters().close(); }
        if (teleports != null) {
            teleports.cancelAll();
        }
        MenuListener.closeAll();
        if (vaultHooked) {
            VaultHook.unregister();
        }
        if (database != null) {
            database.close();
        }
    }

    public Services services() {
        return services;
    }

    private ClaimAdapter detectClaims(Settings settings) {
        String provider = settings.claimProvider();
        if (provider.equals("none")) {
            return new NoClaimAdapter("ปิดใน config");
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return new NoClaimAdapter("ไม่พบ WorldGuard");
        }
        try {
            return new WorldGuardClaimAdapter(settings.playerRegionPrefixes());
        } catch (LinkageError e) {
            getLogger().log(Level.WARNING, "WorldGuard API ไม่ตรงรุ่นที่รองรับ", e);
            return new NoClaimAdapter("WorldGuard API ไม่ตรงรุ่น");
        }
    }

    private void hookVault(EconomyService economy) {
        boolean vaultPlugin = Bukkit.getPluginManager().isPluginEnabled("Vault")
                || Bukkit.getPluginManager().isPluginEnabled("VaultUnlocked");
        // ห้ามแตะคลาส VaultHook ก่อนรู้ว่ามี Vault: JVM จะโหลด Economy ตอน verify แล้วล้ม
        if (!vaultPlugin || !classPresent("net.milkbowl.vault.economy.Economy")) {
            getLogger().warning("ไม่พบ Vault/VaultUnlocked — ปลั๊กอินอื่นจะจ่าย/รับทองผ่าน Vault ไม่ได้");
            return;
        }
        VaultHook.register(this, economy, getConfig().getString("economy.currency.gold", "ทอง"));
        vaultHooked = true;
        getLogger().info("ลงทะเบียน Vault economy provider (gold.wallet) แล้ว");
    }

    private void hookPlaceholders() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            if (PlaceholderHook.register(services)) {
                getLogger().info("ลงทะเบียน PlaceholderAPI expansion %fantasycore_*% แล้ว");
            }
        } catch (LinkageError e) {
            getLogger().log(Level.WARNING, "PlaceholderAPI ไม่ตรงรุ่นที่รองรับ", e);
        }
    }

    public boolean isVaultHooked() {
        return vaultHooked;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, FantasyCorePlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private void register(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void bind(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("คำสั่ง /" + name + " ไม่มีใน plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void saveIfMissing(String resource) {
        if (!new File(getDataFolder(), resource).exists()) {
            saveResource(resource, false);
        }
    }
}
