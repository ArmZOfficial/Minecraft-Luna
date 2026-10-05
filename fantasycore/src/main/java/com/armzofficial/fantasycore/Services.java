package com.armzofficial.fantasycore;

import com.armzofficial.fantasycore.audit.AuditStore;
import com.armzofficial.fantasycore.claim.ClaimAdapter;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.config.Settings;
import com.armzofficial.fantasycore.economy.EconomyService;
import com.armzofficial.fantasycore.exchange.ExchangeService;
import com.armzofficial.fantasycore.home.HomeService;
import com.armzofficial.fantasycore.hook.CitizensBridge;
import com.armzofficial.fantasycore.item.ItemInstanceStore;
import com.armzofficial.fantasycore.item.ItemTemplateService;
import com.armzofficial.fantasycore.mail.MailService;
import com.armzofficial.fantasycore.reward.RewardService;
import com.armzofficial.fantasycore.station.ActionRegistry;
import com.armzofficial.fantasycore.station.StationService;
import com.armzofficial.fantasycore.storage.Database;
import com.armzofficial.fantasycore.storage.PlayerStore;
import com.armzofficial.fantasycore.travel.LandingValidator;
import com.armzofficial.fantasycore.travel.RtpService;
import com.armzofficial.fantasycore.travel.TeleportService;
import com.armzofficial.fantasycore.util.Tasks;
import com.armzofficial.fantasycore.world.WorldService;

import java.util.Optional;

/** บริการทั้งหมดที่สร้างตอน onEnable — ส่งต่อให้คำสั่ง/เมนูแทนการใช้ static */
public record Services(
        FantasyCorePlugin plugin,
        Settings settings,
        Messages messages,
        Tasks tasks,
        Database database,
        PlayerStore players,
        AuditStore audit,
        EconomyService economy,
        ClaimAdapter claims,
        WorldService worlds,
        LandingValidator landing,
        TeleportService teleports,
        HomeService homes,
        RtpService rtp,
        ItemTemplateService items,
        ItemInstanceStore itemInstances,
        StationService stations,
        ActionRegistry actions,
        MailService mail,
        RewardService rewards,
        ExchangeService exchange,
        Optional<CitizensBridge> citizens) {
}
