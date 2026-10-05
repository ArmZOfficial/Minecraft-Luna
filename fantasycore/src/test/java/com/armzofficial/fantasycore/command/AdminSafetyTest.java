package com.armzofficial.fantasycore.command;

import com.armzofficial.fantasycore.TestDatabases;
import com.armzofficial.fantasycore.config.Messages;
import com.armzofficial.fantasycore.storage.PlayerStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AdminSafetyTest {
    private CommandSender sender(Set<String> nodes) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> method.getName().equals("hasPermission") && nodes.contains(args[0]));
    }

    @Test void nonceBelongsToActorAndCannotReplay() {
        var confirmations = new PendingConfirmations(); var writes = new AtomicInteger(); UUID actor = UUID.randomUUID();
        String token = confirmations.create(actor, writes::incrementAndGet);
        assertEquals(PendingConfirmations.Outcome.WRONG_ACTOR, confirmations.confirm(token, UUID.randomUUID()));
        assertEquals(PendingConfirmations.Outcome.APPLIED, confirmations.confirm(token.toLowerCase(), actor));
        assertEquals(PendingConfirmations.Outcome.UNKNOWN, confirmations.confirm(token, actor));
        assertEquals(1, writes.get());
    }

    @Test void revokedPermissionAtConfirmationNeverRunsWrite() {
        var confirmations = new PendingConfirmations(); var allowed = new AtomicBoolean(true); var writes = new AtomicInteger();
        UUID actor = UUID.randomUUID(); String token = confirmations.create(actor, allowed::get, writes::incrementAndGet);
        allowed.set(false);
        assertEquals(PendingConfirmations.Outcome.DENIED, confirmations.confirm(token, actor));
        allowed.set(true);
        assertEquals(PendingConfirmations.Outcome.UNKNOWN, confirmations.confirm(token, actor));
        assertEquals(0, writes.get());
        String outstanding = confirmations.create(actor, writes::incrementAndGet);
        confirmations.clear();
        assertEquals(PendingConfirmations.Outcome.UNKNOWN, confirmations.confirm(outstanding, actor));
        assertEquals(0, writes.get());
    }

    @Test void closingPreviewInvalidatesOnlyItsOwnersNonce() {
        var confirmations = new PendingConfirmations(); UUID actor = UUID.randomUUID(); var writes = new AtomicInteger();
        String token = confirmations.create(actor, writes::incrementAndGet);
        assertFalse(confirmations.cancel(token, UUID.randomUUID()));
        assertTrue(confirmations.cancel(token, actor));
        assertFalse(confirmations.cancel(token, actor));
        assertEquals(PendingConfirmations.Outcome.UNKNOWN, confirmations.confirm(token, actor));
        assertEquals(0, writes.get());
    }

    @Test void exactExpiryBoundaryRejectsConfirmationWithoutSleeping() {
        var time = new AtomicLong(1000); var confirmations = new PendingConfirmations(time::get);
        var writes = new AtomicInteger(); UUID actor = UUID.randomUUID();
        String token = confirmations.create(actor, writes::incrementAndGet);
        time.set(61_000);
        assertEquals(PendingConfirmations.Outcome.EXPIRED, confirmations.confirm(token, actor));
        assertEquals(0, writes.get());
    }

    @Test void concurrentConfirmationsRunOneApply() throws Exception {
        var confirmations = new PendingConfirmations(); UUID actor = UUID.randomUUID(); var writes = new AtomicInteger();
        String token = confirmations.create(actor, writes::incrementAndGet);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<java.util.concurrent.Future<PendingConfirmations.Outcome>>();
            for (int i=0; i<32; i++) { jobs.add(pool.submit(() -> confirmations.confirm(token, actor))); }
            int applied = 0;
            for (var job : jobs) { if (job.get() == PendingConfirmations.Outcome.APPLIED) { applied++; } }
            assertEquals(1, applied); assertEquals(1, writes.get());
        }
    }

    @Test void promptClaimsOnceAcrossAsyncMessagesAndRejectsExpiredResponse() throws Exception {
        var session = new AdminChatInput.Pending(UUID.randomUUID(), 60_000, "fantasyadmin.view", text -> {});
        try (var pool = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i=0; i<32; i++) { jobs.add(pool.submit(() -> session.claim(10))); }
            int accepted = 0;
            for (var job : jobs) { if (job.get()) { accepted++; } }
            assertEquals(1, accepted);
        }
        var expired = new AdminChatInput.Pending(UUID.randomUUID(), 60_000, "fantasyadmin.view", text -> {});
        assertFalse(expired.claim(60_000));
    }

    @Test void inputIsBoundedPlainTextAndCannotAcceptControlCharacters() {
        assertEquals("คืนของจากรายการค้าง", AdminChatInput.text("  คืนของจากรายการค้าง  "));
        assertEquals("<red>เหตุผล /fa eco", AdminChatInput.text("<red>เหตุผล /fa eco"));
        assertEquals(200, AdminChatInput.text("ก".repeat(200)).length());
        for (String input : new String[]{"", "  ", "ก".repeat(201), "เหตุผล\nอีกคำสั่ง", "เหตุผล\tใหม่", "a\u0000b"}) {
            assertThrows(IllegalArgumentException.class, () -> AdminChatInput.text(input));
        }
    }

    @Test void viewAndAdjustAreBothRequiredAndSuggestionsHideWrites() {
        var observer = sender(Set.of("fantasyadmin.view"));
        assertFalse(AdminCommand.canAdjust(observer));
        assertFalse(AdminCommand.canAdjust(sender(Set.of("fantasyadmin.economy.adjust"))));
        assertTrue(AdminCommand.canAdjust(sender(Set.of("fantasyadmin.view", "fantasyadmin.economy.adjust"))));
        assertFalse(AdminCommand.suggestible(observer, "eco", "give"));
        assertTrue(AdminCommand.suggestible(observer, "craft", "review"));
        assertFalse(AdminCommand.suggestible(observer, "craft", "complete"));
        assertFalse(AdminCommand.suggestible(observer, "mail", "release"));
        assertFalse(AdminCommand.suggestible(observer, "item", "give"));
        assertFalse(AdminCommand.suggestible(observer, "dungeon", "buildparty"));
        assertTrue(AdminCommand.suggestible(observer, "dungeon", "status"));
    }

    @Test void offlineUuidLookupNeverRetargetsWhenANameChangesOrIsReused() throws Exception {
        try (var db = TestDatabases.fresh()) {
            var players = new PlayerStore(db); UUID original = UUID.randomUUID(), another = UUID.randomUUID();
            players.touch(original, "Moon", 100); players.touch(original, "MoonRenamed", 200);
            players.touch(another, "Moon", 300);
            assertEquals(original, players.findByUuid(original).orElseThrow().uuid());
            assertEquals("MoonRenamed", players.findByUuid(original).orElseThrow().name());
            assertEquals(another, players.findByName("moon").orElseThrow().uuid());
            assertTrue(players.findByUuid(UUID.randomUUID()).isEmpty());
        }
    }

    @Test void previewRendersExactRecipientAndReasonAsUnparsedText() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString(Files.readString(Path.of("src/main/resources/messages_th.yml")));
        var templates = new HashMap<String,String>();
        for (String key : yaml.getKeys(true)) { if (yaml.isString(key)) { templates.put(key, yaml.getString(key)); } }
        var messages = Messages.ofTemplates(templates);
        UUID id = UUID.randomUUID(); String reason = "<red>คืนยอด /fa eco";
        var lore = messages.lines("admin.panel.confirm-lore", Messages.p("player", "Moon"), Messages.p("uuid", id),
                Messages.p("bucket", "gold.wallet"), Messages.p("delta", "+100"), Messages.p("before", "1,000"),
                Messages.p("after", "1,100"), Messages.p("reason", reason));
        String text = String.join("\n", lore.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList());
        assertTrue(text.contains(id.toString())); assertTrue(text.contains(reason));
        assertTrue(text.contains("1,000") && text.contains("1,100") && text.contains("+100"));
        var balance = messages.lines("admin.panel.balance-lore", Messages.p("uuid", id), Messages.p("gold", 1), Messages.p("bank", 2), Messages.p("redcoin", 3));
        assertEquals("เงินแดง 3", PlainTextComponentSerializer.plainText().serialize(balance.get(3)));
    }
}
